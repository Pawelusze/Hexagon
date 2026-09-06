package io.github.pawelusze.hexagon.configuration;

import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.util.NamingSchemes;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/**
 * A YAML file backed by a {@code @ConfigSerializable} class. Loading fills missing keys with the
 * class defaults and writes the completed file back, so new options appear after upgrades.
 *
 * @param <T> the configuration type
 */
public final class YamlConfigurationFile<T> {

    private static final ObjectMapper.Factory MAPPER = ObjectMapper.factoryBuilder()
            .defaultNamingScheme(NamingSchemes.LOWER_CASE_DASHED)
            .build();

    private final Class<T> type;
    private final YamlConfigurationLoader loader;

    public YamlConfigurationFile(@NotNull Class<T> type, @NotNull Path path, @NotNull String header) {
        this.type = type;
        this.loader = YamlConfigurationLoader.builder()
                .path(path)
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .defaultOptions(options -> options.header(header)
                        .implicitInitialization(true)
                        .serializers(serializers -> serializers.registerAnnotatedObjects(MAPPER)))
                .build();
    }

    public @NotNull T load() throws ConfigurateException {
        CommentedConfigurationNode node = this.loader.load();
        T value = node.get(this.type);
        if (value == null) {
            throw new ConfigurateException("Could not map " + this.type.getSimpleName());
        }
        node.set(this.type, value);
        this.loader.save(node);
        return value;
    }
}
