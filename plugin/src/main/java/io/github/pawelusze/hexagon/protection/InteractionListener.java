package io.github.pawelusze.hexagon.protection;

import io.github.pawelusze.hexagon.api.flag.Flags;
import io.github.pawelusze.hexagon.configuration.MessagesConfig;
import io.github.pawelusze.hexagon.text.Messenger;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

/** Enforces {@code interact} on blocks and entities, and {@code block-break} on hanging entities. */
public final class InteractionListener implements Listener {

    private final AccessService access;
    private final Messenger messenger;
    private final Supplier<MessagesConfig> messages;

    public InteractionListener(
            @NotNull AccessService access, @NotNull Messenger messenger, @NotNull Supplier<MessagesConfig> messages) {
        this.access = access;
        this.messenger = messenger;
        this.messages = messages;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockInteract(@NotNull PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.PHYSICAL) {
            return;
        }
        if (!access.denies(event.getPlayer(), block.getLocation(), Flags.INTERACT)) {
            return;
        }
        event.setUseInteractedBlock(Event.Result.DENY);
        if (action == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND) {
            this.messenger.send(event.getPlayer(), messages.get().protection.interact);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(@NotNull PlayerInteractEntityEvent event) {
        if (!access.denies(event.getPlayer(), event.getRightClicked().getLocation(), Flags.INTERACT)) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND) {
            this.messenger.send(event.getPlayer(), messages.get().protection.interact);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreak(@NotNull HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player player)) {
            return;
        }
        Material material =
                switch (event.getEntity().getType()) {
                    case ITEM_FRAME -> Material.ITEM_FRAME;
                    case GLOW_ITEM_FRAME -> Material.GLOW_ITEM_FRAME;
                    case PAINTING -> Material.PAINTING;
                    default -> Material.AIR;
                };
        Location location = event.getEntity().getLocation();
        if (!access.denies(player, location, Flags.BLOCK_BREAK, material)) {
            return;
        }
        event.setCancelled(true);
        this.messenger.send(player, messages.get().protection.blockBreak);
    }
}
