package io.github.pawelusze.hexagon.api.event;

import io.github.pawelusze.hexagon.api.region.Region;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired after a region has been created. */
public final class RegionCreatedEvent extends RegionEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Creates the event.
     *
     * @param region the created region
     */
    public RegionCreatedEvent(@NotNull Region region) {
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
