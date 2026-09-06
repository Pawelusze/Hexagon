package io.github.pawelusze.hexagon.api.region;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BoundsTest {

    @Test
    void betweenNormalisesCorners() {
        Bounds bounds = Bounds.between(new BlockPoint(10, 64, -5), new BlockPoint(-3, 70, 2));

        assertThat(bounds.min()).isEqualTo(new BlockPoint(-3, 64, -5));
        assertThat(bounds.max()).isEqualTo(new BlockPoint(10, 70, 2));
    }

    @Test
    void containsIsInclusiveOnBorders() {
        Bounds bounds = Bounds.between(new BlockPoint(0, 0, 0), new BlockPoint(4, 4, 4));

        assertThat(bounds.contains(0, 0, 0)).isTrue();
        assertThat(bounds.contains(4, 4, 4)).isTrue();
        assertThat(bounds.contains(5, 4, 4)).isFalse();
        assertThat(bounds.contains(2, -1, 2)).isFalse();
    }

    @Test
    void volumeCountsEveryBlock() {
        Bounds bounds = Bounds.between(new BlockPoint(0, 0, 0), new BlockPoint(1, 2, 3));

        assertThat(bounds.volume()).isEqualTo(2L * 3 * 4);
    }

    @Test
    void centerHandlesNegativeCoordinates() {
        Bounds bounds = Bounds.between(new BlockPoint(-10, 0, -10), new BlockPoint(-1, 0, -1));

        assertThat(bounds.center()).isEqualTo(new BlockPoint(-6, 0, -6));
    }

    @Test
    void rejectsInvertedCorners() {
        assertThatThrownBy(() -> new Bounds(new BlockPoint(1, 0, 0), new BlockPoint(0, 0, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
