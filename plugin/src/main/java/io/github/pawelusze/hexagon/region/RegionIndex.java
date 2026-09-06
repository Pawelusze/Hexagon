package io.github.pawelusze.hexagon.region;

import io.github.pawelusze.hexagon.api.region.Bounds;
import io.github.pawelusze.hexagon.api.region.Region;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Predicate;
import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Spatial index of regions, bucketed per world and chunk. Regions spanning more than {@value
 * #LARGE_REGION_CHUNKS} chunks are kept in a separate per-world list instead of being registered
 * in every chunk they cover.
 *
 * <p>Lookups take no lock: they read optimistically and repeat the read only when a write overlapped
 * it, which on a server means never, because writes come from commands on the same thread.
 * {@link #addAll} takes the write lock once for a whole load.
 *
 * <p>Every bucket stays sorted by priority, because regions change rarely and are looked up on
 * every block a player walks over. A lookup then merges two sorted lists instead of sorting the
 * matches it found.
 */
final class RegionIndex {

    private static final int LARGE_REGION_CHUNKS = 4096;

    /** How many regions cover one block on a normal server, so the match list rarely grows. */
    private static final int TYPICAL_MATCHES = 4;

    /** What a world without regions answers with, so its lookups are as cheap as any other. */
    private static final WorldIndex EMPTY = new WorldIndex();

    private static final Comparator<Region> HIGHEST_PRIORITY_FIRST = Comparator.comparingInt(Region::priority)
            .reversed()
            .thenComparing(region -> region.id().value());

    private final Map<Key, WorldIndex> worlds = new ConcurrentHashMap<>();

    /**
     * Bukkit worlds looked up by id: asking a world for its key builds and validates a new key on
     * every call, and every block event would pay for that. Cleared whenever a world gains its
     * first region, so a cached "nothing here" never outlives that.
     */
    private final Map<UUID, WorldIndex> byWorldId = new ConcurrentHashMap<>();

    void add(@NotNull Region region) {
        this.worldOf(region).add(region);
    }

    void addAll(@NotNull Collection<Region> regions) {
        Map<Key, List<Region>> byWorld = new HashMap<>();
        for (Region region : regions) {
            byWorld.computeIfAbsent(region.world(), _ -> new ArrayList<>()).add(region);
        }
        byWorld.forEach((world, inWorld) -> this.worldOf(world).addAll(inWorld));
    }

    void remove(@NotNull Region region) {
        WorldIndex world = this.worlds.get(region.world());
        if (world != null) {
            world.remove(region);
        }
    }

    @NotNull
    List<Region> at(@NotNull Key world, int x, int y, int z) {
        WorldIndex index = this.worlds.get(world);
        return index == null ? List.of() : index.at(x, y, z);
    }

    @NotNull
    List<Region> at(@NotNull World world, int x, int y, int z) {
        return this.indexOf(world).at(x, y, z);
    }

    @Nullable
    Region firstCovering(@NotNull World world, int x, int y, int z, @NotNull Predicate<Region> accepts) {
        return this.indexOf(world).walkCovering(x, y, z, accepts);
    }

    /**
     * Returns the highest-priority region covering the block that the predicate accepts, or null.
     * Resolving a flag stops at the first region that sets it, so this builds no list at all.
     */
    @Nullable
    Region firstCovering(@NotNull Key world, int x, int y, int z, @NotNull Predicate<Region> accepts) {
        WorldIndex index = this.worlds.get(world);
        return index == null ? null : index.walkCovering(x, y, z, accepts);
    }

    private WorldIndex worldOf(Region region) {
        return this.worldOf(region.world());
    }

    private WorldIndex worldOf(Key world) {
        WorldIndex existing = this.worlds.get(world);
        if (existing != null) {
            return existing;
        }

        WorldIndex created = this.worlds.computeIfAbsent(world, _ -> new WorldIndex());
        this.byWorldId.clear();
        return created;
    }

    private WorldIndex indexOf(World world) {
        WorldIndex cached = this.byWorldId.get(world.getUID());
        if (cached != null) {
            return cached;
        }

        WorldIndex resolved = this.worlds.getOrDefault(world.key(), EMPTY);
        this.byWorldId.put(world.getUID(), resolved);
        return resolved;
    }

    private static final class WorldIndex {

        private final StampedLock lock = new StampedLock();
        private final ChunkTable chunks = new ChunkTable();
        private volatile List<Region> large = List.of();

        void add(Region region) {
            if (isLarge(region.bounds())) {
                this.large = sorted(this.large, region);
                return;
            }

            long stamp = this.lock.writeLock();
            try {
                this.insert(region);
            } finally {
                this.lock.unlockWrite(stamp);
            }
        }

        void addAll(List<Region> regions) {
            List<Region> huge = new ArrayList<>(this.large);
            long stamp = this.lock.writeLock();
            try {
                for (Region region : regions) {
                    if (isLarge(region.bounds())) {
                        huge.add(region);
                    } else {
                        this.insert(region);
                    }
                }
            } finally {
                this.lock.unlockWrite(stamp);
            }
            huge.sort(HIGHEST_PRIORITY_FIRST);
            this.large = List.copyOf(huge);
        }

        void remove(Region region) {
            if (isLarge(region.bounds())) {
                this.large = without(this.large, region);
                return;
            }

            long stamp = this.lock.writeLock();
            try {
                Bounds bounds = region.bounds();
                for (int chunkX = bounds.min().x() >> 4; chunkX <= bounds.max().x() >> 4; chunkX++) {
                    for (int chunkZ = bounds.min().z() >> 4;
                            chunkZ <= bounds.max().z() >> 4;
                            chunkZ++) {
                        long key = ChunkTable.keyOf(chunkX, chunkZ);
                        List<Region> bucket = this.chunks.get(key);
                        if (bucket == null) {
                            continue;
                        }
                        List<Region> remaining = without(bucket, region);
                        if (remaining.isEmpty()) {
                            this.chunks.remove(key);
                        } else {
                            this.chunks.put(key, remaining);
                        }
                    }
                }
            } finally {
                this.lock.unlockWrite(stamp);
            }
        }

        @NotNull
        List<Region> at(int x, int y, int z) {
            List<Region> matches = new ArrayList<>(TYPICAL_MATCHES);
            this.walkCovering(x, y, z, region -> {
                matches.add(region);
                return false;
            });
            return matches.isEmpty() ? List.of() : Collections.unmodifiableList(matches);
        }

        /**
         * Walks the regions covering the block in priority order, merging the chunk bucket with the
         * large regions. Both are already sorted, so nothing is sorted here.
         *
         * @return the first region {@code stopsHere} accepts, or null when it accepted none
         */
        private @Nullable Region walkCovering(int x, int y, int z, Predicate<Region> stopsHere) {
            List<Region> bucket = this.bucketOf(ChunkTable.keyOf(x >> 4, z >> 4));
            List<Region> huge = this.large;

            int fromBucket = 0;
            int fromHuge = 0;
            while (fromBucket < bucket.size() || fromHuge < huge.size()) {
                boolean takeBucket = fromHuge == huge.size()
                        || (fromBucket < bucket.size()
                                && HIGHEST_PRIORITY_FIRST.compare(bucket.get(fromBucket), huge.get(fromHuge)) <= 0);
                Region next = takeBucket ? bucket.get(fromBucket++) : huge.get(fromHuge++);
                if (next.bounds().contains(x, y, z) && stopsHere.test(next)) {
                    return next;
                }
            }
            return null;
        }

        /**
         * Reads a bucket without taking the lock: a write that happens to overlap makes the stamp
         * stale, and only then is the read repeated under the lock.
         */
        private List<Region> bucketOf(long key) {
            long stamp = this.lock.tryOptimisticRead();
            List<Region> bucket = this.chunks.get(key);
            if (!this.lock.validate(stamp)) {
                stamp = this.lock.readLock();
                try {
                    bucket = this.chunks.get(key);
                } finally {
                    this.lock.unlockRead(stamp);
                }
            }
            return bucket == null ? List.of() : bucket;
        }

        /** Registers a region in every chunk bucket it touches; the caller holds the write lock. */
        private void insert(Region region) {
            Bounds bounds = region.bounds();
            for (int chunkX = bounds.min().x() >> 4; chunkX <= bounds.max().x() >> 4; chunkX++) {
                for (int chunkZ = bounds.min().z() >> 4; chunkZ <= bounds.max().z() >> 4; chunkZ++) {
                    long key = ChunkTable.keyOf(chunkX, chunkZ);
                    List<Region> bucket = this.chunks.get(key);
                    this.chunks.put(key, bucket == null ? List.of(region) : sorted(bucket, region));
                }
            }
        }

        private static boolean isLarge(Bounds bounds) {
            long chunksX = (bounds.max().x() >> 4) - (bounds.min().x() >> 4) + 1L;
            long chunksZ = (bounds.max().z() >> 4) - (bounds.min().z() >> 4) + 1L;
            return chunksX * chunksZ > LARGE_REGION_CHUNKS;
        }
    }

    /** A bucket with one more region, kept in the priority order lookups rely on. */
    private static List<Region> sorted(List<Region> bucket, Region region) {
        List<Region> merged = new ArrayList<>(bucket.size() + 1);
        merged.addAll(bucket);
        merged.add(region);
        merged.sort(HIGHEST_PRIORITY_FIRST);
        return List.copyOf(merged);
    }

    private static List<Region> without(List<Region> bucket, Region region) {
        return bucket.stream()
                .filter(candidate -> !candidate.id().equals(region.id()))
                .toList();
    }
}
