package io.github.pawelusze.hexagon.api.flag;

import io.github.pawelusze.hexagon.api.region.Role;
import org.jetbrains.annotations.NotNull;

/** Describes which players a flag applies to, based on their role in the defining region. */
public enum FlagScope {
    /** Applies to everyone, trusted or not. Used for gameplay rules such as PvP. */
    EVERYONE,
    /** Applies to players below {@link Role#MEMBER}. Used for protection such as building. */
    NON_MEMBERS,
    /** Applies only to players holding no role at all. Used for entry and exit. */
    OUTSIDERS;

    /**
     * Tells whether a player holding the given role is exempt from flags of this scope.
     *
     * @param role the player's role in the defining region
     * @return true if the flag does not apply to that player
     */
    public boolean exempts(@NotNull Role role) {
        return switch (this) {
            case EVERYONE -> false;
            case NON_MEMBERS -> role.atLeast(Role.MEMBER);
            case OUTSIDERS -> true;
        };
    }
}
