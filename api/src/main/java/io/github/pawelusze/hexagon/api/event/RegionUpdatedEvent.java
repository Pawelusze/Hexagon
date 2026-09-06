package io.github.pawelusze.hexagon.api.event;

import io.github.pawelusze.hexagon.api.region.Region;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired after a region has been replaced by a new version of itself. */
public final class RegionUpdatedEvent extends RegionEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Region previous;

    /**
     * Creates the event.
     *
     * @param previous the region before the update
     * @param region the region after the update
     */
    public RegionUpdatedEvent(@NotNull Region previous, @NotNull Region region) {
        super(region);
        this.previous = previous;
    }

    /**
     * Returns the region as it was before the update.
     *
     * @return the previous state
     */
    public @NotNull Region previous() {
        return previous;
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
