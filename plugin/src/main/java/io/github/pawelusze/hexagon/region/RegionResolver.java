package io.github.pawelusze.hexagon.region;

import io.github.pawelusze.hexagon.api.flag.Flag;
import io.github.pawelusze.hexagon.api.region.GroupTrustee;
import io.github.pawelusze.hexagon.api.region.PlayerTrustee;
import io.github.pawelusze.hexagon.api.region.Region;
import io.github.pawelusze.hexagon.api.region.RegionQuery;
import io.github.pawelusze.hexagon.api.region.Role;
import io.github.pawelusze.hexagon.api.region.Trustee;
import io.github.pawelusze.hexagon.membership.GroupMembership;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Answers spatial queries and resolves flags according to the rules documented on the API. */
public final class RegionResolver implements RegionQuery {

    private final DefaultRegionService regions;
    private final GroupMembership groups;

    public RegionResolver(@NotNull DefaultRegionService regions, @NotNull GroupMembership groups) {
        this.regions = regions;
        this.groups = groups;
    }

    @Override
    public @NotNull List<Region> at(@NotNull Key world, int x, int y, int z) {
        return this.regions.at(world, x, y, z);
    }

    @Override
    public @NotNull Optional<Role> roleOf(@NotNull Player player, @NotNull Region region) {
        Role highest = null;
        for (Map.Entry<Trustee, Role> entry : region.trustees().entrySet()) {
            if (!this.applies(entry.getKey(), player)) {
                continue;
            }
            if (highest == null || entry.getValue().atLeast(highest)) {
                highest = entry.getValue();
            }
        }
        return Optional.ofNullable(highest);
    }

    private boolean applies(Trustee trustee, Player player) {
        return switch (trustee) {
            case PlayerTrustee direct -> direct.playerId().equals(player.getUniqueId());
            case GroupTrustee group -> this.groups.isInGroup(player, group.name());
        };
    }

    @Override
    public <T> @NotNull Optional<T> resolve(@NotNull Location location, @NotNull Flag<T> flag) {
        Region defining = this.definingRegion(location, flag);
        return defining == null ? Optional.empty() : defining.flag(flag);
    }

    @Override
    public <T> @NotNull Optional<T> resolve(
            @NotNull Location location, @NotNull Flag<T> flag, @NotNull Player subject) {
        Region defining = this.definingRegion(location, flag);
        if (defining == null || this.isExempt(subject, defining, flag)) {
            return Optional.empty();
        }
        return defining.flag(flag);
    }

    /** The highest-priority region covering the location that sets the flag, or null if none does. */
    private @Nullable Region definingRegion(Location location, Flag<?> flag) {
        return this.regions.firstCovering(
                location.getWorld().key(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                region -> region.flags().contains(flag));
    }

    boolean isExempt(@NotNull Player subject, @NotNull Region region, @NotNull Flag<?> flag) {
        return this.roleOf(subject, region).map(flag.scope()::exempts).orElse(false);
    }
}
