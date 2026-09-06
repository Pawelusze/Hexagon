package io.github.pawelusze.hexagon.region;

import io.github.pawelusze.hexagon.api.event.RegionCreatedEvent;
import io.github.pawelusze.hexagon.api.event.RegionDeletedEvent;
import io.github.pawelusze.hexagon.api.event.RegionUpdatedEvent;
import io.github.pawelusze.hexagon.api.region.Bounds;
import io.github.pawelusze.hexagon.api.region.Region;
import io.github.pawelusze.hexagon.api.region.RegionId;
import io.github.pawelusze.hexagon.api.region.RegionService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.kyori.adventure.key.Key;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The regions the server is running with: the map every lookup goes through, the spatial index
 * behind it, and the files they are written to.
 *
 * <p>A reload builds a whole new {@link Snapshot} on a background thread and publishes it with one
 * reference write, so the server never queries a half-filled index and never waits for a disk.
 */
public final class DefaultRegionService implements RegionService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRegionService.class);

    private final YamlRegionRepository repository;
    private final PluginManager events;
    private final Supplier<RegionDefaults> defaults;

    private volatile Snapshot state = new Snapshot(new ConcurrentHashMap<>(), new RegionIndex());

    public DefaultRegionService(
            @NotNull YamlRegionRepository repository,
            @NotNull PluginManager events,
            @NotNull Supplier<RegionDefaults> defaults) {
        this.repository = repository;
        this.events = events;
        this.defaults = defaults;
    }

    /**
     * Reads the region files and indexes them, without touching what the server is running with.
     * This is the slow part of a reload, and the part that is safe to run off the main thread.
     *
     * @return the regions on disk, ready to be published
     */
    public @NotNull Snapshot read() {
        Snapshot snapshot = new Snapshot(new ConcurrentHashMap<>(), new RegionIndex());
        for (Region region : this.repository.loadAll()) {
            if (snapshot.regions.putIfAbsent(region.id(), region) != null) {
                LOG.warn("Two region files both claim the id '{}'; keeping the first one", region.id());
                continue;
            }
            snapshot.index.add(region);
        }
        return snapshot;
    }

    /**
     * Publishes a snapshot as the regions the server protects from now on. A region created while
     * the files were being read is on disk but not in the snapshot, so it returns with the next
     * reload; the window is the parse itself, and only a command can open it.
     *
     * @param snapshot the snapshot, as returned by {@link #read()}
     * @return how many regions are now loaded
     */
    public int install(@NotNull Snapshot snapshot) {
        this.state = snapshot;
        return snapshot.regions.size();
    }

    /**
     * Reads the files and publishes them on the calling thread, for the one moment where that is
     * the right thing to do: startup, before a player can stand in an unprotected region.
     *
     * @return how many regions are now loaded
     */
    public int load() {
        return this.install(this.read());
    }

    @NotNull
    List<Region> at(@NotNull Key world, int x, int y, int z) {
        return this.state.index.at(world, x, y, z);
    }

    @Nullable
    Region firstCovering(@NotNull Key world, int x, int y, int z, @NotNull Predicate<Region> accepts) {
        return this.state.index.firstCovering(world, x, y, z, accepts);
    }

    @Override
    public @NotNull Optional<Region> find(@NotNull RegionId id) {
        return Optional.ofNullable(this.state.regions.get(id));
    }

    @Override
    public @NotNull Collection<Region> all() {
        return List.copyOf(this.state.regions.values());
    }

    @Override
    public @NotNull Collection<Region> in(@NotNull Key world) {
        return this.state.regions.values().stream()
                .filter(region -> region.world().equals(world))
                .toList();
    }

    @Override
    public @NotNull Region create(@NotNull RegionId id, @NotNull Key world, @NotNull Bounds bounds) {
        Snapshot current = this.state;
        RegionDefaults regionDefaults = this.defaults.get();
        Region region = new Region(id, world, bounds, regionDefaults.priority(), Map.of(), regionDefaults.flags());
        if (current.regions.putIfAbsent(id, region) != null) {
            throw new IllegalArgumentException("Region '" + id + "' already exists");
        }
        current.index.add(region);
        this.repository.save(region);
        this.fire(new RegionCreatedEvent(region));
        return region;
    }

    @Override
    public void update(@NotNull Region region) {
        Snapshot current = this.state;
        Region previous = current.regions.replace(region.id(), region);
        if (previous == null) {
            throw new IllegalArgumentException("Region '" + region.id() + "' does not exist");
        }
        current.index.remove(previous);
        current.index.add(region);
        this.repository.save(region);
        this.fire(new RegionUpdatedEvent(previous, region));
    }

    @Override
    public boolean delete(@NotNull RegionId id) {
        Snapshot current = this.state;
        Region removed = current.regions.remove(id);
        if (removed == null) {
            return false;
        }
        current.index.remove(removed);
        this.repository.delete(id);
        this.fire(new RegionDeletedEvent(removed));
        return true;
    }

    private void fire(Event event) {
        this.events.callEvent(event);
    }

    /**
     * One complete set of regions with its index. Built by {@link #read()}, published by
     * {@link #install(Snapshot)}; there is nothing to see from the outside.
     */
    public static final class Snapshot {

        private final Map<RegionId, Region> regions;
        private final RegionIndex index;

        private Snapshot(Map<RegionId, Region> regions, RegionIndex index) {
            this.regions = regions;
            this.index = index;
        }
    }
}
