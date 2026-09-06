package io.github.pawelusze.hexagon.command.argument;

import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import io.github.pawelusze.hexagon.api.flag.Flag;
import io.github.pawelusze.hexagon.api.flag.GameRuleFlags;
import io.github.pawelusze.hexagon.configuration.MessagesConfiguration;
import io.github.pawelusze.hexagon.text.Messenger;
import java.util.function.Supplier;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.GameRule;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/** Accepts the vanilla name of a game rule Hexagon can enforce per region, such as keepInventory. */
public final class GameRuleArgument extends ArgumentResolver<CommandSender, GameRule<Boolean>> {

    private final Messenger messenger;
    private final Supplier<MessagesConfiguration> messages;

    public GameRuleArgument(@NotNull Messenger messenger, @NotNull Supplier<MessagesConfiguration> messages) {
        this.messenger = messenger;
        this.messages = messages;
    }

    @Override
    protected @NotNull ParseResult<GameRule<Boolean>> parse(
            @NotNull Invocation<CommandSender> invocation,
            @NotNull Argument<GameRule<Boolean>> context,
            @NotNull String input) {
        GameRule<?> rule = GameRule.getByName(input);
        if (rule == null) {
            return this.failure(this.messages.get().gameRule.unknown, Placeholder.unparsed("input", input));
        }
        return GameRuleFlags.coveringFlag(rule)
                .map(flag -> this.useTheFlagInstead(rule, flag))
                .orElseGet(() -> this.supportedRule(rule));
    }

    private ParseResult<GameRule<Boolean>> useTheFlagInstead(GameRule<?> rule, Flag<?> flag) {
        return failure(
                this.messages.get().gameRule.coveredByFlag,
                Placeholder.unparsed("rule", rule.getName()),
                Placeholder.unparsed("flag", flag.name()));
    }

    private ParseResult<GameRule<Boolean>> supportedRule(GameRule<?> rule) {
        return GameRuleFlags.supportedRules().stream()
                .filter(candidate -> candidate.getName().equals(rule.getName()))
                .findFirst()
                .<ParseResult<GameRule<Boolean>>>map(ParseResult::success)
                .orElseGet(() -> this.failure(
                        this.messages.get().gameRule.worldWide, Placeholder.unparsed("rule", rule.getName())));
    }

    private ParseResult<GameRule<Boolean>> failure(String template, TagResolver... placeholders) {
        return ParseResult.failure(this.messenger.message(template, placeholders));
    }

    @Override
    public @NotNull SuggestionResult suggest(
            @NotNull Invocation<CommandSender> invocation,
            @NotNull Argument<GameRule<Boolean>> argument,
            @NotNull SuggestionContext context) {
        return GameRuleFlags.supportedRules().stream().map(GameRule::getName).collect(SuggestionResult.collector());
    }
}
