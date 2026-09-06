package io.github.pawelusze.hexagon.api.region;

import java.util.Collection;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Lifecycle of regions.
 *
 * <p>Call these from the server thread. Creating, updating and deleting a region touches the map
 * of regions and the spatial index one after the other, so two threads changing regions at once
 * could leave a lookup seeing a region in neither. Writing the files happens on a background
 * thread afterwards, and {@link RegionQuery} stays safe to read from anywhere.
 */
public interface RegionService {

    /**
     * Finds a region by identifier.
     *
     * @param id the identifier
     * @return the region, or empty if none exists
     */
    @NotNull
    Optional<Region> find(@NotNull RegionId id);

    /**
     * Returns every region on the server.
     *
     * @return an unmodifiable snapshot of all regions
     */
    @NotNull
    Collection<Region> all();

    /**
     * Returns every region in a world.
     *
     * @param world the world key
     * @return an unmodifiable snapshot of the world's regions
     */
    @NotNull
    Collection<Region> in(@NotNull Key world);

    /**
     * Creates and persists a region, applying the server's configured default flags.
     *
     * @param id the identifier, which must be unused
     * @param world the world key
     * @param bounds the cuboid
     * @return the created region
     * @throws IllegalArgumentException if a region with the identifier already exists
     */
    @NotNull
    Region create(@NotNull RegionId id, @NotNull Key world, @NotNull Bounds bounds);

    /**
     * Replaces the stored region that has the same identifier and persists the change.
     *
     * @param region the new state
     * @throws IllegalArgumentException if no region with that identifier exists
     */
    void update(@NotNull Region region);

    /**
     * Deletes a region.
     *
     * @param id the identifier
     * @return true if a region was deleted, false if none existed
     */
    boolean delete(@NotNull RegionId id);
}
