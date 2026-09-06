package io.github.pawelusze.hexagon.command;

import io.github.pawelusze.hexagon.api.flag.Flag;
import io.github.pawelusze.hexagon.api.flag.FlagMap;
import io.github.pawelusze.hexagon.api.region.Bounds;
import io.github.pawelusze.hexagon.api.region.GroupTrustee;
import io.github.pawelusze.hexagon.api.region.PlayerTrustee;
import io.github.pawelusze.hexagon.api.region.Region;
import io.github.pawelusze.hexagon.api.region.RegionId;
import io.github.pawelusze.hexagon.api.region.Role;
import io.github.pawelusze.hexagon.api.region.Trustee;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.GameRule;
import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;

/**
 * The placeholders Hexagon messages understand. Every command builds them here, so a message
 * written for one command keeps working when another command sends it.
 */
public final class CommandPlaceholders {

    /** How a permission group is written in commands and messages, as in {@code group:vip}. */
    public static final String GROUP_PREFIX = "group:";

    private CommandPlaceholders() {}

    public static @NotNull TagResolver region(@NotNull Region region) {
        return region(region.id());
    }

    public static @NotNull TagResolver region(@NotNull RegionId id) {
        return Placeholder.unparsed("region", id.value());
    }

    public static @NotNull TagResolver size(@NotNull Bounds bounds) {
        return Placeholder.unparsed("size", Long.toString(bounds.volume()));
    }

    public static @NotNull TagResolver world(@NotNull String name) {
        return Placeholder.unparsed("world", name);
    }

    public static @NotNull TagResolver flag(@NotNull Flag<?> flag) {
        return Placeholder.unparsed("flag", flag.name());
    }

    /** What a region carries, as {@code pvp (deny), block-break (allow)}. */
    public static @NotNull TagResolver flags(@NotNull Region region) {
        FlagMap set = region.flags();
        String listed = set.flags().stream()
                .sorted(Comparator.comparing(Flag::name))
                .map(flag -> flag.name() + " (" + set.serialized(flag).orElse("") + ")")
                .collect(Collectors.joining(", "));
        return Placeholder.unparsed("flags", listed.isEmpty() ? "no flags set" : listed);
    }

    /** The values a flag accepts, for flags whose type knows them; free text has none to list. */
    public static @NotNull TagResolver accepts(@NotNull Flag<?> flag) {
        List<String> suggestions = flag.type().suggestions();
        return Placeholder.unparsed("accepts", suggestions.isEmpty() ? "any text" : String.join(", ", suggestions));
    }

    public static @NotNull TagResolver gameRule(@NotNull GameRule<?> rule) {
        return Placeholder.unparsed("rule", rule.getName());
    }

    public static @NotNull TagResolver value(@NotNull String value) {
        return Placeholder.unparsed("value", value);
    }

    public static @NotNull TagResolver input(@NotNull String input) {
        return Placeholder.unparsed("input", input);
    }

    public static @NotNull TagResolver trustee(@NotNull Server server, @NotNull Trustee trustee) {
        return Placeholder.unparsed("trustee", displayName(server, trustee));
    }

    private static String displayName(Server server, Trustee trustee) {
        return switch (trustee) {
            case GroupTrustee(String group) -> GROUP_PREFIX + group;
            case PlayerTrustee(UUID playerId) -> {
                String name = server.getOfflinePlayer(playerId).getName();
                yield name != null ? name : playerId.toString();
            }
        };
    }

    public static @NotNull TagResolver role(@NotNull Role role) {
        return Placeholder.unparsed("role", role.name().toLowerCase(Locale.ROOT));
    }

    public static @NotNull TagResolver priority(int priority) {
        return Placeholder.unparsed("priority", Integer.toString(priority));
    }

    public static @NotNull TagResolver page(int page) {
        return Placeholder.unparsed("page", Integer.toString(page));
    }

    public static @NotNull TagResolver pages(int pages) {
        return Placeholder.unparsed("pages", Integer.toString(pages));
    }
}
