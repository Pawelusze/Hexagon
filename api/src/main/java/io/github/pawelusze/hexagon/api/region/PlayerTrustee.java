package io.github.pawelusze.hexagon.api.region;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * A single player trusted in a region.
 *
 * @param playerId the player's unique id
 */
public record PlayerTrustee(@NotNull UUID playerId) implements Trustee {}
