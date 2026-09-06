package io.github.pawelusze.hexagon.api.region;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Integer block coordinates inside a world.
 *
 * @param x the block x coordinate
 * @param y the block y coordinate
 * @param z the block z coordinate
 */
public record BlockPoint(int x, int y, int z) {

    /**
     * Creates a point from the block containing the location.
     *
     * @param location the location
     * @return the block point
     */
    public static @NotNull BlockPoint of(@NotNull Location location) {
        return new BlockPoint(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Returns the component-wise minimum of this point and another.
     *
     * @param other the other point
     * @return a point with the smaller coordinate on every axis
     */
    public @NotNull BlockPoint min(@NotNull BlockPoint other) {
        return new BlockPoint(Math.min(x, other.x), Math.min(y, other.y), Math.min(z, other.z));
    }

    /**
     * Returns the component-wise maximum of this point and another.
     *
     * @param other the other point
     * @return a point with the larger coordinate on every axis
     */
    public @NotNull BlockPoint max(@NotNull BlockPoint other) {
        return new BlockPoint(Math.max(x, other.x), Math.max(y, other.y), Math.max(z, other.z));
    }

    @Override
    public @NotNull String toString() {
        return x + ", " + y + ", " + z;
    }
}
