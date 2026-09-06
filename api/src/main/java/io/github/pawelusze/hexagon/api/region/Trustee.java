package io.github.pawelusze.hexagon.api.region;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * A party that can be granted a {@link Role} in a region: either a single player or a permission
 * group resolved through the server's permission plugin (LuckPerms when available).
 */
public sealed interface Trustee permits PlayerTrustee, GroupTrustee {

    /**
     * Creates a trustee for a single player.
     *
     * @param playerId the player's unique id
     * @return the trustee
     */
    static @NotNull Trustee player(@NotNull UUID playerId) {
        return new PlayerTrustee(playerId);
    }

    /**
     * Creates a trustee for a permission group.
     *
     * @param groupName the group name, in any letter case
     * @return the trustee
     */
    static @NotNull Trustee group(@NotNull String groupName) {
        return new GroupTrustee(groupName);
    }
}
