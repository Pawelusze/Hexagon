package io.github.pawelusze.hexagon.protection;

import io.github.pawelusze.hexagon.api.flag.Flags;
import io.github.pawelusze.hexagon.configuration.MessagesConfiguration;
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

    private final AccessControl access;
    private final Messenger messenger;
    private final Supplier<MessagesConfiguration> messages;

    public CombatListener(
            @NotNull AccessControl access,
            @NotNull Messenger messenger,
            @NotNull Supplier<MessagesConfiguration> messages) {
        this.access = access;
        this.messenger = messenger;
        this.messages = messages;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(@NotNull EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (this.access.denies(victim, victim, Flags.DAMAGE)) {
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
        boolean denied =
                this.access.denies(attacker, victim, Flags.PVP) || this.access.denies(attacker, attacker, Flags.PVP);
        if (!denied) {
            return;
        }
        event.setCancelled(true);
        this.messenger.send(attacker, this.messages.get().protection.pvp);
    }
}
