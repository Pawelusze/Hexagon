package io.github.pawelusze.hexagon.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlConfigurationFileTest {

    @TempDir
    Path directory;

    @Test
    void writesDefaultsWithHeaderOnFirstLoad() throws Exception {
        Path file = directory.resolve("config.yml");

        PluginConfiguration config =
                new YamlConfigurationFile<>(PluginConfiguration.class, file, PluginConfiguration.HEADER).load();

        assertThat(config.defaultPriority).isZero();
        assertThat(config.defaultFlags).containsEntry("block-break", "deny");
        assertThat(Files.readString(file))
                .startsWith("# Hexagon configuration.")
                .contains("default-priority: 0");
    }

    @Test
    void keepsUserValuesAndFillsMissingKeys() throws Exception {
        Path file = directory.resolve("config.yml");
        Files.writeString(file, "default-priority: 5\ndefault-flags:\n  pvp: deny\n");

        PluginConfiguration config =
                new YamlConfigurationFile<>(PluginConfiguration.class, file, PluginConfiguration.HEADER).load();

        assertThat(config.defaultPriority).isEqualTo(5);
        assertThat(config.defaultFlags).containsExactlyEntriesOf(Map.of("pvp", "deny"));
        assertThat(config.listPageSize).isEqualTo(10);
        assertThat(Files.readString(file)).contains("list-page-size: 10");
    }
}
