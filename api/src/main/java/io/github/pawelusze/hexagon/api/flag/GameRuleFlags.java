package io.github.pawelusze.hexagon.api.flag;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.bukkit.GameRule;
import org.jetbrains.annotations.NotNull;

/**
 * The vanilla game rules a region can override, exposed as ordinary flags so they share the region
 * storage, the priority resolution and the API with every other flag.
 *
 * <p>A rule is listed here when Hexagon can enforce it inside a cuboid through a Bukkit event.
 * Rules the server only answers for a whole world, such as {@code doDaylightCycle} or
 * {@code randomTickSpeed}, cannot be scoped to a region and are absent on purpose;
 * {@link #isWorldWide(GameRule)} tells them apart from a misspelled name.
 *
 * <p>The flag name is the rule name in kebab case, so {@code keepInventory} is stored and shown as
 * {@code keep-inventory}.
 */
public final class GameRuleFlags {

    private static final List<GameRule<Boolean>> SUPPORTED_RULES = List.of(
            GameRule.KEEP_INVENTORY,
            GameRule.SHOW_DEATH_MESSAGES,
            GameRule.ANNOUNCE_ADVANCEMENTS,
            GameRule.FALL_DAMAGE,
            GameRule.FIRE_DAMAGE,
            GameRule.DROWNING_DAMAGE,
            GameRule.FREEZE_DAMAGE,
            GameRule.NATURAL_REGENERATION,
            GameRule.MOB_GRIEFING,
            GameRule.PROJECTILES_CAN_BREAK_BLOCKS,
            GameRule.DO_FIRE_TICK,
            GameRule.DO_VINES_SPREAD,
            GameRule.DO_TILE_DROPS,
            GameRule.DO_MOB_LOOT,
            GameRule.DO_MOB_SPAWNING,
            GameRule.SPAWN_MONSTERS,
            GameRule.DO_PATROL_SPAWNING,
            GameRule.DO_TRADER_SPAWNING,
            GameRule.DO_INSOMNIA,
            GameRule.DO_WARDEN_SPAWNING,
            GameRule.SPAWNER_BLOCKS_ENABLED,
            GameRule.DISABLE_RAIDS,
            GameRule.TNT_EXPLODES,
            GameRule.TNT_EXPLOSION_DROP_DECAY,
            GameRule.MOB_EXPLOSION_DROP_DECAY,
            GameRule.BLOCK_EXPLOSION_DROP_DECAY,
            GameRule.ALLOW_ENTERING_NETHER_USING_PORTALS);

    /** Every upper case letter that is not the first one starts a new word in the flag name. */
    private static final Pattern WORD_BOUNDARY = Pattern.compile("(?<!^)(?=\\p{Upper})");

    private static final Map<String, Flag<?>> COVERED_BY_FLAG = Map.of(GameRule.PVP.getName(), Flags.PVP);

    private static final Map<String, Flag<Boolean>> FLAGS_BY_RULE_NAME = createFlags();

    private GameRuleFlags() {}

    /**
     * Returns the game rules that can be set on a region.
     *
     * @return the supported rules, in the order they are listed to players
     */
    public static @NotNull List<GameRule<Boolean>> supportedRules() {
        return SUPPORTED_RULES;
    }

    /**
     * Returns every game rule flag, for registration and for listing.
     *
     * @return the flags backing the supported rules
     */
    public static @NotNull Collection<Flag<Boolean>> all() {
        return FLAGS_BY_RULE_NAME.values();
    }

    /**
     * Returns the flag backing a game rule.
     *
     * @param rule the rule, as named by Bukkit
     * @return the flag, or empty when Hexagon cannot enforce the rule per region
     */
    public static @NotNull Optional<Flag<Boolean>> find(@NotNull GameRule<?> rule) {
        return Optional.ofNullable(FLAGS_BY_RULE_NAME.get(rule.getName()));
    }

    /**
     * Returns the flag backing a game rule that Hexagon is known to support.
     *
     * @param rule the rule
     * @return the flag
     * @throws IllegalArgumentException if the rule cannot be enforced per region
     */
    public static @NotNull Flag<Boolean> of(@NotNull GameRule<Boolean> rule) {
        Flag<Boolean> flag = FLAGS_BY_RULE_NAME.get(rule.getName());
        if (flag == null) {
            throw new IllegalArgumentException("Game rule '" + rule.getName() + "' cannot be enforced per region");
        }
        return flag;
    }

    /**
     * Returns the Hexagon flag that already covers a game rule, for rules where a flag says the
     * same thing with roles and a bypass permission on top.
     *
     * @param rule the rule
     * @return the flag to use instead, or empty when no flag covers the rule
     */
    public static @NotNull Optional<Flag<?>> coveringFlag(@NotNull GameRule<?> rule) {
        return Optional.ofNullable(COVERED_BY_FLAG.get(rule.getName()));
    }

    /**
     * Tells whether Minecraft only answers a rule for a whole world, which is why Hexagon cannot
     * scope it to a region.
     *
     * @param rule the rule
     * @return true if the rule is neither supported nor covered by a flag
     */
    public static boolean isWorldWide(@NotNull GameRule<?> rule) {
        return find(rule).isEmpty() && coveringFlag(rule).isEmpty();
    }

    /**
     * Tells whether a flag came from a game rule rather than from Hexagon's own flags.
     *
     * @param flag the flag
     * @return true if the flag mirrors a vanilla game rule
     */
    public static boolean isGameRule(@NotNull Flag<?> flag) {
        return FLAGS_BY_RULE_NAME.containsValue(flag);
    }

    /**
     * Bukkit names a rule either in camel case ({@code keepInventory}) or as a namespaced key
     * ({@code minecraft:keep_inventory}), depending on the server version; both become
     * {@code keep-inventory}.
     */
    static @NotNull String flagName(@NotNull String ruleName) {
        String withoutNamespace = ruleName.substring(ruleName.indexOf(':') + 1);
        return WORD_BOUNDARY
                .matcher(withoutNamespace)
                .replaceAll("-")
                .replace('_', '-')
                .toLowerCase(Locale.ROOT);
    }

    private static Map<String, Flag<Boolean>> createFlags() {
        Map<String, Flag<Boolean>> flags = new LinkedHashMap<>();
        for (GameRule<Boolean> rule : SUPPORTED_RULES) {
            flags.put(rule.getName(), new Flag<>(flagName(rule.getName()), FlagTypes.bool(), FlagScope.EVERYONE));
        }
        return Map.copyOf(flags);
    }
}
