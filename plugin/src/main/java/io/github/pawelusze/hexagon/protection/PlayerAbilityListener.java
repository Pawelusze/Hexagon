package io.github.pawelusze.hexagon.protection;

import io.github.pawelusze.hexagon.api.flag.Flags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

/** Enforces {@code elytra}, {@code totem} and {@code potion-effects}. */
public final class PlayerAbilityListener implements Listener {

    private final AccessService access;

    public PlayerAbilityListener(@NotNull AccessService access) {
        this.access = access;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGlide(@NotNull EntityToggleGlideEvent event) {
        if (!event.isGliding() || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (access.denies(player, player.getLocation(), Flags.ELYTRA)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onResurrect(@NotNull EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (access.denies(player, player.getLocation(), Flags.TOTEM)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionEffect(@NotNull EntityPotionEffectEvent event) {
        PotionEffect effect = event.getNewEffect();
        if (effect == null || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (access.denies(player, player.getLocation(), Flags.POTION_EFFECTS, effect.getType())) {
            event.setCancelled(true);
        }
    }
}
