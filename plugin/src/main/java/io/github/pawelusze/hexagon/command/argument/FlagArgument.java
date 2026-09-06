package io.github.pawelusze.hexagon.command.argument;

import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import io.github.pawelusze.hexagon.api.flag.Flag;
import io.github.pawelusze.hexagon.api.flag.FlagRegistry;
import io.github.pawelusze.hexagon.configuration.MessagesConfiguration;
import io.github.pawelusze.hexagon.text.Messenger;
import java.util.function.Supplier;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class FlagArgument extends ArgumentResolver<CommandSender, Flag<?>> {

    private final FlagRegistry flags;
    private final Messenger messenger;
    private final Supplier<MessagesConfiguration> messages;

    public FlagArgument(FlagRegistry flags, Messenger messenger, Supplier<MessagesConfiguration> messages) {
        this.flags = flags;
        this.messenger = messenger;
        this.messages = messages;
    }

    @Override
    protected @NotNull ParseResult<Flag<?>> parse(
            @NotNull Invocation<CommandSender> invocation, @NotNull Argument<Flag<?>> context, @NotNull String input) {
        return this.flags
                .find(input)
                .<ParseResult<Flag<?>>>map(ParseResult::success)
                .orElseGet(() -> ParseResult.failure(this.messenger.message(
                        this.messages.get().flag.unknown, Placeholder.unparsed("input", input))));
    }

    @Override
    public @NotNull SuggestionResult suggest(
            @NotNull Invocation<CommandSender> invocation,
            @NotNull Argument<Flag<?>> argument,
            @NotNull SuggestionContext context) {
        return this.flags.all().stream().map(Flag::name).collect(SuggestionResult.collector());
    }
}
