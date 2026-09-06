package io.github.pawelusze.hexagon.protection;

import io.github.pawelusze.hexagon.api.flag.Flags;
import io.github.pawelusze.hexagon.api.region.RegionQuery;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jetbrains.annotations.NotNull;

/** Keeps explosions from damaging blocks inside a region, whatever set them off. */
public final class ExplosionListener implements Listener {

    private final RegionQuery query;

    public ExplosionListener(@NotNull RegionQuery query) {
        this.query = query;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(@NotNull EntityExplodeEvent event) {
        this.protectBlocks(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(@NotNull BlockExplodeEvent event) {
        this.protectBlocks(event.blockList());
    }

    private void protectBlocks(List<Block> blocks) {
        blocks.removeIf(block -> !this.query.allows(block.getLocation(), Flags.EXPLOSIONS));
    }
}
