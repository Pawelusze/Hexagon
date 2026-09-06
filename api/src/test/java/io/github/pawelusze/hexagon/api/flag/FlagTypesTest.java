package io.github.pawelusze.hexagon.api.flag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FlagTypesTest {

    private enum Block {
        STONE,
        DIRT,
        TNT
    }

    private final FlagType<AccessRule<Block>> rule = FlagTypes.accessRule(
            name -> {
                try {
                    return Optional.of(Block.valueOf(name.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException _) {
                    return Optional.empty();
                }
            },
            block -> block.name().toLowerCase(Locale.ROOT));

    @Test
    void stateAcceptsAliases() {
        assertThat(FlagTypes.state().parse("ALLOW")).isEqualTo(State.ALLOW);
        assertThat(FlagTypes.state().parse("off")).isEqualTo(State.DENY);
        assertThatThrownBy(() -> FlagTypes.state().parse("maybe")).isInstanceOf(FlagValueException.class);
    }

    @Test
    void ruleParsesUniversalForms() {
        assertThat(rule.parse("deny")).isEqualTo(AccessRule.deny());
        assertThat(rule.parse("deny *")).isEqualTo(AccessRule.deny());
        assertThat(rule.parse("allow all")).isEqualTo(AccessRule.allow());
    }

    @Test
    void ruleParsesTargetsSeparatedByCommasOrSpaces() {
        assertThat(rule.parse("deny tnt,dirt")).isEqualTo(AccessRule.denyOnly(Set.of(Block.TNT, Block.DIRT)));
        assertThat(rule.parse("allow stone dirt")).isEqualTo(AccessRule.allowOnly(Set.of(Block.STONE, Block.DIRT)));
    }

    @Test
    void ruleRejectsUnknownTargets() {
        assertThatThrownBy(() -> rule.parse("deny bedrock"))
                .isInstanceOf(FlagValueException.class)
                .hasMessageContaining("bedrock");
    }

    @Test
    void ruleRoundTripsThroughSerialization() {
        AccessRule<Block> parsed = rule.parse("deny tnt, stone");

        String serialized = rule.serialize(parsed);

        assertThat(serialized).isEqualTo("deny stone,tnt");
        assertThat(rule.parse(serialized)).isEqualTo(parsed);
    }

    @Test
    void booleanAcceptsVanillaAndFriendlySpellings() {
        assertThat(FlagTypes.bool().parse("true")).isTrue();
        assertThat(FlagTypes.bool().parse("OFF")).isFalse();
        assertThat(FlagTypes.bool().serialize(true)).isEqualTo("true");
        assertThatThrownBy(() -> FlagTypes.bool().parse("maybe")).isInstanceOf(FlagValueException.class);
    }

    @Test
    void textRejectsBlankValues() {
        assertThatThrownBy(() -> FlagTypes.text().parse("  ")).isInstanceOf(FlagValueException.class);
    }
}
