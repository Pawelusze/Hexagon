package io.github.pawelusze.hexagon.command.argument;

import org.jetbrains.annotations.NotNull;

/**
 * The text a player typed for a flag, before the flag's own type reads it. A type of its own, so
 * the command can offer the values that particular flag accepts while the player is still typing.
 */
public record FlagValue(@NotNull String text) {}
