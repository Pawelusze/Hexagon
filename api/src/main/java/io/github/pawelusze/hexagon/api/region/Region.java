package io.github.pawelusze.hexagon.api.region;

import io.github.pawelusze.hexagon.api.flag.Flag;
import io.github.pawelusze.hexagon.api.flag.FlagMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * An immutable protected area. Every mutation returns a new instance; persist changes through
 * {@link RegionService#update(Region)}.
 *
 * @param id the unique identifier
 * @param world the key of the world the region lives in
 * @param bounds the cuboid the region covers
 * @param priority the priority; when regions overlap, the highest priority decides flag values
 * @param trustees the parties trusted in the region and their roles
 * @param flags the flags explicitly set on the region
 */
public record Region(
        @NotNull RegionId id,
        @NotNull Key world,
        @NotNull Bounds bounds,
        int priority,
        @NotNull Map<Trustee, Role> trustees,
        @NotNull FlagMap flags) {

    /**
     * Creates a region, defensively copying the trustee map.
     *
     * @param id the unique identifier
     * @param world the world key
     * @param bounds the cuboid
     * @param priority the priority
     * @param trustees the trusted parties
     * @param flags the explicitly set flags
     */
    public Region {
        trustees = Map.copyOf(trustees);
    }

    /**
     * Creates a region with priority zero, no trustees and no flags.
     *
     * @param id the unique identifier
     * @param world the world key
     * @param bounds the cuboid
     * @return the region
     */
    public static @NotNull Region of(@NotNull RegionId id, @NotNull Key world, @NotNull Bounds bounds) {
        return new Region(id, world, bounds, 0, Map.of(), FlagMap.empty());
    }

    /**
     * Tells whether a location is inside this region.
     *
     * @param location the location, including its world
     * @return true if the location's world matches and the block is inside the bounds
     */
    public boolean contains(@NotNull Location location) {
        return this.world.equals(location.getWorld().key())
                && this.bounds.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Returns the role directly granted to a trustee.
     *
     * @param trustee the trustee
     * @return the role, or empty if the trustee is not trusted
     */
    public @NotNull Optional<Role> roleOf(@NotNull Trustee trustee) {
        return Optional.ofNullable(this.trustees.get(trustee));
    }

    /**
     * Returns the value explicitly set for a flag on this region.
     *
     * @param flag the flag
     * @param <T> the flag value type
     * @return the value, or empty if the flag is not set
     */
    public <T> @NotNull Optional<T> flag(@NotNull Flag<T> flag) {
        return this.flags.get(flag);
    }

    /**
     * Returns a copy with different bounds.
     *
     * @param newBounds the bounds
     * @return the copy
     */
    public @NotNull Region withBounds(@NotNull Bounds newBounds) {
        return new Region(this.id, this.world, newBounds, this.priority, this.trustees, this.flags);
    }

    /**
     * Returns a copy with a different priority.
     *
     * @param newPriority the priority
     * @return the copy
     */
    public @NotNull Region withPriority(int newPriority) {
        return new Region(this.id, this.world, this.bounds, newPriority, this.trustees, this.flags);
    }

    /**
     * Returns a copy granting a role to a trustee, replacing any previous role.
     *
     * @param trustee the trustee
     * @param role the role
     * @return the copy
     */
    public @NotNull Region withTrustee(@NotNull Trustee trustee, @NotNull Role role) {
        Map<Trustee, Role> updated = new HashMap<>(this.trustees);
        updated.put(trustee, role);
        return new Region(this.id, this.world, this.bounds, this.priority, updated, this.flags);
    }

    /**
     * Returns a copy without the given trustee.
     *
     * @param trustee the trustee
     * @return the copy
     */
    public @NotNull Region withoutTrustee(@NotNull Trustee trustee) {
        Map<Trustee, Role> updated = new HashMap<>(this.trustees);
        updated.remove(trustee);
        return new Region(this.id, this.world, this.bounds, this.priority, updated, this.flags);
    }

    /**
     * Returns a copy with a flag set.
     *
     * @param flag the flag
     * @param value the value
     * @param <T> the flag value type
     * @return the copy
     */
    public <T> @NotNull Region withFlag(@NotNull Flag<T> flag, @NotNull T value) {
        return new Region(this.id, this.world, this.bounds, this.priority, this.trustees, this.flags.with(flag, value));
    }

    /**
     * Returns a copy with a flag cleared.
     *
     * @param flag the flag
     * @return the copy
     */
    public @NotNull Region withoutFlag(@NotNull Flag<?> flag) {
        return new Region(this.id, this.world, this.bounds, this.priority, this.trustees, this.flags.without(flag));
    }
}
