package io.github.pawelusze.hexagon.api.flag;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Converts flag values to and from their textual form, used both by commands and by storage.
 *
 * @param <T> the value type
 */
public interface FlagType<T> {

    /**
     * Parses text into a value.
     *
     * @param input the text, as typed by a user or read from storage
     * @return the value
     * @throws FlagValueException if the text is not a valid value
     */
    @NotNull
    T parse(@NotNull String input);

    /**
     * Renders a value as text that {@link #parse(String)} accepts.
     *
     * @param value the value
     * @return the textual form
     */
    @NotNull
    String serialize(@NotNull T value);

    /**
     * Returns example values offered to users while they type.
     *
     * @return suggestions, possibly empty
     */
    @NotNull
    List<String> suggestions();
}
