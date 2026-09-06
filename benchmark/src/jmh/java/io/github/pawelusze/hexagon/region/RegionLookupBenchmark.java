package io.github.pawelusze.hexagon.region;

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.index.ChunkHashTable;
import com.sk89q.worldguard.protection.managers.index.PriorityRTreeIndex;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.github.pawelusze.hexagon.api.flag.FlagMap;
import io.github.pawelusze.hexagon.api.region.BlockPoint;
import io.github.pawelusze.hexagon.api.region.Bounds;
import io.github.pawelusze.hexagon.api.region.Region;
import io.github.pawelusze.hexagon.api.region.RegionId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.key.Key;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Answers one question for both plugins: given a world full of regions, how long does it take to
 * find the regions covering a block? That lookup runs for every block a player breaks, places,
 * walks over or hits, so it is the number that decides whether protection costs a server anything.
 *
 * <p>Both indexes are handed the same regions and asked about the same blocks. WorldGuard is
 * measured through its own two indexes: the R-tree it builds by default, and the chunk table it
 * layers on top once a chunk has been asked about, which is measured warm.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class RegionLookupBenchmark {

    private static final Key WORLD = Key.key("minecraft", "overworld");
    private static final int WORLD_RADIUS = 5000;
    private static final int REGION_SIZE = 48;
    private static final int QUERY_POINTS = 1024;
    private static final long SEED = 20260906L;

    /** Small, busy and unreasonable, so the shape of the curve is visible rather than one point. */
    @Param({"100", "10000", "50000"})
    private int regionCount;

    private RegionIndex hexagon;
    private PriorityRTreeIndex worldGuardRTree;
    private ChunkHashTable worldGuardChunkTable;

    private int[] queryX;
    private int[] queryY;
    private int[] queryZ;
    private BlockVector3[] queryVectors;
    private int cursor;

    @Setup
    public void setUp() throws InterruptedException {
        Random random = new Random(SEED);
        this.hexagon = new RegionIndex();
        this.worldGuardRTree = new PriorityRTreeIndex();
        this.worldGuardChunkTable = new ChunkHashTable(new PriorityRTreeIndex(), "benchmark");

        List<ProtectedRegion> worldGuardRegions = new ArrayList<>(this.regionCount);
        for (int index = 0; index < this.regionCount; index++) {
            int x = random.nextInt(-WORLD_RADIUS, WORLD_RADIUS);
            int z = random.nextInt(-WORLD_RADIUS, WORLD_RADIUS);
            int priority = random.nextInt(0, 10);

            this.hexagon.add(new Region(
                    RegionId.of("region-" + index),
                    WORLD,
                    new Bounds(new BlockPoint(x, 0, z), new BlockPoint(x + REGION_SIZE, 255, z + REGION_SIZE)),
                    priority,
                    Map.of(),
                    FlagMap.empty()));

            ProtectedCuboidRegion cuboid = new ProtectedCuboidRegion(
                    "region-" + index,
                    BlockVector3.at(x, 0, z),
                    BlockVector3.at(x + REGION_SIZE, 255, z + REGION_SIZE));
            cuboid.setPriority(priority);
            worldGuardRegions.add(cuboid);
        }

        this.worldGuardRTree.addAll(worldGuardRegions);
        this.worldGuardChunkTable.addAll(worldGuardRegions);

        this.queryX = new int[QUERY_POINTS];
        this.queryY = new int[QUERY_POINTS];
        this.queryZ = new int[QUERY_POINTS];
        this.queryVectors = new BlockVector3[QUERY_POINTS];
        List<BlockVector2> queryChunks = new ArrayList<>(QUERY_POINTS);
        for (int index = 0; index < QUERY_POINTS; index++) {
            this.queryX[index] = random.nextInt(-WORLD_RADIUS, WORLD_RADIUS);
            this.queryY[index] = random.nextInt(0, 256);
            this.queryZ[index] = random.nextInt(-WORLD_RADIUS, WORLD_RADIUS);
            this.queryVectors[index] = BlockVector3.at(this.queryX[index], this.queryY[index], this.queryZ[index]);
            queryChunks.add(BlockVector2.at(this.queryX[index] >> 4, this.queryZ[index] >> 4));
        }

        // WorldGuard only caches a chunk that was biased, which a server does when the chunk loads.
        this.worldGuardChunkTable.biasAll(queryChunks);
        this.worldGuardChunkTable.awaitCompletion(60, TimeUnit.SECONDS);
    }

    @Benchmark
    public void hexagonRegionsAt(Blackhole blackhole) {
        int index = this.nextPoint();
        blackhole.consume(this.hexagon.at(WORLD, this.queryX[index], this.queryY[index], this.queryZ[index]));
    }

    @Benchmark
    public void worldGuardRegionsAtRTree(Blackhole blackhole) {
        List<ProtectedRegion> found = new ArrayList<>();
        this.worldGuardRTree.applyContaining(this.queryVectors[this.nextPoint()], found::add);
        blackhole.consume(found);
    }

    @Benchmark
    public void worldGuardRegionsAtChunkTable(Blackhole blackhole) {
        List<ProtectedRegion> found = new ArrayList<>();
        this.worldGuardChunkTable.applyContaining(this.queryVectors[this.nextPoint()], found::add);
        blackhole.consume(found);
    }

    /**
     * What a flag check really needs: the first region that has something to say, not every region
     * covering the block. Hexagon stops there; WorldGuard's index has no way to stop early, so its
     * closest equivalent is asking for everything and taking the first.
     */
    @Benchmark
    public void hexagonHighestPriorityAt(Blackhole blackhole) {
        int index = this.nextPoint();
        blackhole.consume(this.hexagon.firstCovering(
                WORLD, this.queryX[index], this.queryY[index], this.queryZ[index], _ -> true));
    }

    private int nextPoint() {
        this.cursor = (this.cursor + 1) & (QUERY_POINTS - 1);
        return this.cursor;
    }
}
