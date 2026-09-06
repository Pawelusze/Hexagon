package io.github.pawelusze.hexagon.api.region;

import io.github.pawelusze.hexagon.api.flag.AccessRule;
import io.github.pawelusze.hexagon.api.flag.Flag;
import io.github.pawelusze.hexagon.api.flag.State;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Read-only questions about regions: which regions cover a point and what a flag resolves to.
 *
 * <p>Every method here may be called from any thread: the index behind them is concurrent and
 * regions are immutable, so a query either sees a region as it was before a change or as it is
 * after one. Changing regions is a different matter, and {@link RegionService} says so.
 *
 * <p>Resolution rule: among the regions covering a location, ordered by descending priority, the
 * first region that explicitly sets the flag defines its value. When a subject is supplied, the
 * subject's role in that defining region is checked against the flag's {@link
 * io.github.pawelusze.hexagon.api.flag.FlagScope}; an exempt subject sees no value at all, which callers treat as
 * "allowed".
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted") // Callers invert them; a public API reads better positively.
public interface RegionQuery {

    /**
     * Returns the regions covering a block, highest priority first.
     *
     * @param world the world key
     * @param x the block x coordinate
     * @param y the block y coordinate
     * @param z the block z coordinate
     * @return the covering regions, possibly empty
     */
    @NotNull
    List<Region> at(@NotNull Key world, int x, int y, int z);

    /**
     * Returns the regions covering a block of a loaded world, highest priority first.
     *
     * <p>Prefer this over the {@link Key} form when a {@link World} is at hand: a Bukkit world
     * builds a new key each time it is asked for one, and this method does not ask.
     *
     * @param world the world
     * @param x the block x coordinate
     * @param y the block y coordinate
     * @param z the block z coordinate
     * @return the covering regions, possibly empty
     */
    @NotNull
    List<Region> at(@NotNull World world, int x, int y, int z);

    /**
     * Returns the regions covering a location, highest priority first.
     *
     * @param location the location
     * @return the covering regions, possibly empty
     */
    default @NotNull List<Region> at(@NotNull Location location) {
        return this.at(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Returns the highest-priority region covering a location.
     *
     * @param location the location
     * @return the region, or empty if no region covers the location
     */
    default @NotNull Optional<Region> topAt(@NotNull Location location) {
        return this.at(location).stream().findFirst();
    }

    /**
     * Resolves the effective role of a player in a region, taking group membership into account.
     * When a player is trusted both directly and through groups, the highest role wins.
     *
     * @param player the player
     * @param region the region
     * @return the role, or empty if the player is an outsider
     */
    @NotNull
    Optional<Role> roleOf(@NotNull Player player, @NotNull Region region);

    /**
     * Resolves a flag at a block, ignoring who asks.
     *
     * @param world the world key
     * @param x the block x coordinate
     * @param y the block y coordinate
     * @param z the block z coordinate
     * @param flag the flag
     * @param <T> the flag value type
     * @return the value, or empty if no covering region sets the flag
     */
    <T> @NotNull Optional<T> resolve(@NotNull Key world, int x, int y, int z, @NotNull Flag<T> flag);

    /**
     * Resolves a flag at a block for a subject, honouring the flag's scope.
     *
     * @param world the world key
     * @param x the block x coordinate
     * @param y the block y coordinate
     * @param z the block z coordinate
     * @param flag the flag
     * @param subject the player the flag would apply to
     * @param <T> the flag value type
     * @return the value, or empty if no covering region sets the flag or the subject is exempt
     */
    <T> @NotNull Optional<T> resolve(
            @NotNull Key world, int x, int y, int z, @NotNull Flag<T> flag, @NotNull Player subject);

    /**
     * Resolves a flag at a block of a loaded world, ignoring who asks.
     *
     * @param world the world
     * @param x the block x coordinate
     * @param y the block y coordinate
     * @param z the block z coordinate
     * @param flag the flag
     * @param <T> the flag value type
     * @return the value, or empty if no covering region sets the flag
     */
    <T> @NotNull Optional<T> resolve(@NotNull World world, int x, int y, int z, @NotNull Flag<T> flag);

    /**
     * Resolves a flag at a block of a loaded world for a subject, honouring the flag's scope.
     *
     * @param world the world
     * @param x the block x coordinate
     * @param y the block y coordinate
     * @param z the block z coordinate
     * @param flag the flag
     * @param subject the player the flag would apply to
     * @param <T> the flag value type
     * @return the value, or empty if no covering region sets the flag or the subject is exempt
     */
    <T> @NotNull Optional<T> resolve(
            @NotNull World world, int x, int y, int z, @NotNull Flag<T> flag, @NotNull Player subject);

    /**
     * Resolves a flag at a location, ignoring who asks.
     *
     * @param location the location
     * @param flag the flag
     * @param <T> the flag value type
     * @return the value, or empty if no covering region sets the flag
     */
    default <T> @NotNull Optional<T> resolve(@NotNull Location location, @NotNull Flag<T> flag) {
        return this.resolve(
                location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), flag);
    }

    /**
     * Resolves a flag at a location for a subject, honouring the flag's scope.
     *
     * @param location the location
     * @param flag the flag
     * @param subject the player the flag would apply to
     * @param <T> the flag value type
     * @return the value, or empty if no covering region sets the flag or the subject is exempt
     */
    default <T> @NotNull Optional<T> resolve(
            @NotNull Location location, @NotNull Flag<T> flag, @NotNull Player subject) {
        return this.resolve(
                location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), flag, subject);
    }

    /**
     * Tells whether a state flag allows an action at a block of a loaded world, ignoring who asks.
     *
     * @param world the world
     * @param x the block x coordinate
     * @param y the block y coordinate
     * @param z the block z coordinate
     * @param flag the flag
     * @return false only if the flag resolves to {@link State#DENY}
     */
    default boolean allows(@NotNull World world, int x, int y, int z, @NotNull Flag<State> flag) {
        return this.resolve(world, x, y, z, flag).orElse(State.ALLOW).isAllowed();
    }

    /**
     * Tells whether a state flag allows an action at a location, ignoring who asks.
     *
     * @param location the location
     * @param flag the flag
     * @return false only if the flag resolves to {@link State#DENY}
     */
    default boolean allows(@NotNull Location location, @NotNull Flag<State> flag) {
        return this.resolve(location, flag).orElse(State.ALLOW).isAllowed();
    }

    /**
     * Tells whether a state flag allows a subject to act at a location.
     *
     * @param location the location
     * @param flag the flag
     * @param subject the acting player
     * @return false only if the flag resolves to {@link State#DENY} for the subject
     */
    default boolean allows(@NotNull Location location, @NotNull Flag<State> flag, @NotNull Player subject) {
        return this.resolve(location, flag, subject).orElse(State.ALLOW).isAllowed();
    }

    /**
     * Tells whether an access-rule flag allows a subject to act on an element at a location.
     *
     * @param location the location
     * @param flag the flag
     * @param element the element being acted on, for example a block material
     * @param subject the acting player
     * @param <E> the element type
     * @return false only if the resolved rule denies the element for the subject
     */
    default <E> boolean allows(
            @NotNull Location location,
            @NotNull Flag<AccessRule<E>> flag,
            @NotNull E element,
            @NotNull Player subject) {
        Optional<AccessRule<E>> rule = this.resolve(location, flag, subject);
        return rule.isEmpty() || rule.get().allows(element);
    }
}
