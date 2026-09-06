package io.github.pawelusze.hexagon.text;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pawelusze.hexagon.configuration.MessagesConfiguration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class MessengerTest {

    private final Messenger messenger = new Messenger();

    @Test
    void leavesMiniMessageUntouched() {
        assertThat(Messenger.legacyToMiniMessage("<green>Hello <player>")).isEqualTo("<green>Hello <player>");
    }

    @Test
    void rendersTheShippedPaletteIncludingHexClosingTags() {
        Component rendered = messenger.render(new MessagesConfiguration().prefix);

        assertThat(PlainTextComponentSerializer.plainText().serialize(rendered)).isEqualTo("[Hexagon] ");
    }

    @Test
    void keepsUnfilledPlaceholdersReadable() {
        Component rendered = messenger.render("<white>/hx trust <region> <player> [role]");

        assertThat(PlainTextComponentSerializer.plainText().serialize(rendered))
                .isEqualTo("/hx trust <region> <player> [role]");
    }

    @Test
    void translatesLegacyCodesIncludingHex() {
        assertThat(Messenger.legacyToMiniMessage("&aHi &l&#ff8800there &r!"))
                .isEqualTo("<green>Hi <bold><#ff8800>there <reset>!");
    }

    @Test
    void rendersLegacyTemplatesWithPlaceholders() {
        Component rendered = messenger.render("&cNo entry to <region>", Placeholder.unparsed("region", "spawn"));

        assertThat(PlainTextComponentSerializer.plainText().serialize(rendered)).isEqualTo("No entry to spawn");
        assertThat(rendered.color()).isEqualTo(NamedTextColor.RED);
    }

    @Test
    void prefixIsPrependedToMessages() {
        messenger.prefix("<gray>[B] ");

        Component message = messenger.message("<white>hello");

        assertThat(PlainTextComponentSerializer.plainText().serialize(message)).isEqualTo("[B] hello");
    }
}
