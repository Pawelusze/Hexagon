package io.github.pawelusze.hexagon.command.argument;

import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.MultipleArgumentResolver;
import dev.rollczi.litecommands.input.raw.RawInput;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.range.Range;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import io.github.pawelusze.hexagon.api.flag.Flag;
import io.github.pawelusze.hexagon.api.flag.FlagRegistry;
import java.util.List;
import java.util.Optional;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Reads the rest of the line as a flag value, and completes it with the values the flag already
 * typed on that line accepts, so nobody has to remember whether a flag wants allow or true.
 */
public final class FlagValueArgument implements MultipleArgumentResolver<CommandSender, FlagValue> {

    private final FlagRegistry flags;

    public FlagValueArgument(@NotNull FlagRegistry flags) {
        this.flags = flags;
    }

    @Override
    public @NotNull ParseResult<FlagValue> parse(
            @NotNull Invocation<CommandSender> invocation,
            @NotNull Argument<FlagValue> argument,
            @NotNull RawInput input) {
        List<String> words = input.nextAll();
        if (words.isEmpty()) {
            return ParseResult.failure("Missing value");
        }

        return ParseResult.success(new FlagValue(String.join(" ", words)));
    }

    @Override
    public @NotNull Range getRange(@NotNull Argument<FlagValue> argument) {
        return Range.moreThan(1);
    }

    @Override
    public @NotNull SuggestionResult suggest(
            @NotNull Invocation<CommandSender> invocation,
            @NotNull Argument<FlagValue> argument,
            @NotNull SuggestionContext context) {
        return this.flagBeingSet(invocation)
                .map(flag -> flag.type().suggestions().stream().collect(SuggestionResult.collector()))
                .orElseGet(SuggestionResult::empty);
    }

    /** The last word on the line that names a flag; the words after it are the value being typed. */
    private Optional<Flag<?>> flagBeingSet(Invocation<CommandSender> invocation) {
        List<String> words = invocation.arguments().asList();
        for (int index = words.size() - 1; index >= 0; index--) {
            Optional<Flag<?>> flag = this.flags.find(words.get(index));
            if (flag.isPresent()) {
                return flag;
            }
        }
        return Optional.empty();
    }
}
