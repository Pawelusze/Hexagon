package io.github.pawelusze.hexagon.api.region;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegionIdTest {

    @Test
    void lowerCasesInput() {
        assertThat(RegionId.of("Spawn_Area-1").value()).isEqualTo("spawn_area-1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-spawn", "has space", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "ünïcode"})
    void rejectsInvalidIds(String raw) {
        assertThat(RegionId.isValid(raw)).isFalse();
        assertThatThrownBy(() -> RegionId.of(raw)).isInstanceOf(IllegalArgumentException.class);
    }
}
