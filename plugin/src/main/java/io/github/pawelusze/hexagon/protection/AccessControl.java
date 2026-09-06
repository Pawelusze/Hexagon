package io.github.pawelusze.hexagon.protection;

import io.github.pawelusze.hexagon.api.flag.AccessRule;
import io.github.pawelusze.hexagon.api.flag.Flag;
import io.github.pawelusze.hexagon.api.flag.State;
import io.github.pawelusze.hexagon.api.region.Region;
import io.github.pawelusze.hexagon.api.region.RegionQuery;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Decides whether a flag stops a player somewhere, honouring roles and the bypass permission.
 *
 * <p>The place is given as a block or an entity rather than a {@link Location}, because a block
 * event carries its block and an entity carries its coordinates, while {@code getLocation()}
 * allocates a new object on every call — and these questions are asked for every block broken,
 * placed or walked over.
 *
 * <p>The flag is resolved before the permission is read: most blocks a player touches lie in no
 * region at all, and an index lookup is cheaper than walking a permission tree.
 */
public final class AccessControl {

    private final RegionQuery query;

    public AccessControl(@NotNull RegionQuery query) {
        this.query = query;
    }

    public boolean denies(@NotNull Player player, @NotNull Block block, @NotNull Flag<State> flag) {
        return this.denies(player, block.getWorld().key(), block.getX(), block.getY(), block.getZ(), flag);
    }

    public <E> boolean denies(
            @NotNull Player player, @NotNull Block block, @NotNull Flag<AccessRule<E>> flag, @NotNull E element) {
        return this.denies(player, block.getWorld().key(), block.getX(), block.getY(), block.getZ(), flag, element);
    }

    /** Whether the flag stops the player at the block the entity is standing in. */
    public boolean denies(@NotNull Player player, @NotNull Entity at, @NotNull Flag<State> flag) {
        return this.denies(
                player, at.getWorld().key(), blockOf(at.getX()), blockOf(at.getY()), blockOf(at.getZ()), flag);
    }

    public <E> boolean denies(
            @NotNull Player player, @NotNull Entity at, @NotNull Flag<AccessRule<E>> flag, @NotNull E element) {
        Key world = at.getWorld().key();
        return this.denies(player, world, blockOf(at.getX()), blockOf(at.getY()), blockOf(at.getZ()), flag, element);
    }

    public boolean denies(@NotNull Player player, @NotNull Location location, @NotNull Flag<State> flag) {
        Key world = location.getWorld().key();
        return this.denies(player, world, location.getBlockX(), location.getBlockY(), location.getBlockZ(), flag);
    }

    /** Whether the flag, as set on this very region, stops the player; used when crossing its border. */
    public boolean denies(@NotNull Player player, @NotNull Region region, @NotNull Flag<State> flag) {
        if (region.flag(flag).orElse(State.ALLOW).isAllowed()) {
            return false;
        }

        boolean exempt =
                this.query.roleOf(player, region).map(flag.scope()::exempts).orElse(false);
        return !exempt && obeysFlags(player);
    }

    private boolean denies(Player player, Key world, int x, int y, int z, Flag<State> flag) {
        Optional<State> state = this.query.resolve(world, x, y, z, flag, player);
        return state.isPresent() && !state.get().isAllowed() && obeysFlags(player);
    }

    private <E> boolean denies(Player player, Key world, int x, int y, int z, Flag<AccessRule<E>> flag, E element) {
        Optional<AccessRule<E>> rule = this.query.resolve(world, x, y, z, flag, player);
        return rule.isPresent() && !rule.get().allows(element) && obeysFlags(player);
    }

    /** A player holding the bypass permission is stopped by nothing. */
    private static boolean obeysFlags(Player player) {
        return !player.hasPermission("hexagon.bypass");
    }

    private static int blockOf(double coordinate) {
        return Location.locToBlock(coordinate);
    }
}
