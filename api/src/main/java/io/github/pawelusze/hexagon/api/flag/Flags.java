package io.github.pawelusze.hexagon.api.flag;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.List;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

/** Flags shipped with Hexagon. */
public final class Flags {

    /** Whether non-members may break blocks, optionally limited to a list of materials. */
    public static final Flag<AccessRule<Material>> BLOCK_BREAK =
            new Flag<>("block-break", materialRule(), FlagScope.NON_MEMBERS);

    /** Whether non-members may place blocks, optionally limited to a list of materials. */
    public static final Flag<AccessRule<Material>> BLOCK_PLACE =
            new Flag<>("block-place", materialRule(), FlagScope.NON_MEMBERS);

    /** Whether non-members may use blocks and entities: doors, containers, item frames and such. */
    public static final Flag<State> INTERACT = Flag.state("interact", FlagScope.NON_MEMBERS);

    /** Whether outsiders may enter the region. */
    public static final Flag<State> ENTRY = Flag.state("entry", FlagScope.OUTSIDERS);

    /** Whether outsiders may leave the region. */
    public static final Flag<State> EXIT = Flag.state("exit", FlagScope.OUTSIDERS);

    /** Whether players may damage each other. */
    public static final Flag<State> PVP = Flag.state("pvp", FlagScope.EVERYONE);

    /** Whether players take any damage at all. */
    public static final Flag<State> DAMAGE = Flag.state("damage", FlagScope.EVERYONE);

    /** Whether players may glide with elytra. */
    public static final Flag<State> ELYTRA = Flag.state("elytra", FlagScope.EVERYONE);

    /** Whether a totem of undying may save a player. */
    public static final Flag<State> TOTEM = Flag.state("totem", FlagScope.EVERYONE);

    /** Whether players may gain potion effects, optionally limited to a list of effects. */
    public static final Flag<AccessRule<PotionEffectType>> POTION_EFFECTS =
            new Flag<>("potion-effects", potionEffectRule(), FlagScope.EVERYONE);

    /** Whether explosions damage blocks. */
    public static final Flag<State> EXPLOSIONS = Flag.state("explosions", FlagScope.EVERYONE);

    /** MiniMessage text shown to players entering the region. */
    public static final Flag<String> GREETING = Flag.text("greeting");

    /** MiniMessage text shown to players leaving the region. */
    public static final Flag<String> FAREWELL = Flag.text("farewell");

    private Flags() {}

    /**
     * Returns every built-in flag.
     *
     * @return the flags, in declaration order
     */
    public static @NotNull List<Flag<?>> builtIn() {
        return List.of(
                BLOCK_BREAK,
                BLOCK_PLACE,
                INTERACT,
                ENTRY,
                EXIT,
                PVP,
                DAMAGE,
                ELYTRA,
                TOTEM,
                POTION_EFFECTS,
                EXPLOSIONS,
                GREETING,
                FAREWELL);
    }

    private static FlagType<AccessRule<Material>> materialRule() {
        return FlagTypes.accessRule(
                Flags::matchMaterial, material -> material.key().value());
    }

    private static Optional<Material> matchMaterial(String name) {
        return Optional.ofNullable(Material.matchMaterial(name));
    }

    private static FlagType<AccessRule<PotionEffectType>> potionEffectRule() {
        return FlagTypes.accessRule(
                Flags::matchPotionEffect, effect -> effect.key().value());
    }

    private static Optional<PotionEffectType> matchPotionEffect(String name) {
        return Optional.ofNullable(NamespacedKey.fromString(name))
                .map(key -> RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.MOB_EFFECT)
                        .get(key));
    }
}
