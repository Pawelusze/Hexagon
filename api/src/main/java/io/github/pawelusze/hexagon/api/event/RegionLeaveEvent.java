package io.github.pawelusze.hexagon.api.event;

import io.github.pawelusze.hexagon.api.region.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired when a player is about to leave a region. Cancelling keeps the player inside. */
public final class RegionLeaveEvent extends PlayerRegionEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Creates the event.
     *
     * @param player the leaving player
     * @param region the region being left
     */
    public RegionLeaveEvent(@NotNull Player player, @NotNull Region region) {
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
