package io.github.pawelusze.hexagon.api.event;

import io.github.pawelusze.hexagon.api.region.Region;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired after a region has been deleted. */
public final class RegionDeletedEvent extends RegionEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Creates the event.
     *
     * @param region the deleted region, as it was before deletion
     */
    public RegionDeletedEvent(@NotNull Region region) {
        super(region);
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
