package io.github.pawelusze.hexagon.api.flag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FlagMapTest {

    private static final Flag<State> PVP = Flag.state("pvp", FlagScope.EVERYONE);
    private static final Flag<String> GREETING = Flag.text("greeting");

    @Test
    void storesTypedValuesImmutably() {
        FlagMap empty = FlagMap.empty();
        FlagMap withPvp = empty.with(PVP, State.DENY);

        assertThat(empty.isEmpty()).isTrue();
        assertThat(withPvp.get(PVP)).contains(State.DENY);
        assertThat(withPvp.get(GREETING)).isEmpty();
        assertThat(withPvp.serialized(PVP)).contains("deny");
    }

    @Test
    void withoutReturnsSameInstanceWhenAbsent() {
        FlagMap map = FlagMap.empty().with(GREETING, "hi");

        assertThat(map.without(PVP)).isSameAs(map);
        assertThat(map.without(GREETING).isEmpty()).isTrue();
    }

    @Test
    void scopeExemptionsFollowRoles() {
        assertThat(FlagScope.EVERYONE.exempts(io.github.pawelusze.hexagon.api.region.Role.OWNER))
                .isFalse();
        assertThat(FlagScope.NON_MEMBERS.exempts(io.github.pawelusze.hexagon.api.region.Role.GUEST))
                .isFalse();
        assertThat(FlagScope.NON_MEMBERS.exempts(io.github.pawelusze.hexagon.api.region.Role.MEMBER))
                .isTrue();
        assertThat(FlagScope.OUTSIDERS.exempts(io.github.pawelusze.hexagon.api.region.Role.GUEST))
                .isTrue();
    }
}
