package io.github.pawelusze.hexagon.api;

import io.github.pawelusze.hexagon.api.flag.FlagRegistry;
import io.github.pawelusze.hexagon.api.region.RegionQuery;
import io.github.pawelusze.hexagon.api.region.RegionService;
import org.jetbrains.annotations.NotNull;

/**
 * Root of the Hexagon API. Obtain it through {@link Hexagon#api()}.
 *
 * <p>The API is split by responsibility: {@link #regions()} manages the lifecycle of regions,
 * {@link #query()} answers spatial and flag questions, and {@link #flags()} holds every flag the
 * server knows about, including flags contributed by other plugins.
 */
@SuppressWarnings("unused") // Every method here exists for plugins that depend on Hexagon.
public interface HexagonApi {

    /**
     * Returns the service responsible for creating, updating and deleting regions.
     *
     * @return the region service
     */
    @NotNull
    RegionService regions();

    /**
     * Returns the query facade used to find regions at a location and to resolve flag values.
     *
     * @return the region query
     */
    @NotNull
    RegionQuery query();

    /**
     * Returns the registry of known flags.
     *
     * @return the flag registry
     */
    @NotNull
    FlagRegistry flags();
}
