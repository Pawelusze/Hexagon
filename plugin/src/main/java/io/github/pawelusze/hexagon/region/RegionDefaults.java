package io.github.pawelusze.hexagon.region;

import io.github.pawelusze.hexagon.api.flag.FlagMap;
import org.jetbrains.annotations.NotNull;

/**
 * What a freshly created region starts with, as configured in {@code config.yml}.
 *
 * @param priority the priority given to a region the create command does not set one for
 * @param flags the flags applied to every new region
 */
public record RegionDefaults(int priority, @NotNull FlagMap flags) {}
