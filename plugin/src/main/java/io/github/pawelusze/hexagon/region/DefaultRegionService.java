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
 */
public final class DefaultRegionService implements RegionService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRegionService.class);

    private final Map<RegionId, Region> regions = new ConcurrentHashMap<>();
    private final RegionIndex index = new RegionIndex();
    private final YamlRegionRepository repository;
    private final PluginManager events;
    private final Supplier<RegionDefaults> defaults;

    public DefaultRegionService(
            @NotNull YamlRegionRepository repository,
            @NotNull PluginManager events,
            @NotNull Supplier<RegionDefaults> defaults) {
        this.repository = repository;
        this.events = events;
        this.defaults = defaults;
    }

    /**
     * Replaces everything in memory with what is on disk.
     *
     * @return how many regions are now loaded
     */
    public int load() {
        this.regions.clear();
        this.index.clear();

        for (Region region : this.repository.loadAll()) {
            Region clash = this.regions.putIfAbsent(region.id(), region);
            if (clash != null) {
                LOG.warn("Two region files both claim the id '{}'; keeping the first one", region.id());
                continue;
            }
            this.index.add(region);
        }
        return this.regions.size();
    }

    @NotNull
    List<Region> at(@NotNull Key world, int x, int y, int z) {
        return this.index.at(world, x, y, z);
    }

    @Nullable
    Region firstCovering(@NotNull Key world, int x, int y, int z, @NotNull Predicate<Region> accepts) {
        return this.index.firstCovering(world, x, y, z, accepts);
    }

    @Override
    public @NotNull Optional<Region> find(@NotNull RegionId id) {
        return Optional.ofNullable(this.regions.get(id));
    }

    @Override
    public @NotNull Collection<Region> all() {
        return List.copyOf(this.regions.values());
    }

    @Override
    public @NotNull Collection<Region> in(@NotNull Key world) {
        return this.regions.values().stream()
                .filter(region -> region.world().equals(world))
                .toList();
    }

    @Override
    public @NotNull Region create(@NotNull RegionId id, @NotNull Key world, @NotNull Bounds bounds) {
        RegionDefaults regionDefaults = this.defaults.get();
        Region region = new Region(id, world, bounds, regionDefaults.priority(), Map.of(), regionDefaults.flags());
        if (this.regions.putIfAbsent(id, region) != null) {
            throw new IllegalArgumentException("Region '" + id + "' already exists");
        }
        this.index.add(region);
        this.repository.save(region);
        this.fire(new RegionCreatedEvent(region));
        return region;
    }

    @Override
    public void update(@NotNull Region region) {
        Region previous = this.regions.replace(region.id(), region);
        if (previous == null) {
            throw new IllegalArgumentException("Region '" + region.id() + "' does not exist");
        }
        this.index.remove(previous);
        this.index.add(region);
        this.repository.save(region);
        this.fire(new RegionUpdatedEvent(previous, region));
    }

    @Override
    public boolean delete(@NotNull RegionId id) {
        Region removed = this.regions.remove(id);
        if (removed == null) {
            return false;
        }
        this.index.remove(removed);
        this.repository.delete(id);
        this.fire(new RegionDeletedEvent(removed));
        return true;
    }

    private void fire(Event event) {
        this.events.callEvent(event);
    }
}
