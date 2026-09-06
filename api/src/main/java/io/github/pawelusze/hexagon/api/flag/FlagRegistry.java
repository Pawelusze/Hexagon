package io.github.pawelusze.hexagon.api.flag;

import java.util.Collection;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Registry of every flag known to the server. Hexagon registers its built-in {@link Flags}
 * before loading regions; other plugins should register theirs while enabling, and must do so
 * before any region using the flag is loaded from storage.
 */
public interface FlagRegistry {

    /**
     * Registers a flag.
     *
     * @param flag the flag
     * @throws IllegalArgumentException if a flag with the same name is already registered
     */
    void register(@NotNull Flag<?> flag);

    /**
     * Finds a flag by name.
     *
     * @param name the flag name, in any letter case
     * @return the flag, or empty if unknown
     */
    @NotNull
    Optional<Flag<?>> find(@NotNull String name);

    /**
     * Returns every registered flag, ordered by name.
     *
     * @return an unmodifiable collection of flags
     */
    @NotNull
    Collection<Flag<?>> all();
}
