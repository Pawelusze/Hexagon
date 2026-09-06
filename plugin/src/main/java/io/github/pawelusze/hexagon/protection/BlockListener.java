package io.github.pawelusze.hexagon.protection;

import io.github.pawelusze.hexagon.api.flag.Flags;
import io.github.pawelusze.hexagon.configuration.MessagesConfiguration;
import io.github.pawelusze.hexagon.text.Messenger;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

    private final AccessControl access;
    private final Messenger messenger;
    private final Supplier<MessagesConfiguration> messages;

    public BlockListener(
            @NotNull AccessControl access,
            @NotNull Messenger messenger,
            @NotNull Supplier<MessagesConfiguration> messages) {
        this.access = access;
        this.messenger = messenger;
        this.messages = messages;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(@NotNull BlockBreakEvent event) {
        if (this.deniesBreak(
                event.getPlayer(), event.getBlock(), event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(@NotNull BlockPlaceEvent event) {
        if (this.deniesPlace(
                event.getPlayer(), event.getBlock(), event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(@NotNull PlayerBucketEmptyEvent event) {
        if (this.deniesPlace(event.getPlayer(), event.getBlock(), liquidOf(event.getBucket()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(@NotNull PlayerBucketFillEvent event) {
        if (this.deniesBreak(
                event.getPlayer(), event.getBlock(), event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    private boolean deniesBreak(Player player, Block block, Material material) {
        if (!this.access.denies(player, block, Flags.BLOCK_BREAK, material)) {
            return false;
        }
        this.messenger.send(player, this.messages.get().protection.blockBreak);
        return true;
    }

    private boolean deniesPlace(Player player, Block block, Material material) {
        if (!this.access.denies(player, block, Flags.BLOCK_PLACE, material)) {
            return false;
        }
        this.messenger.send(player, this.messages.get().protection.blockPlace);
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
