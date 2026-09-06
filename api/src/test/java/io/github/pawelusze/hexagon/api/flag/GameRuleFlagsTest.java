package io.github.pawelusze.hexagon.api.flag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bukkit.GameRule;
import org.junit.jupiter.api.Test;

class GameRuleFlagsTest {

    @Test
    void namesFlagsAfterTheRuleInKebabCase() {
        assertThat(GameRuleFlags.of(GameRule.KEEP_INVENTORY).name()).isEqualTo("keep-inventory");
        assertThat(GameRuleFlags.of(GameRule.DO_FIRE_TICK).name()).isEqualTo("do-fire-tick");
        assertThat(GameRuleFlags.of(GameRule.MOB_GRIEFING).name()).isEqualTo("mob-griefing");
    }

    @Test
    void acceptsBothShapesBukkitUsesForARuleName() {
        assertThat(GameRuleFlags.flagName("keepInventory")).isEqualTo("keep-inventory");
        assertThat(GameRuleFlags.flagName("minecraft:keep_inventory")).isEqualTo("keep-inventory");
        assertThat(GameRuleFlags.flagName("minecraft:tnt_explosion_drop_decay")).isEqualTo("tnt-explosion-drop-decay");
    }

    @Test
    void everySupportedRuleHasABooleanFlag() {
        assertThat(GameRuleFlags.all()).hasSameSizeAs(GameRuleFlags.supportedRules());
        assertThat(GameRuleFlags.all())
                .allSatisfy(flag -> assertThat(flag.type()).isSameAs(FlagTypes.bool()))
                .allSatisfy(flag -> assertThat(GameRuleFlags.isGameRule(flag)).isTrue());
    }

    @Test
    void refusesRulesThatOnlyExistForAWholeWorld() {
        assertThat(GameRuleFlags.find(GameRule.DO_DAYLIGHT_CYCLE)).isEmpty();
        assertThat(GameRuleFlags.isWorldWide(GameRule.DO_DAYLIGHT_CYCLE)).isTrue();
        assertThat(GameRuleFlags.isWorldWide(GameRule.RANDOM_TICK_SPEED)).isTrue();
        assertThatThrownBy(() -> GameRuleFlags.of(GameRule.DO_DAYLIGHT_CYCLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("doDaylightCycle");
    }

    @Test
    void sendsRulesThatAFlagAlreadyCoversToThatFlag() {
        assertThat(GameRuleFlags.coveringFlag(GameRule.PVP)).contains(Flags.PVP);
        assertThat(GameRuleFlags.isWorldWide(GameRule.PVP)).isFalse();
        assertThat(GameRuleFlags.find(GameRule.PVP)).isEmpty();
    }

    @Test
    void coversTheRulesPlayersAskAboutMost() {
        assertThat(GameRuleFlags.supportedRules())
                .contains(
                        GameRule.KEEP_INVENTORY,
                        GameRule.MOB_GRIEFING,
                        GameRule.DO_FIRE_TICK,
                        GameRule.DO_MOB_SPAWNING,
                        GameRule.DO_TILE_DROPS,
                        GameRule.TNT_EXPLODES)
                .doesNotHaveDuplicates();
    }

    @Test
    void doesNotClaimHexagonsOwnFlags() {
        assertThat(GameRuleFlags.isGameRule(Flags.PVP)).isFalse();
        assertThat(GameRuleFlags.isGameRule(Flags.GREETING)).isFalse();
    }
}
