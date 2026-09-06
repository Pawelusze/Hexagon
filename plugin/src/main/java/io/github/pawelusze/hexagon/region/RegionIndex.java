package io.github.pawelusze.hexagon.region;

import io.github.pawelusze.hexagon.api.region.Bounds;
import io.github.pawelusze.hexagon.api.region.Region;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Spatial index of regions, bucketed per world and chunk. Regions spanning more than {@value
 * #LARGE_REGION_CHUNKS} chunks are kept in a separate per-world list instead of being registered
 * in every chunk they cover. Reads are lock-free; writes replace immutable bucket lists.
 *
 * <p>Every bucket stays sorted by priority, because regions change rarely and are looked up on
 * every block a player walks over. A lookup then merges two sorted lists instead of sorting the
 * matches it found.
 */
final class RegionIndex {

    private static final int LARGE_REGION_CHUNKS = 4096;

    /** How many regions cover one block on a normal server, so the match list rarely grows. */
    private static final int TYPICAL_MATCHES = 4;

    private static final Comparator<Region> HIGHEST_PRIORITY_FIRST = Comparator.comparingInt(Region::priority)
            .reversed()
            .thenComparing(region -> region.id().value());

    private final Map<Key, WorldIndex> worlds = new ConcurrentHashMap<>();

    void add(@NotNull Region region) {
        this.worlds.computeIfAbsent(region.world(), _ -> new WorldIndex()).add(region);
    }

    void remove(@NotNull Region region) {
        WorldIndex world = this.worlds.get(region.world());
        if (world != null) {
            world.remove(region);
        }
    }

    void clear() {
        this.worlds.clear();
    }

    @NotNull
    List<Region> at(@NotNull Key world, int x, int y, int z) {
        WorldIndex index = this.worlds.get(world);
        if (index == null) {
            return List.of();
        }
        return index.at(x, y, z);
    }

    /**
     * Returns the highest-priority region covering the block that the predicate accepts, or null.
     * Resolving a flag stops at the first region that sets it, so this builds no list at all.
     */
    @Nullable
    Region firstCovering(@NotNull Key world, int x, int y, int z, @NotNull Predicate<Region> accepts) {
        WorldIndex index = this.worlds.get(world);
        if (index == null) {
            return null;
        }
        return index.walkCovering(x, y, z, accepts);
    }

    private static final class WorldIndex {

        private final Map<Long, List<Region>> chunks = new ConcurrentHashMap<>();
        private final AtomicReference<List<Region>> large = new AtomicReference<>(List.of());

        void add(@NotNull Region region) {
            if (isLarge(region.bounds())) {
                large.updateAndGet(current -> appended(current, region));
                return;
            }
            forEachChunk(region.bounds(), key -> this.chunks.merge(key, List.of(region), RegionIndex::concat));
        }

        void remove(@NotNull Region region) {
            if (isLarge(region.bounds())) {
                large.updateAndGet(current -> without(current, region));
                return;
            }
            forEachChunk(
                    region.bounds(),
                    key -> chunks.computeIfPresent(key, (_, bucket) -> {
                        List<Region> remaining = without(bucket, region);
                        return remaining.isEmpty() ? null : remaining;
                    }));
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
            List<Region> bucket = this.chunks.getOrDefault(chunkKey(x >> 4, z >> 4), List.of());
            List<Region> huge = this.large.get();

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

        private static boolean isLarge(Bounds bounds) {
            long chunksX = (bounds.max().x() >> 4) - (bounds.min().x() >> 4) + 1L;
            long chunksZ = (bounds.max().z() >> 4) - (bounds.min().z() >> 4) + 1L;
            return chunksX * chunksZ > LARGE_REGION_CHUNKS;
        }

        private static void forEachChunk(Bounds bounds, LongConsumer action) {
            for (int chunkX = bounds.min().x() >> 4; chunkX <= bounds.max().x() >> 4; chunkX++) {
                for (int chunkZ = bounds.min().z() >> 4; chunkZ <= bounds.max().z() >> 4; chunkZ++) {
                    action.accept(chunkKey(chunkX, chunkZ));
                }
            }
        }

        private static long chunkKey(int chunkX, int chunkZ) {
            return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
        }
    }

    /** Joins two buckets and keeps the result in priority order, which is what lookups rely on. */
    private static List<Region> concat(List<Region> first, List<Region> second) {
        List<Region> merged = new ArrayList<>(first.size() + second.size());
        merged.addAll(first);
        merged.addAll(second);
        merged.sort(HIGHEST_PRIORITY_FIRST);
        return List.copyOf(merged);
    }

    private static List<Region> appended(List<Region> list, Region region) {
        return concat(list, List.of(region));
    }

    private static List<Region> without(List<Region> list, Region region) {
        return list.stream()
                .filter(candidate -> !candidate.id().equals(region.id()))
                .toList();
    }
}
