package io.github.pawelusze.hexagon.api.event;

import io.github.pawelusze.hexagon.api.region.Region;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/** Base of every event concerning a single region. */
public abstract class RegionEvent extends Event {

    private final Region region;

    /**
     * Creates the event.
     *
     * @param region the region concerned
     */
    protected RegionEvent(@NotNull Region region) {
        this.region = region;
    }

    /**
     * Returns the region concerned.
     *
     * @return the region
     */
    public @NotNull Region region() {
        return region;
    }
}
