package io.github.pawelusze.hexagon.api.region;

import java.util.Locale;
import org.jetbrains.annotations.NotNull;

/**
 * A permission group trusted in a region. Every player inheriting the group holds the granted
 * role.
 *
 * @param name the group name, normalised to lower case
 */
public record GroupTrustee(@NotNull String name) implements Trustee {

    /**
     * Creates a group trustee, normalising the name to lower case.
     *
     * @param name the group name
     * @throws IllegalArgumentException if the name is blank
     */
    public GroupTrustee {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Group name must not be blank");
        }
        name = name.toLowerCase(Locale.ROOT);
    }
}
