package io.github.pawelusze.hexagon.protection;

import io.github.pawelusze.hexagon.api.flag.AccessRule;
import io.github.pawelusze.hexagon.api.flag.Flag;
import io.github.pawelusze.hexagon.api.flag.State;
import io.github.pawelusze.hexagon.api.region.Region;
import io.github.pawelusze.hexagon.api.region.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Decides whether a flag stops a player, honouring roles and the bypass permission.
 *
 * <p>The flag is resolved before the permission is read: most blocks a player touches lie in no
 * region at all, and an index lookup is cheaper than walking a permission tree.
 */
public final class AccessService {

    private final RegionQuery query;

    public AccessService(@NotNull RegionQuery query) {
        this.query = query;
    }

    public boolean denies(@NotNull Player player, @NotNull Location location, @NotNull Flag<State> flag) {
        return !this.query.allows(location, flag, player) && obeysFlags(player);
    }

    public <E> boolean denies(
            @NotNull Player player, @NotNull Location location, @NotNull Flag<AccessRule<E>> flag, @NotNull E element) {
        return !this.query.allows(location, flag, element, player) && obeysFlags(player);
    }

    public boolean denies(@NotNull Player player, @NotNull Region region, @NotNull Flag<State> flag) {
        State state = region.flag(flag).orElse(State.ALLOW);
        if (state.isAllowed()) {
            return false;
        }

        boolean applies = this.query
                .roleOf(player, region)
                .map(role -> !flag.scope().exempts(role))
                .orElse(true);
        return applies && obeysFlags(player);
    }

    /** A player holding the bypass permission is stopped by nothing. */
    private static boolean obeysFlags(Player player) {
        return !player.hasPermission("hexagon.bypass");
    }
}
