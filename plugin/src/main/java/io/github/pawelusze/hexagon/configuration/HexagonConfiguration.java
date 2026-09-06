package io.github.pawelusze.hexagon.configuration;

import io.github.pawelusze.hexagon.api.flag.Flag;
import io.github.pawelusze.hexagon.api.flag.FlagMap;
import io.github.pawelusze.hexagon.api.flag.FlagRegistry;
import io.github.pawelusze.hexagon.api.flag.FlagValueException;
import io.github.pawelusze.hexagon.region.RegionDefaults;
import java.nio.file.Path;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurateException;

/**
 * The two files Hexagon reads, and what it makes of them.
 *
 * <p>{@link #reload()} replaces all three values at once. Everything else only reads them, from
 * any thread, and always sees a complete set rather than a half-applied reload.
 */
public final class HexagonConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(HexagonConfiguration.class);

    private final Path directory;
    private final FlagRegistry flags;

    private volatile PluginConfiguration plugin = new PluginConfiguration();
    private volatile MessagesConfiguration messages = new MessagesConfiguration();
    private volatile RegionDefaults regionDefaults = new RegionDefaults(0, FlagMap.empty());

    public HexagonConfiguration(@NotNull Path directory, @NotNull FlagRegistry flags) {
        this.directory = directory;
        this.flags = flags;
    }

    /**
     * Reads both files, filling in anything they are missing.
     *
     * @return false when a file could not be read, leaving the previous values in place
     */
    public boolean reload() {
        PluginConfiguration loadedPlugin;
        MessagesConfiguration loadedMessages;
        try {
            loadedPlugin = this.read(PluginConfiguration.class, "config.yml", PluginConfiguration.HEADER);
            loadedMessages = this.read(MessagesConfiguration.class, "messages.yml", MessagesConfiguration.HEADER);
        } catch (ConfigurateException exception) {
            LOG.error("Could not read the configuration", exception);
            return false;
        }

        this.plugin = loadedPlugin;
        this.messages = loadedMessages;
        this.regionDefaults =
                new RegionDefaults(loadedPlugin.defaultPriority, this.parseFlags(loadedPlugin.defaultFlags));
        return true;
    }

    public @NotNull PluginConfiguration plugin() {
        return this.plugin;
    }

    public @NotNull MessagesConfiguration messages() {
        return this.messages;
    }

    public @NotNull RegionDefaults regionDefaults() {
        return this.regionDefaults;
    }

    private <T> T read(Class<T> type, String fileName, String header) throws ConfigurateException {
        return new YamlConfigurationFile<>(type, this.directory.resolve(fileName), header).load();
    }

    private FlagMap parseFlags(Map<String, String> configured) {
        FlagMap defaults = FlagMap.empty();

        for (Map.Entry<String, String> entry : configured.entrySet()) {
            Flag<?> flag = this.flags.find(entry.getKey()).orElse(null);
            if (flag == null) {
                LOG.warn("config.yml: unknown default flag '{}'", entry.getKey());
                continue;
            }

            try {
                defaults = withParsed(defaults, flag, entry.getValue());
            } catch (FlagValueException exception) {
                LOG.warn("config.yml: invalid default for '{}': {}", entry.getKey(), exception.getMessage());
            }
        }

        return defaults;
    }

    private static <T> FlagMap withParsed(FlagMap map, Flag<T> flag, String raw) {
        return map.with(flag, flag.type().parse(raw));
    }
}
