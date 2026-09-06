package io.github.pawelusze.hexagon.api.event;

import io.github.pawelusze.hexagon.api.region.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/** Base of events fired when a player crosses a region border. Cancelling blocks the movement. */
public abstract class PlayerRegionEvent extends RegionEvent implements Cancellable {

    private final Player player;
    private boolean cancelled;

    /**
     * Creates the event.
     *
     * @param player the moving player
     * @param region the region whose border is crossed
     */
    protected PlayerRegionEvent(@NotNull Player player, @NotNull Region region) {
        super(region);
        this.player = player;
    }

    /**
     * Returns the moving player.
     *
     * @return the player
     */
    public @NotNull Player player() {
        return player;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
