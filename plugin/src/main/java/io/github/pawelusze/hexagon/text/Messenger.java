package io.github.pawelusze.hexagon.text;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

/**
 * Renders message templates and sends them. Templates are MiniMessage first; legacy {@code &}
 * colour codes are translated to MiniMessage tags so both styles can be mixed freely.
 */
public final class Messenger {

    private static final Pattern LEGACY_CODE = Pattern.compile("[&§](#[0-9a-fA-F]{6}|[0-9a-fA-Fk-oK-OrR])");
    private static final Map<Character, String> LEGACY_TAGS = Map.ofEntries(
            Map.entry('0', "<black>"),
            Map.entry('1', "<dark_blue>"),
            Map.entry('2', "<dark_green>"),
            Map.entry('3', "<dark_aqua>"),
            Map.entry('4', "<dark_red>"),
            Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"),
            Map.entry('7', "<gray>"),
            Map.entry('8', "<dark_gray>"),
            Map.entry('9', "<blue>"),
            Map.entry('a', "<green>"),
            Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"),
            Map.entry('d', "<light_purple>"),
            Map.entry('e', "<yellow>"),
            Map.entry('f', "<white>"),
            Map.entry('k', "<obfuscated>"),
            Map.entry('l', "<bold>"),
            Map.entry('m', "<strikethrough>"),
            Map.entry('n', "<underlined>"),
            Map.entry('o', "<italic>"),
            Map.entry('r', "<reset>"));

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private volatile Component prefix = Component.empty();

    public void prefix(@NotNull String template) {
        this.prefix = this.render(template);
    }

    public @NotNull Component render(@NotNull String template, @NotNull TagResolver... placeholders) {
        return this.miniMessage.deserialize(legacyToMiniMessage(template), placeholders);
    }

    public void send(@NotNull Audience audience, @NotNull String template, @NotNull TagResolver... placeholders) {
        if (template.isBlank()) {
            return;
        }
        audience.sendMessage(this.message(template, placeholders));
    }

    /** Sends a line as written, without the prefix, for blocks of text such as the help. */
    public void sendUnprefixed(@NotNull Audience audience, @NotNull String template) {
        if (template.isBlank()) {
            return;
        }
        audience.sendMessage(this.render(template));
    }

    public @NotNull Component message(@NotNull String template, @NotNull TagResolver... placeholders) {
        return this.prefix.append(this.render(template, placeholders));
    }

    static @NotNull String legacyToMiniMessage(@NotNull String template) {
        return LEGACY_CODE.matcher(template).replaceAll(match -> tagFor(match.group(1)));
    }

    private static String tagFor(String code) {
        return code.length() == 1
                ? LEGACY_TAGS.get(Character.toLowerCase(code.charAt(0)))
                : "<" + code.toLowerCase(Locale.ROOT) + ">";
    }
}
