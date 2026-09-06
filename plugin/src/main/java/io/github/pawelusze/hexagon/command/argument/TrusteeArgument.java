package io.github.pawelusze.hexagon.command.argument;

import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import io.github.pawelusze.hexagon.api.region.Trustee;
import io.github.pawelusze.hexagon.command.CommandPlaceholders;
import io.github.pawelusze.hexagon.configuration.MessagesConfig;
import io.github.pawelusze.hexagon.membership.GroupMembership;
import io.github.pawelusze.hexagon.text.Messenger;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Parses {@code group:<name>} into a group trustee and anything else into a player trustee. Player
 * lookups never touch the network: the player must be online, cached, or given as a UUID.
 */
public final class TrusteeArgument extends ArgumentResolver<CommandSender, Trustee> {

    private final Server server;
    private final GroupMembership groups;
    private final Messenger messenger;
    private final Supplier<MessagesConfig> messages;

    public TrusteeArgument(
            Server server, GroupMembership groups, Messenger messenger, Supplier<MessagesConfig> messages) {
        this.server = server;
        this.groups = groups;
        this.messenger = messenger;
        this.messages = messages;
    }

    @Override
    protected @NotNull ParseResult<Trustee> parse(
            @NotNull Invocation<CommandSender> invocation, @NotNull Argument<Trustee> context, @NotNull String input) {
        if (input.regionMatches(
                true, 0, CommandPlaceholders.GROUP_PREFIX, 0, CommandPlaceholders.GROUP_PREFIX.length())) {
            return this.parseGroup(input.substring(CommandPlaceholders.GROUP_PREFIX.length()));
        }
        return this.parsePlayer(input);
    }

    private ParseResult<Trustee> parseGroup(String name) {
        Collection<String> known = groups.knownGroups();
        if (name.isBlank() || (!known.isEmpty() && !known.contains(name.toLowerCase(Locale.ROOT)))) {
            return this.failure(messages.get().trust.unknownGroup, name);
        }
        return ParseResult.success(Trustee.group(name));
    }

    private ParseResult<Trustee> parsePlayer(String input) {
        Player online = server.getPlayerExact(input);
        if (online != null) {
            return ParseResult.success(Trustee.player(online.getUniqueId()));
        }
        OfflinePlayer cached = server.getOfflinePlayerIfCached(input);
        if (cached != null) {
            return ParseResult.success(Trustee.player(cached.getUniqueId()));
        }
        try {
            return ParseResult.success(Trustee.player(UUID.fromString(input)));
        } catch (IllegalArgumentException _) {
            return this.failure(messages.get().trust.unknownPlayer, input);
        }
    }

    private ParseResult<Trustee> failure(String template, String input) {
        return ParseResult.failure(messenger.message(template, Placeholder.unparsed("input", input)));
    }

    @Override
    public @NotNull SuggestionResult suggest(
            @NotNull Invocation<CommandSender> invocation,
            @NotNull Argument<Trustee> argument,
            @NotNull SuggestionContext context) {
        Stream<String> players = server.getOnlinePlayers().stream().map(Player::getName);
        Stream<String> groupNames = groups.knownGroups().stream().map(name -> CommandPlaceholders.GROUP_PREFIX + name);
        return Stream.concat(players, groupNames).collect(SuggestionResult.collector());
    }
}
