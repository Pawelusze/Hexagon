package io.github.pawelusze.hexagon.protection;

import io.github.pawelusze.hexagon.api.flag.Flags;
import io.github.pawelusze.hexagon.configuration.MessagesConfig;
import io.github.pawelusze.hexagon.text.Messenger;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

/** Enforces {@code pvp} and {@code damage}. */
public final class CombatListener implements Listener {

    private final AccessService access;
    private final Messenger messenger;
    private final Supplier<MessagesConfig> messages;

    public CombatListener(
            @NotNull AccessService access, @NotNull Messenger messenger, @NotNull Supplier<MessagesConfig> messages) {
        this.access = access;
        this.messenger = messenger;
        this.messages = messages;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(@NotNull EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (access.denies(victim, victim.getLocation(), Flags.DAMAGE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerVersusPlayer(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!(event.getDamageSource().getCausingEntity() instanceof Player attacker) || attacker.equals(victim)) {
            return;
        }
        boolean denied = access.denies(attacker, victim.getLocation(), Flags.PVP)
                || access.denies(attacker, attacker.getLocation(), Flags.PVP);
        if (!denied) {
            return;
        }
        event.setCancelled(true);
        this.messenger.send(attacker, messages.get().protection.pvp);
    }
}
