package io.github.pawelusze.hexagon.region;

import io.github.pawelusze.hexagon.api.region.Region;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Open-addressing hash table from a chunk key to the regions touching that chunk, with primitive
 * {@code long} keys. A boxed key costs a hundred nanoseconds per lookup once the table holds a
 * few hundred thousand chunks; this costs three.
 *
 * <p>Writes happen in place and are the owner's job to serialise. A lookup that races a write may
 * read a half-moved entry, but never crashes: the arrays it probes always belong together, because
 * they live in one {@link Slots} object that is swapped whole when the table grows. The owner
 * validates a racing lookup and repeats it.
 */
final class ChunkTable {

    /** A key no chunk can have: chunk x would have to be {@code Integer.MIN_VALUE}. */
    private static final long EMPTY = Long.MIN_VALUE;

    private static final int INITIAL_CAPACITY = 1024;

    private Slots slots = new Slots(INITIAL_CAPACITY);
    private int size;

    static long keyOf(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    @Nullable
    List<Region> get(long key) {
        Slots current = this.slots;
        int slot = current.slotOf(key);
        while (true) {
            long found = current.keys[slot];
            if (found == key) {
                return current.bucketAt(slot);
            }
            if (found == EMPTY) {
                return null;
            }
            slot = current.next(slot);
        }
    }

    void put(long key, @NotNull List<Region> bucket) {
        if (this.size * 2 >= this.slots.keys.length) {
            this.grow();
        }

        Slots current = this.slots;
        int slot = current.slotOf(key);
        while (current.keys[slot] != EMPTY && current.keys[slot] != key) {
            slot = current.next(slot);
        }
        if (current.keys[slot] == EMPTY) {
            this.size++;
        }
        current.keys[slot] = key;
        current.buckets[slot] = bucket;
    }

    /** Removes a key, closing the gap it leaves so later probes still find their entries. */
    void remove(long key) {
        Slots current = this.slots;
        int slot = current.slotOf(key);
        while (current.keys[slot] != key) {
            if (current.keys[slot] == EMPTY) {
                return;
            }
            slot = current.next(slot);
        }

        int gap = slot;
        int next = current.next(gap);
        while (current.keys[next] != EMPTY) {
            int home = current.slotOf(current.keys[next]);
            boolean belongsBeforeGap = gap <= next ? (home <= gap || home > next) : (home <= gap && home > next);
            if (belongsBeforeGap) {
                current.keys[gap] = current.keys[next];
                current.buckets[gap] = current.buckets[next];
                gap = next;
            }
            next = current.next(next);
        }
        current.keys[gap] = EMPTY;
        current.buckets[gap] = null;
        this.size--;
    }

    private void grow() {
        Slots old = this.slots;
        this.slots = new Slots(old.keys.length * 2);
        this.size = 0;
        for (int index = 0; index < old.keys.length; index++) {
            if (old.keys[index] != EMPTY) {
                this.put(old.keys[index], old.bucketAt(index));
            }
        }
    }

    /** The two arrays of one capacity, kept together so a reader never mixes two generations. */
    private static final class Slots {

        private final long[] keys;
        private final Object[] buckets;
        private final int mask;

        private Slots(int capacity) {
            this.keys = new long[capacity];
            this.buckets = new Object[capacity];
            this.mask = capacity - 1;
            Arrays.fill(this.keys, EMPTY);
        }

        private int slotOf(long key) {
            long hashed = key * 0x9E3779B97F4A7C15L;
            return (int) (hashed ^ (hashed >>> 32)) & this.mask;
        }

        private int next(int slot) {
            return (slot + 1) & this.mask;
        }

        @SuppressWarnings("unchecked")
        private List<Region> bucketAt(int slot) {
            return (List<Region>) this.buckets[slot];
        }
    }
}
