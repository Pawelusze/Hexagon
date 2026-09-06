package io.github.pawelusze.hexagon.api.flag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class AccessRuleTest {

    @Test
    void universalRulesApplyToEverything() {
        assertThat(AccessRule.<String>allow().allows("stone")).isTrue();
        assertThat(AccessRule.<String>deny().allows("stone")).isFalse();
    }

    @Test
    void denyOnlyBlocksListedTargetsOnly() {
        AccessRule<String> rule = AccessRule.denyOnly(Set.of("tnt"));

        assertThat(rule.allows("tnt")).isFalse();
        assertThat(rule.allows("stone")).isTrue();
    }

    @Test
    void allowOnlyPermitsListedTargetsOnly() {
        AccessRule<String> rule = AccessRule.allowOnly(Set.of("stone", "dirt"));

        assertThat(rule.allows("stone")).isTrue();
        assertThat(rule.allows("tnt")).isFalse();
    }

    @Test
    void rejectsMoreThanMaximumTargets() {
        Set<String> tooMany = IntStream.rangeClosed(0, AccessRule.MAX_TARGETS)
                .mapToObj(Integer::toString)
                .collect(Collectors.toSet());

        assertThatThrownBy(() -> AccessRule.denyOnly(tooMany)).isInstanceOf(IllegalArgumentException.class);
    }
}
