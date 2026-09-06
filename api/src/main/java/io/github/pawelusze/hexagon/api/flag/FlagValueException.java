package io.github.pawelusze.hexagon.api.flag;

import org.jetbrains.annotations.NotNull;

/** Thrown when text cannot be parsed into a flag value. The message is safe to show to users. */
public class FlagValueException extends IllegalArgumentException {

    /**
     * Creates the exception.
     *
     * @param message a user-facing explanation of what was wrong with the input
     */
    public FlagValueException(@NotNull String message) {
        super(message);
    }
}
