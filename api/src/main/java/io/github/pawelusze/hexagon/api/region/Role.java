package io.github.pawelusze.hexagon.api.region;

import org.jetbrains.annotations.NotNull;

/**
 * Level of trust a party holds inside a region. Roles are ordered from least to most trusted.
 *
 * <ul>
 *   <li>{@link #GUEST} may enter and leave, but is otherwise treated as an outsider.
 *   <li>{@link #MEMBER} additionally bypasses flags scoped to non-members, such as building.
 *   <li>{@link #OWNER} additionally manages the region through commands.
 * </ul>
 */
public enum Role {
    GUEST,
    MEMBER,
    OWNER;

    /**
     * Tells whether this role is at least as trusted as another.
     *
     * @param other the role to compare with
     * @return true if this role is equal to or above {@code other}
     */
    public boolean atLeast(@NotNull Role other) {
        return this.compareTo(other) >= 0;
    }
}
