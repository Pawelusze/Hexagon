package io.github.pawelusze.hexagon.api.region;

import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Unique, case-insensitive identifier of a region.
 *
 * <p>Identifiers are stored lower-cased and may only contain letters, digits, hyphens and
 * underscores. They are at most {@value #MAX_LENGTH} characters long.
 *
 * @param value the normalised identifier
 */
public record RegionId(@NotNull String value) {

    /** Maximum number of characters in an identifier. */
    public static final int MAX_LENGTH = 32;

    private static final Pattern VALID = Pattern.compile("[a-z0-9][a-z0-9_-]{0," + (MAX_LENGTH - 1) + "}");

    /**
     * Creates an identifier from an already normalised value.
     *
     * @param value the identifier
     * @throws IllegalArgumentException if the value is not a valid identifier
     */
    public RegionId {
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid region id '" + value + "': use 1-" + MAX_LENGTH
                    + " lowercase letters, digits, '-' or '_'");
        }
    }

    /**
     * Normalises user input into an identifier.
     *
     * @param raw the raw input, in any letter case
     * @return the identifier
     * @throws IllegalArgumentException if the input is not a valid identifier
     */
    public static @NotNull RegionId of(@NotNull String raw) {
        return new RegionId(raw.toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * Tells whether the input would form a valid identifier.
     *
     * @param raw the raw input, in any letter case
     * @return true if {@link #of(String)} would succeed
     */
    public static boolean isValid(@NotNull String raw) {
        return VALID.matcher(raw.toLowerCase(java.util.Locale.ROOT)).matches();
    }

    @Override
    public @NotNull String toString() {
        return value;
    }
}
