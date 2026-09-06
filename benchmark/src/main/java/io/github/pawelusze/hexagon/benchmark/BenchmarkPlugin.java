package io.github.pawelusze.hexagon.benchmark;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import io.github.pawelusze.hexagon.api.Hexagon;
import io.github.pawelusze.hexagon.api.HexagonApi;
import io.github.pawelusze.hexagon.api.flag.Flags;
import io.github.pawelusze.hexagon.api.flag.State;
import io.github.pawelusze.hexagon.api.region.BlockPoint;
import io.github.pawelusze.hexagon.api.region.Bounds;
import io.github.pawelusze.hexagon.api.region.Region;
import io.github.pawelusze.hexagon.api.region.RegionId;
import io.github.pawelusze.hexagon.api.region.RegionQuery;
import java.util.Arrays;
import java.util.Random;
import java.util.function.LongSupplier;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Times Hexagon and WorldGuard on the server they are both running on, asking each the two
 * questions every protection event comes down to: which regions cover this block, and does a flag
 * stop something here.
 *
 * <p>Regions are spread over an area sized so that a block is covered by about two of them, the
 * way a real map looks. Queries land only in loaded chunks, because that is where events happen
 * and where WorldGuard has had the chance to warm its chunk cache, exactly as it would in play.
 * Everything runs on the main thread and the server stands still for the few seconds it takes.
 */
public final class BenchmarkPlugin extends JavaPlugin {

    private static final int DEFAULT_REGIONS = 10_000;
    private static final int DEFAULT_QUERIES = 100_000;
    private static final int REGION_SIZE = 48;
    private static final int PRIORITIES = 10;
    private static final int WARMUP_ROUNDS = 3;
    private static final int MEASURED_ROUNDS = 5;
    private static final long SEED = 20260906L;
    private static final String ID_PREFIX = "bench-";

    /** Chunks kept loaded around spawn, so queries have somewhere to land on an empty server. */
    private static final int LOADED_RADIUS = 8;

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        int regions = argument(args, 0, DEFAULT_REGIONS);
        int queries = argument(args, 1, DEFAULT_QUERIES);
        if (regions <= 0 || queries <= 0) {
            sender.sendMessage("Usage: /hexbench [regions] [queries], both positive.");
            return true;
        }

        World world = getServer().getWorlds().getFirst();
        RegionManager worldGuard =
                WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (worldGuard == null) {
            sender.sendMessage("WorldGuard has no region manager for " + world.getName() + ".");
            return true;
        }

        HexagonApi hexagon = Hexagon.api();
        sender.sendMessage("Creating " + regions + " regions in both plugins, then running " + queries
                + " queries per round; the server will pause for a moment.");
        this.keepChunksLoaded(world, true);
        try {
            this.createRegions(sender, hexagon, worldGuard, world, regions);
            this.run(sender, hexagon.query(), worldGuard, world, queries);
        } finally {
            this.deleteRegions(hexagon, worldGuard, regions);
            this.keepChunksLoaded(world, false);
        }
        return true;
    }

    private void keepChunksLoaded(World world, boolean keep) {
        Location spawn = world.getSpawnLocation();
        for (int chunkX = -LOADED_RADIUS; chunkX <= LOADED_RADIUS; chunkX++) {
            for (int chunkZ = -LOADED_RADIUS; chunkZ <= LOADED_RADIUS; chunkZ++) {
                int x = (spawn.getBlockX() >> 4) + chunkX;
                int z = (spawn.getBlockZ() >> 4) + chunkZ;
                if (keep) {
                    world.addPluginChunkTicket(x, z, this);
                } else {
                    world.removePluginChunkTicket(x, z, this);
                }
            }
        }
    }

    private void createRegions(
            CommandSender sender, HexagonApi hexagon, RegionManager worldGuard, World world, int count) {
        Random random = new Random(SEED);
        Location spawn = world.getSpawnLocation();
        // Area in which a block is covered by roughly two regions.
        int radius = (int) Math.sqrt((double) count * REGION_SIZE * REGION_SIZE / 2) / 2;
        int[] xs = new int[count];
        int[] zs = new int[count];
        int[] priorities = new int[count];
        for (int index = 0; index < count; index++) {
            xs[index] = spawn.getBlockX() + random.nextInt(-radius, radius);
            zs[index] = spawn.getBlockZ() + random.nextInt(-radius, radius);
            priorities[index] = random.nextInt(PRIORITIES);
        }

        long startedAt = System.nanoTime();
        for (int index = 0; index < count; index++) {
            Region created = hexagon.regions()
                    .create(
                            RegionId.of(ID_PREFIX + index),
                            world.key(),
                            new Bounds(
                                    new BlockPoint(xs[index], 0, zs[index]),
                                    new BlockPoint(xs[index] + REGION_SIZE, 255, zs[index] + REGION_SIZE)));
            hexagon.regions().update(created.withPriority(priorities[index]).withFlag(Flags.PVP, State.DENY));
        }
        long hexagonMillis = (System.nanoTime() - startedAt) / 1_000_000L;

        startedAt = System.nanoTime();
        for (int index = 0; index < count; index++) {
            ProtectedCuboidRegion cuboid = new ProtectedCuboidRegion(
                    ID_PREFIX + index,
                    BlockVector3.at(xs[index], 0, zs[index]),
                    BlockVector3.at(xs[index] + REGION_SIZE, 255, zs[index] + REGION_SIZE));
            cuboid.setPriority(priorities[index]);
            cuboid.setFlag(com.sk89q.worldguard.protection.flags.Flags.PVP, StateFlag.State.DENY);
            worldGuard.addRegion(cuboid);
        }
        long worldGuardMillis = (System.nanoTime() - startedAt) / 1_000_000L;

        sender.sendMessage(String.format(
                "Created %d regions through each API: Hexagon %d ms, WorldGuard %d ms.",
                count, hexagonMillis, worldGuardMillis));
    }

    private void deleteRegions(HexagonApi hexagon, RegionManager worldGuard, int count) {
        for (int index = 0; index < count; index++) {
            hexagon.regions().delete(RegionId.of(ID_PREFIX + index));
            worldGuard.removeRegion(ID_PREFIX + index);
        }
    }

    private void run(CommandSender sender, RegionQuery hexagon, RegionManager worldGuard, World world, int queries) {
        Random random = new Random(SEED);
        Chunk[] loaded = world.getLoadedChunks();
        Location[] locations = new Location[queries];
        BlockVector3[] vectors = new BlockVector3[queries];
        for (int index = 0; index < queries; index++) {
            Chunk chunk = loaded[random.nextInt(loaded.length)];
            int x = (chunk.getX() << 4) + random.nextInt(16);
            int y = random.nextInt(256);
            int z = (chunk.getZ() << 4) + random.nextInt(16);
            locations[index] = new Location(world, x, y, z);
            vectors[index] = BlockVector3.at(x, y, z);
        }

        sender.sendMessage(String.format(
                "%d regions, %d loaded chunks, %d queries per round, median of %d rounds:",
                worldGuard.size(), loaded.length, queries, MEASURED_ROUNDS));
        report(sender, queries, "regions at a block", "Hexagon", () -> {
            long found = 0;
            for (Location location : locations) {
                found += hexagon.at(location).size();
            }
            return found;
        });
        report(sender, queries, "regions at a block", "WorldGuard", () -> {
            long found = 0;
            for (BlockVector3 vector : vectors) {
                found += worldGuard.getApplicableRegions(vector).size();
            }
            return found;
        });
        report(sender, queries, "pvp flag at a block", "Hexagon", () -> {
            long allowed = 0;
            for (Location location : locations) {
                allowed += hexagon.allows(location, Flags.PVP) ? 1 : 0;
            }
            return allowed;
        });
        report(sender, queries, "pvp flag at a block", "WorldGuard", () -> {
            long allowed = 0;
            for (BlockVector3 vector : vectors) {
                StateFlag.State state = worldGuard
                        .getApplicableRegions(vector)
                        .queryState(null, com.sk89q.worldguard.protection.flags.Flags.PVP);
                allowed += state == StateFlag.State.DENY ? 0 : 1;
            }
            return allowed;
        });
    }

    /** Runs one loop over every query, a few times untimed and then timed, and prints the median. */
    private static void report(CommandSender sender, int queries, String question, String plugin, LongSupplier round) {
        long checksum = 0;
        for (int warmup = 0; warmup < WARMUP_ROUNDS; warmup++) {
            checksum += round.getAsLong();
        }

        long[] nanos = new long[MEASURED_ROUNDS];
        for (int measured = 0; measured < MEASURED_ROUNDS; measured++) {
            long startedAt = System.nanoTime();
            checksum += round.getAsLong();
            nanos[measured] = System.nanoTime() - startedAt;
        }
        Arrays.sort(nanos);

        sender.sendMessage(String.format(
                "  %-20s %-11s %,7.1f ns/op   (checksum %d)",
                question, plugin, (double) nanos[MEASURED_ROUNDS / 2] / queries, checksum));
    }

    private static int argument(String[] args, int index, int fallback) {
        if (args.length <= index) {
            return fallback;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException _) {
            return -1;
        }
    }
}
