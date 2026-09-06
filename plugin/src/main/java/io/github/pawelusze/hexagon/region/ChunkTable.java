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
 * <p>The table is mutated only while it is private to one thread: while a snapshot is being built,
 * or on a {@link #copy()} that the owner publishes afterwards. Lookups never lock.
 */
final class ChunkTable {

    /** A key no chunk can have: chunk x would have to be {@code Integer.MIN_VALUE}. */
    private static final long EMPTY = Long.MIN_VALUE;

    private static final int INITIAL_CAPACITY = 1024;

    private long[] keys;
    private Object[] buckets;
    private int mask;
    private int size;

    ChunkTable() {
        this(INITIAL_CAPACITY);
    }

    private ChunkTable(int capacity) {
        this.keys = new long[capacity];
        this.buckets = new Object[capacity];
        this.mask = capacity - 1;
        Arrays.fill(this.keys, EMPTY);
    }

    static long keyOf(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    List<Region> get(long key) {
        int slot = slotOf(key);
        while (true) {
            long found = this.keys[slot];
            if (found == key) {
                return (List<Region>) this.buckets[slot];
            }
            if (found == EMPTY) {
                return null;
            }
            slot = (slot + 1) & this.mask;
        }
    }

    void put(long key, @NotNull List<Region> bucket) {
        if (this.size * 2 >= this.keys.length) {
            this.grow();
        }

        int slot = slotOf(key);
        while (this.keys[slot] != EMPTY && this.keys[slot] != key) {
            slot = (slot + 1) & this.mask;
        }
        if (this.keys[slot] == EMPTY) {
            this.size++;
        }
        this.keys[slot] = key;
        this.buckets[slot] = bucket;
    }

    /** Removes a key, closing the gap it leaves so later probes still find their entries. */
    void remove(long key) {
        int slot = slotOf(key);
        while (this.keys[slot] != key) {
            if (this.keys[slot] == EMPTY) {
                return;
            }
            slot = (slot + 1) & this.mask;
        }

        int gap = slot;
        int next = (gap + 1) & this.mask;
        while (this.keys[next] != EMPTY) {
            int home = slotOf(this.keys[next]);
            boolean belongsBeforeGap = gap <= next ? (home <= gap || home > next) : (home <= gap && home > next);
            if (belongsBeforeGap) {
                this.keys[gap] = this.keys[next];
                this.buckets[gap] = this.buckets[next];
                gap = next;
            }
            next = (next + 1) & this.mask;
        }
        this.keys[gap] = EMPTY;
        this.buckets[gap] = null;
        this.size--;
    }

    @NotNull
    ChunkTable copy() {
        ChunkTable copy = new ChunkTable(this.keys.length);
        System.arraycopy(this.keys, 0, copy.keys, 0, this.keys.length);
        System.arraycopy(this.buckets, 0, copy.buckets, 0, this.buckets.length);
        copy.size = this.size;
        return copy;
    }

    private void grow() {
        long[] oldKeys = this.keys;
        Object[] oldBuckets = this.buckets;

        int capacity = oldKeys.length * 2;
        this.keys = new long[capacity];
        this.buckets = new Object[capacity];
        this.mask = capacity - 1;
        this.size = 0;
        Arrays.fill(this.keys, EMPTY);

        for (int index = 0; index < oldKeys.length; index++) {
            if (oldKeys[index] != EMPTY) {
                this.put(oldKeys[index], castBucket(oldBuckets[index]));
            }
        }
    }

    private int slotOf(long key) {
        long hashed = key * 0x9E3779B97F4A7C15L;
        return (int) (hashed ^ (hashed >>> 32)) & this.mask;
    }

    @SuppressWarnings("unchecked")
    private static List<Region> castBucket(Object bucket) {
        return (List<Region>) bucket;
    }
}
