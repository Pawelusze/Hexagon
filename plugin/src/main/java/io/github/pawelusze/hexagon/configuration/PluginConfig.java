package io.github.pawelusze.hexagon.configuration;

import java.util.LinkedHashMap;
import java.util.Map;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

/**
 * Contents of {@code config.yml}. Configurate assigns the fields when the file is read, and the
 * plugin reads them directly.
 */
@ConfigSerializable
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"}) // Configurate assigns the fields by reflection.
public final class PluginConfig {

    public static final String HEADER = """
            Hexagon configuration.

            default-priority: priority a new region starts with; change it later with /hx priority.
            default-flags: flags applied to every newly created region, using the /hx flag value syntax.
            list-page-size: regions shown per page by /hx list.""";

    public int defaultPriority = 0;
    public Map<String, String> defaultFlags = builtInDefaultFlags();
    public int listPageSize = 10;

    private static Map<String, String> builtInDefaultFlags() {
        Map<String, String> flags = new LinkedHashMap<>();
        flags.put("block-break", "deny");
        flags.put("block-place", "deny");
        flags.put("interact", "deny");
        return flags;
    }
}
