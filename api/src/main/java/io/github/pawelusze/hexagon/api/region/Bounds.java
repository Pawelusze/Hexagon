package io.github.pawelusze.hexagon.api.region;

import org.jetbrains.annotations.NotNull;

/**
 * Axis-aligned cuboid described by its two extreme corners, both inclusive.
 *
 * @param min the corner with the smallest coordinates on every axis
 * @param max the corner with the largest coordinates on every axis
 */
public record Bounds(@NotNull BlockPoint min, @NotNull BlockPoint max) {

    /**
     * Creates bounds from already ordered corners.
     *
     * @param min the minimum corner
     * @param max the maximum corner
     * @throws IllegalArgumentException if any coordinate of {@code min} exceeds {@code max}
     */
    public Bounds {
        if (min.x() > max.x() || min.y() > max.y() || min.z() > max.z()) {
            throw new IllegalArgumentException("Bounds min " + min + " exceeds max " + max);
        }
    }

    /**
     * Creates bounds spanning two arbitrary corners.
     *
     * @param first one corner
     * @param second the opposite corner
     * @return bounds enclosing both corners
     */
    public static @NotNull Bounds between(@NotNull BlockPoint first, @NotNull BlockPoint second) {
        return new Bounds(first.min(second), first.max(second));
    }

    /**
     * Tells whether a block position lies inside these bounds.
     *
     * @param x the block x coordinate
     * @param y the block y coordinate
     * @param z the block z coordinate
     * @return true if the position is inside, borders included
     */
    public boolean contains(int x, int y, int z) {
        return x >= this.min.x()
                && x <= this.max.x()
                && y >= this.min.y()
                && y <= this.max.y()
                && z >= this.min.z()
                && z <= this.max.z();
    }

    /**
     * Tells whether a block point lies inside these bounds.
     *
     * @param point the block point
     * @return true if the point is inside, borders included
     */
    public boolean contains(@NotNull BlockPoint point) {
        return this.contains(point.x(), point.y(), point.z());
    }

    /**
     * Returns the number of blocks enclosed by these bounds.
     *
     * @return the volume in blocks
     */
    public long volume() {
        return (long) this.sizeX() * this.sizeY() * this.sizeZ();
    }

    /**
     * Returns the size along the x axis, in blocks.
     *
     * @return the width
     */
    public int sizeX() {
        return this.max.x() - this.min.x() + 1;
    }

    /**
     * Returns the size along the y axis, in blocks.
     *
     * @return the height
     */
    public int sizeY() {
        return this.max.y() - this.min.y() + 1;
    }

    /**
     * Returns the size along the z axis, in blocks.
     *
     * @return the depth
     */
    public int sizeZ() {
        return this.max.z() - this.min.z() + 1;
    }

    /**
     * Returns the block closest to the geometric centre of these bounds.
     *
     * @return the centre point
     */
    public @NotNull BlockPoint center() {
        return new BlockPoint(
                Math.floorDiv(this.min.x() + this.max.x(), 2),
                Math.floorDiv(this.min.y() + this.max.y(), 2),
                Math.floorDiv(this.min.z() + this.max.z(), 2));
    }
}
