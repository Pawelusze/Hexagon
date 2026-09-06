package io.github.pawelusze.hexagon;

import io.github.pawelusze.hexagon.api.HexagonApi;
import io.github.pawelusze.hexagon.api.flag.FlagRegistry;
import io.github.pawelusze.hexagon.api.region.RegionQuery;
import io.github.pawelusze.hexagon.api.region.RegionService;
import org.jetbrains.annotations.NotNull;

record DefaultHexagonApi(
        @NotNull RegionService regions,
        @NotNull RegionQuery query,
        @NotNull FlagRegistry flags) implements HexagonApi {}
