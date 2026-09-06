package io.github.pawelusze.hexagon.api.event;

import io.github.pawelusze.hexagon.api.region.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired when a player is about to enter a region. Cancelling keeps the player outside. */
public final class RegionEnterEvent extends PlayerRegionEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Creates the event.
     *
     * @param player the entering player
     * @param region the region being entered
     */
    public RegionEnterEvent(@NotNull Player player, @NotNull Region region) {
        super(player, region);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Returns the handler list, as required by Bukkit.
     *
     * @return the handler list
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
