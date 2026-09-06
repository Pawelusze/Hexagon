package io.github.pawelusze.hexagon.region;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pawelusze.hexagon.api.region.BlockPoint;
import io.github.pawelusze.hexagon.api.region.Bounds;
import io.github.pawelusze.hexagon.api.region.Region;
import io.github.pawelusze.hexagon.api.region.RegionId;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class RegionIndexTest {

    private static final Key WORLD = Key.key("minecraft", "overworld");
    private static final Key NETHER = Key.key("minecraft", "the_nether");

    private final RegionIndex index = new RegionIndex();

    @Test
    void findsRegionsCoveringAPointOrderedByPriority() {
        Region town = region("town", 0, 0, 0, 200, 100, 200).withPriority(1);
        Region shop = region("shop", 10, 60, 10, 20, 70, 20).withPriority(5);
        index.add(town);
        index.add(shop);

        assertThat(index.at(WORLD, 15, 65, 15)).containsExactly(shop, town);
        assertThat(index.at(WORLD, 150, 65, 150)).containsExactly(town);
        assertThat(index.at(WORLD, -1, 65, 15)).isEmpty();
        assertThat(index.at(NETHER, 15, 65, 15)).isEmpty();
    }

    @Test
    void largeRegionsAreFoundWithoutChunkBuckets() {
        Region world = region("world", -100_000, -64, -100_000, 100_000, 320, 100_000);
        index.add(world);

        assertThat(index.at(WORLD, 54_321, 0, -12_345)).containsExactly(world);

        index.remove(world);

        assertThat(index.at(WORLD, 54_321, 0, -12_345)).isEmpty();
    }

    @Test
    void removalDropsRegionFromEveryChunk() {
        Region region = region("plot", 0, 0, 0, 40, 10, 40);
        index.add(region);
        index.remove(region);

        assertThat(index.at(WORLD, 0, 0, 0)).isEmpty();
        assertThat(index.at(WORLD, 39, 5, 39)).isEmpty();
    }

    @Test
    void replacingARegionKeepsOnlyTheNewVersion() {
        Region original = region("plot", 0, 0, 0, 10, 10, 10);
        Region moved = original.withBounds(Bounds.between(new BlockPoint(50, 0, 50), new BlockPoint(60, 10, 60)));
        index.add(original);
        index.remove(original);
        index.add(moved);

        assertThat(index.at(WORLD, 5, 5, 5)).isEmpty();
        assertThat(index.at(WORLD, 55, 5, 55)).containsExactly(moved);
    }

    private static Region region(String id, int x1, int y1, int z1, int x2, int y2, int z2) {
        return Region.of(
                RegionId.of(id), WORLD, Bounds.between(new BlockPoint(x1, y1, z1), new BlockPoint(x2, y2, z2)));
    }
}
