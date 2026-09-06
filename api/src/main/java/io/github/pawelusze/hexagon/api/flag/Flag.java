package io.github.pawelusze.hexagon.api.flag;

import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * A typed setting that regions can carry. Flags are identified by name inside a {@link
 * FlagRegistry}; the type defines how values are parsed and stored, and the scope defines which
 * players the flag applies to.
 *
 * <p>Other plugins may define their own flags and register them at startup.
 *
 * @param name the unique name: lowercase letters, digits and hyphens
 * @param type the value type
 * @param scope the players the flag applies to
 * @param <T> the value type
 */
public record Flag<T>(
        @NotNull String name,
        @NotNull FlagType<T> type,
        @NotNull FlagScope scope) {

    private static final Pattern VALID_NAME = Pattern.compile("[a-z][a-z0-9-]*");

    /**
     * Creates a flag.
     *
     * @param name the unique name
     * @param type the value type
     * @param scope the players the flag applies to
     * @throws IllegalArgumentException if the name is not lowercase-kebab-case
     */
    public Flag {
        if (!VALID_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "Invalid flag name '" + name + "': use lowercase letters, digits and '-'");
        }
    }

    /**
     * Creates an {@code allow}/{@code deny} flag.
     *
     * @param name the unique name
     * @param scope the players the flag applies to
     * @return the flag
     */
    public static @NotNull Flag<State> state(@NotNull String name, @NotNull FlagScope scope) {
        return new Flag<>(name, FlagTypes.state(), scope);
    }

    /**
     * Creates a free-text flag, such as a greeting message.
     *
     * @param name the unique name
     * @return the flag
     */
    public static @NotNull Flag<String> text(@NotNull String name) {
        return new Flag<>(name, FlagTypes.text(), FlagScope.EVERYONE);
    }

    @Override
    public @NotNull String toString() {
        return name;
    }
}
