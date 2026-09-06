package io.github.pawelusze.hexagon.protection;

import io.github.pawelusze.hexagon.api.flag.Flags;
import io.github.pawelusze.hexagon.configuration.MessagesConfig;
import io.github.pawelusze.hexagon.text.Messenger;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.jetbrains.annotations.NotNull;

/** Enforces {@code block-break} and {@code block-place}, buckets included. */
public final class BlockListener implements Listener {

    private final AccessService access;
    private final Messenger messenger;
    private final Supplier<MessagesConfig> messages;

    public BlockListener(
            @NotNull AccessService access, @NotNull Messenger messenger, @NotNull Supplier<MessagesConfig> messages) {
        this.access = access;
        this.messenger = messenger;
        this.messages = messages;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(@NotNull BlockBreakEvent event) {
        if (this.deniesBreak(
                event.getPlayer(),
                event.getBlock().getLocation(),
                event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(@NotNull BlockPlaceEvent event) {
        if (this.deniesPlace(
                event.getPlayer(),
                event.getBlock().getLocation(),
                event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(@NotNull PlayerBucketEmptyEvent event) {
        Material placed = liquidOf(event.getBucket());
        if (this.deniesPlace(event.getPlayer(), event.getBlock().getLocation(), placed)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(@NotNull PlayerBucketFillEvent event) {
        if (this.deniesBreak(
                event.getPlayer(),
                event.getBlock().getLocation(),
                event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    private boolean deniesBreak(Player player, Location location, Material material) {
        if (!access.denies(player, location, Flags.BLOCK_BREAK, material)) {
            return false;
        }
        this.messenger.send(player, messages.get().protection.blockBreak);
        return true;
    }

    private boolean deniesPlace(Player player, Location location, Material material) {
        if (!access.denies(player, location, Flags.BLOCK_PLACE, material)) {
            return false;
        }
        this.messenger.send(player, messages.get().protection.blockPlace);
        return true;
    }

    private static Material liquidOf(Material bucket) {
        return switch (bucket) {
            case LAVA_BUCKET -> Material.LAVA;
            case POWDER_SNOW_BUCKET -> Material.POWDER_SNOW;
            default -> Material.WATER;
        };
    }
}
