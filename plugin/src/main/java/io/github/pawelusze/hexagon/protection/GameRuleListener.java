package io.github.pawelusze.hexagon.protection;

import io.github.pawelusze.hexagon.api.flag.GameRuleFlags;
import io.github.pawelusze.hexagon.api.region.RegionQuery;
import java.util.Map;
import java.util.Optional;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Applies the game rules a region overrides. Every handler mirrors what the vanilla rule does for a
 * whole world, limited to what happens inside the region.
 *
 * <p>A rule that no region sets stays out of the way: {@link #valueOf} then answers empty and the
 * world keeps deciding.
 */
public final class GameRuleListener implements Listener {

    /** Vanilla scopes fireDamage to its fire damage tag, so several damage types share one rule. */
    private static final Map<DamageType, GameRule<Boolean>> RULES_BY_DAMAGE_TYPE = Map.of(
            DamageType.FALL, GameRule.FALL_DAMAGE,
            DamageType.IN_FIRE, GameRule.FIRE_DAMAGE,
            DamageType.ON_FIRE, GameRule.FIRE_DAMAGE,
            DamageType.LAVA, GameRule.FIRE_DAMAGE,
            DamageType.HOT_FLOOR, GameRule.FIRE_DAMAGE,
            DamageType.CAMPFIRE, GameRule.FIRE_DAMAGE,
            DamageType.DROWN, GameRule.DROWNING_DAMAGE,
            DamageType.FREEZE, GameRule.FREEZE_DAMAGE);

    /** Rules that look at what is spawning rather than at how the spawn was triggered. */
    private static final Map<EntityType, GameRule<Boolean>> RULES_BY_SPAWNED_ENTITY = Map.of(
            EntityType.PHANTOM, GameRule.DO_INSOMNIA,
            EntityType.WARDEN, GameRule.DO_WARDEN_SPAWNING,
            EntityType.WANDERING_TRADER, GameRule.DO_TRADER_SPAWNING,
            EntityType.TRADER_LLAMA, GameRule.DO_TRADER_SPAWNING);

    /** A yield of one means the explosion destroys blocks without eating their drops. */
    private static final float EVERY_BLOCK_DROPS = 1.0f;

    private final RegionQuery query;

    public GameRuleListener(@NotNull RegionQuery query) {
        this.query = query;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        Player dead = event.getEntity();

        this.valueOf(dead, GameRule.KEEP_INVENTORY).ifPresent(keep -> keepBelongings(event, keep));

        if (this.isDisabled(dead, GameRule.SHOW_DEATH_MESSAGES)) {
            event.deathMessage(null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAdvancement(@NotNull PlayerAdvancementDoneEvent event) {
        if (this.isDisabled(event.getPlayer(), GameRule.ANNOUNCE_ADVANCEMENTS)) {
            event.message(null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(@NotNull EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        GameRule<Boolean> rule =
                RULES_BY_DAMAGE_TYPE.get(event.getDamageSource().getDamageType());
        if (rule != null) {
            this.cancelIfDisabled(event, player, rule);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegainHealth(@NotNull EntityRegainHealthEvent event) {
        RegainReason reason = event.getRegainReason();
        if (reason != RegainReason.REGEN && reason != RegainReason.SATIATED) {
            return;
        }

        this.cancelIfDisabled(event, event.getEntity(), GameRule.NATURAL_REGENERATION);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(@NotNull EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }

        GameRule<Boolean> rule =
                event.getEntity() instanceof Projectile ? GameRule.PROJECTILES_CAN_BREAK_BLOCKS : GameRule.MOB_GRIEFING;
        this.cancelIfDisabled(event, event.getBlock(), rule);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(@NotNull BlockBurnEvent event) {
        this.cancelIfDisabled(event, event.getBlock(), GameRule.DO_FIRE_TICK);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockSpread(@NotNull BlockSpreadEvent event) {
        Material source = event.getSource().getType();

        if (source == Material.FIRE) {
            this.cancelIfDisabled(event, event.getBlock(), GameRule.DO_FIRE_TICK);
            return;
        }

        if (Tag.CLIMBABLE.isTagged(source)) {
            this.cancelIfDisabled(event, event.getBlock(), GameRule.DO_VINES_SPREAD);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        if (this.isDisabled(event.getBlock(), GameRule.DO_TILE_DROPS)) {
            event.setDropItems(false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(@NotNull EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }

        if (this.isDisabled(event.getEntity(), GameRule.DO_MOB_LOOT)) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(@NotNull CreatureSpawnEvent event) {
        Location spawn = event.getLocation();

        GameRule<Boolean> byEntity = RULES_BY_SPAWNED_ENTITY.get(event.getEntityType());
        if (byEntity != null && this.isDisabled(spawn, byEntity)) {
            event.setCancelled(true);
            return;
        }

        switch (event.getSpawnReason()) {
            case SPAWNER -> this.cancelIfDisabled(event, spawn, GameRule.SPAWNER_BLOCKS_ENABLED);
            case PATROL -> this.cancelIfDisabled(event, spawn, GameRule.DO_PATROL_SPAWNING);
            case NATURAL -> this.cancelNaturalSpawn(event, spawn);
            default -> {
                // Anything a player asked for, such as a spawn egg, is not a game rule's business.
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRaidTrigger(@NotNull RaidTriggerEvent event) {
        if (this.raidsAreBlocked(event.getRaid().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplosionPrime(@NotNull ExplosionPrimeEvent event) {
        if (event.getEntity() instanceof TNTPrimed) {
            this.cancelIfDisabled(event, event.getEntity(), GameRule.TNT_EXPLODES);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(@NotNull EntityExplodeEvent event) {
        GameRule<Boolean> rule = event.getEntity() instanceof TNTPrimed
                ? GameRule.TNT_EXPLOSION_DROP_DECAY
                : GameRule.MOB_EXPLOSION_DROP_DECAY;

        if (this.isDisabled(event.getLocation(), rule)) {
            event.setYield(EVERY_BLOCK_DROPS);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(@NotNull BlockExplodeEvent event) {
        if (this.isDisabled(event.getBlock(), GameRule.BLOCK_EXPLOSION_DROP_DECAY)) {
            event.setYield(EVERY_BLOCK_DROPS);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(@NotNull PlayerPortalEvent event) {
        if (event.getCause() == TeleportCause.NETHER_PORTAL) {
            this.cancelIfDisabled(event, event.getFrom(), GameRule.ALLOW_ENTERING_NETHER_USING_PORTALS);
        }
    }

    private void cancelNaturalSpawn(CreatureSpawnEvent event, Location spawn) {
        if (this.isDisabled(spawn, GameRule.DO_MOB_SPAWNING)) {
            event.setCancelled(true);
            return;
        }

        if (event.getEntity() instanceof Monster) {
            this.cancelIfDisabled(event, spawn, GameRule.SPAWN_MONSTERS);
        }
    }

    private static void keepBelongings(PlayerDeathEvent event, boolean keep) {
        event.setKeepInventory(keep);
        event.setKeepLevel(keep);

        if (keep) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    private void cancelIfDisabled(Cancellable event, Block block, GameRule<Boolean> rule) {
        if (this.isDisabled(block, rule)) {
            event.setCancelled(true);
        }
    }

    private void cancelIfDisabled(Cancellable event, Entity entity, GameRule<Boolean> rule) {
        if (this.isDisabled(entity, rule)) {
            event.setCancelled(true);
        }
    }

    private void cancelIfDisabled(Cancellable event, Location location, GameRule<Boolean> rule) {
        if (this.isDisabled(location, rule)) {
            event.setCancelled(true);
        }
    }

    private boolean isDisabled(Block block, GameRule<Boolean> rule) {
        return isOff(this.valueOf(block, rule));
    }

    private boolean isDisabled(Entity entity, GameRule<Boolean> rule) {
        return isOff(this.valueOf(entity, rule));
    }

    private boolean isDisabled(Location location, GameRule<Boolean> rule) {
        return isOff(this.query.resolve(location, GameRuleFlags.of(rule)));
    }

    private static boolean isOff(Optional<Boolean> value) {
        return value.isPresent() && !value.get();
    }

    /** disableRaids is the one rule where true means "stop", so it reads the other way round. */
    private boolean raidsAreBlocked(Location location) {
        return this.query
                .resolve(location, GameRuleFlags.of(GameRule.DISABLE_RAIDS))
                .orElse(false);
    }

    /** The value the region gives a rule at a block, or empty when no region there sets it. */
    private Optional<Boolean> valueOf(Block block, GameRule<Boolean> rule) {
        return this.query.resolve(block.getWorld(), block.getX(), block.getY(), block.getZ(), GameRuleFlags.of(rule));
    }

    /** The same, at the block an entity stands in; entities carry their coordinates, no Location is built. */
    private Optional<Boolean> valueOf(Entity entity, GameRule<Boolean> rule) {
        return this.query.resolve(
                entity.getWorld(),
                Location.locToBlock(entity.getX()),
                Location.locToBlock(entity.getY()),
                Location.locToBlock(entity.getZ()),
                GameRuleFlags.of(rule));
    }
}
