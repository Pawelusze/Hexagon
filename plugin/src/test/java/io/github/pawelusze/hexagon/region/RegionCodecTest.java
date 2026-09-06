package io.github.pawelusze.hexagon.region;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.pawelusze.hexagon.api.flag.Flag;
import io.github.pawelusze.hexagon.api.flag.FlagScope;
import io.github.pawelusze.hexagon.api.flag.State;
import io.github.pawelusze.hexagon.api.region.BlockPoint;
import io.github.pawelusze.hexagon.api.region.Bounds;
import io.github.pawelusze.hexagon.api.region.Region;
import io.github.pawelusze.hexagon.api.region.RegionId;
import io.github.pawelusze.hexagon.api.region.Role;
import io.github.pawelusze.hexagon.api.region.Trustee;
import io.github.pawelusze.hexagon.flag.DefaultFlagRegistry;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

class RegionCodecTest {

    private static final Flag<State> PVP = Flag.state("pvp", FlagScope.EVERYONE);
    private static final Flag<String> GREETING = Flag.text("greeting");

    private final DefaultFlagRegistry registry = new DefaultFlagRegistry();
    private final RegionCodec codec = new RegionCodec(registry);

    @Test
    void roundTripsEveryField() throws SerializationException {
        registry.register(PVP);
        registry.register(GREETING);
        UUID owner = UUID.randomUUID();
        Region region = Region.of(
                        RegionId.of("spawn"),
                        Key.key("minecraft", "overworld"),
                        Bounds.between(new BlockPoint(-10, 0, -10), new BlockPoint(10, 100, 10)))
                .withPriority(7)
                .withTrustee(Trustee.player(owner), Role.OWNER)
                .withTrustee(Trustee.group("VIP"), Role.GUEST)
                .withFlag(PVP, State.DENY)
                .withFlag(GREETING, "<green>Welcome to <region>");

        CommentedConfigurationNode node = CommentedConfigurationNode.root();
        codec.encode(region, node);

        assertThat(codec.decode(node)).isEqualTo(region);
        assertThat(node.node("trustees", "group:vip").getString()).isEqualTo("guest");
        assertThat(node.node("flags", "pvp").getString()).isEqualTo("deny");
    }

    @Test
    void reportsAMalformedWorldKeyAsASerializationProblem() throws SerializationException {
        CommentedConfigurationNode node = CommentedConfigurationNode.root();
        node.node("id").set("plot");
        node.node("world").set("Not A Key");
        node.node("bounds", "min", "x").set(0);
        node.node("bounds", "max", "x").set(0);

        assertThatThrownBy(() -> codec.decode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Not A Key");
    }

    @Test
    void dropsUnknownFlagsInsteadOfFailing() throws SerializationException {
        CommentedConfigurationNode node = CommentedConfigurationNode.root();
        node.node("id").set("plot");
        node.node("world").set("minecraft:overworld");
        node.node("bounds", "min", "x").set(0);
        node.node("bounds", "max", "x").set(0);
        node.node("flags", "from-another-plugin").set("whatever");

        Region decoded = codec.decode(node);

        assertThat(decoded.flags().isEmpty()).isTrue();
        assertThat(decoded.priority()).isZero();
    }
}
