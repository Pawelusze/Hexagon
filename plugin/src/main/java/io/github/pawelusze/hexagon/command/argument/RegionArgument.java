package io.github.pawelusze.hexagon.command.argument;

import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import io.github.pawelusze.hexagon.api.region.Region;
import io.github.pawelusze.hexagon.api.region.RegionId;
import io.github.pawelusze.hexagon.api.region.RegionService;
import io.github.pawelusze.hexagon.configuration.MessagesConfiguration;
import io.github.pawelusze.hexagon.text.Messenger;
import java.util.Optional;
import java.util.function.Supplier;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class RegionArgument extends ArgumentResolver<CommandSender, Region> {

    private final RegionService regions;
    private final Messenger messenger;
    private final Supplier<MessagesConfiguration> messages;

    public RegionArgument(RegionService regions, Messenger messenger, Supplier<MessagesConfiguration> messages) {
        this.regions = regions;
        this.messenger = messenger;
        this.messages = messages;
    }

    @Override
    protected @NotNull ParseResult<Region> parse(
            @NotNull Invocation<CommandSender> invocation, @NotNull Argument<Region> context, @NotNull String input) {
        if (!RegionId.isValid(input)) {
            return this.failure(this.messages.get().region.invalidId, input);
        }
        Optional<Region> region = this.regions.find(RegionId.of(input));
        if (region.isEmpty()) {
            return this.failure(this.messages.get().region.notFound, input);
        }
        return ParseResult.success(region.get());
    }

    private ParseResult<Region> failure(String template, String input) {
        return ParseResult.failure(this.messenger.message(template, Placeholder.unparsed("input", input)));
    }

    @Override
    public @NotNull SuggestionResult suggest(
            @NotNull Invocation<CommandSender> invocation,
            @NotNull Argument<Region> argument,
            @NotNull SuggestionContext context) {
        return this.regions.all().stream()
                .map(region -> region.id().value())
                .sorted()
                .collect(SuggestionResult.collector());
    }
}
