package io.github.pawelusze.hexagon.region;

import io.github.pawelusze.hexagon.api.flag.Flag;
import io.github.pawelusze.hexagon.api.flag.FlagMap;
import io.github.pawelusze.hexagon.api.flag.FlagRegistry;
import io.github.pawelusze.hexagon.api.flag.FlagValueException;
import io.github.pawelusze.hexagon.api.region.BlockPoint;
import io.github.pawelusze.hexagon.api.region.Bounds;
import io.github.pawelusze.hexagon.api.region.GroupTrustee;
import io.github.pawelusze.hexagon.api.region.PlayerTrustee;
import io.github.pawelusze.hexagon.api.region.Region;
import io.github.pawelusze.hexagon.api.region.RegionId;
import io.github.pawelusze.hexagon.api.region.Role;
import io.github.pawelusze.hexagon.api.region.Trustee;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

/**
 * Maps a region to and from the node of its file.
 *
 * <p>Anything malformed becomes a {@link SerializationException}, so the repository can skip that
 * one file. The exception is a flag the server does not know: a region should not disappear
 * because a plugin that registered the flag is gone, so the flag is logged and dropped.
 */
final class RegionCodec {

    private static final Logger LOG = LoggerFactory.getLogger(RegionCodec.class);

    private static final String PLAYER_PREFIX = "player:";
    private static final String GROUP_PREFIX = "group:";

    private final FlagRegistry flags;

    RegionCodec(@NotNull FlagRegistry flags) {
        this.flags = flags;
    }

    void encode(@NotNull Region region, @NotNull ConfigurationNode node) throws SerializationException {
        node.node("id").set(region.id().value());
        node.node("world").set(region.world().asString());
        node.node("priority").set(region.priority());

        writePoint(node.node("bounds", "min"), region.bounds().min());
        writePoint(node.node("bounds", "max"), region.bounds().max());

        ConfigurationNode trustees = node.node("trustees");
        for (Map.Entry<Trustee, Role> trusted : region.trustees().entrySet()) {
            trustees.node(trusteeKey(trusted.getKey()))
                    .set(trusted.getValue().name().toLowerCase(Locale.ROOT));
        }

        ConfigurationNode flagValues = node.node("flags");
        for (Flag<?> flag : region.flags().flags()) {
            flagValues.node(flag.name()).set(region.flags().serialized(flag).orElseThrow());
        }
    }

    @NotNull
    Region decode(@NotNull ConfigurationNode node) throws SerializationException {
        RegionId id = RegionId.of(required(node, "id"));
        Key world = parseWorldKey(required(node, "world"));
        Bounds bounds = Bounds.between(readPoint(node.node("bounds", "min")), readPoint(node.node("bounds", "max")));
        int priority = node.node("priority").getInt(0);

        return new Region(
                id,
                world,
                bounds,
                priority,
                this.readTrustees(node.node("trustees")),
                this.readFlags(id, node.node("flags")));
    }

    private Map<Trustee, Role> readTrustees(ConfigurationNode node) throws SerializationException {
        Map<Trustee, Role> trustees = new HashMap<>();

        for (Map.Entry<Object, ? extends ConfigurationNode> entry :
                node.childrenMap().entrySet()) {
            String key = String.valueOf(entry.getKey());
            trustees.put(parseTrustee(key), parseRole(key, entry.getValue().getString()));
        }

        return trustees;
    }

    private FlagMap readFlags(RegionId id, ConfigurationNode node) {
        FlagMap values = FlagMap.empty();

        for (Map.Entry<Object, ? extends ConfigurationNode> entry :
                node.childrenMap().entrySet()) {
            String name = String.valueOf(entry.getKey());
            String raw = entry.getValue().getString("");

            Flag<?> flag = this.flags.find(name).orElse(null);
            if (flag == null) {
                LOG.warn("Region '{}' uses unknown flag '{}'; it will be ignored", id, name);
                continue;
            }

            try {
                values = withParsed(values, flag, raw);
            } catch (FlagValueException exception) {
                LOG.warn(
                        "Region '{}' has an invalid value '{}' for flag '{}': {}",
                        id,
                        raw,
                        name,
                        exception.getMessage());
            }
        }

        return values;
    }

    private static <T> FlagMap withParsed(FlagMap values, Flag<T> flag, String raw) {
        return values.with(flag, flag.type().parse(raw));
    }

    private static String trusteeKey(Trustee trustee) {
        return switch (trustee) {
            case PlayerTrustee(UUID playerId) -> PLAYER_PREFIX + playerId;
            case GroupTrustee(String name) -> GROUP_PREFIX + name;
        };
    }

    private static Trustee parseTrustee(String key) throws SerializationException {
        if (key.startsWith(PLAYER_PREFIX)) {
            return parsePlayerTrustee(key.substring(PLAYER_PREFIX.length()));
        }
        if (key.startsWith(GROUP_PREFIX)) {
            return Trustee.group(key.substring(GROUP_PREFIX.length()));
        }
        throw new SerializationException("Trustee '" + key + "' is neither a player nor a group");
    }

    private static Trustee parsePlayerTrustee(String playerId) throws SerializationException {
        try {
            return Trustee.player(UUID.fromString(playerId));
        } catch (IllegalArgumentException exception) {
            throw new SerializationException("Trustee '" + playerId + "' is not a player id");
        }
    }

    private static Role parseRole(String trustee, String value) throws SerializationException {
        try {
            return Role.valueOf(String.valueOf(value).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new SerializationException("Trustee '" + trustee + "' has unknown role '" + value + "'");
        }
    }

    private static void writePoint(ConfigurationNode node, BlockPoint point) throws SerializationException {
        node.node("x").set(point.x());
        node.node("y").set(point.y());
        node.node("z").set(point.z());
    }

    private static BlockPoint readPoint(ConfigurationNode node) throws SerializationException {
        if (node.virtual()) {
            throw new SerializationException("Missing bounds corner at " + node.path());
        }

        return new BlockPoint(
                node.node("x").getInt(), node.node("y").getInt(), node.node("z").getInt());
    }

    private static Key parseWorldKey(@Subst("minecraft:overworld") String value) throws SerializationException {
        try {
            return Key.key(value);
        } catch (InvalidKeyException exception) {
            throw new SerializationException("Invalid world key '" + value + "'");
        }
    }

    private static String required(ConfigurationNode node, String key) throws SerializationException {
        String value = node.node(key).getString();
        if (value == null || value.isBlank()) {
            throw new SerializationException("Missing '" + key + "'");
        }
        return value;
    }
}
