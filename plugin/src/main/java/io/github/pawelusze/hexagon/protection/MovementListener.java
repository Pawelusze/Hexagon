package io.github.pawelusze.hexagon.protection;

import io.github.pawelusze.hexagon.api.event.PlayerRegionEvent;
import io.github.pawelusze.hexagon.api.event.RegionEnterEvent;
import io.github.pawelusze.hexagon.api.event.RegionLeaveEvent;
import io.github.pawelusze.hexagon.api.flag.Flag;
import io.github.pawelusze.hexagon.api.flag.Flags;
import io.github.pawelusze.hexagon.api.flag.State;
import io.github.pawelusze.hexagon.api.region.Region;
import io.github.pawelusze.hexagon.api.region.RegionQuery;
import io.github.pawelusze.hexagon.configuration.MessagesConfig;
import io.github.pawelusze.hexagon.text.Messenger;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Enforces {@code entry} and {@code exit}, fires border-crossing events, shows greetings and
 * farewells, and grounds gliding players entering a no-elytra area.
 *
 * <p>Teleports need their own handler: {@link PlayerTeleportEvent} carries its own handler list,
 * so a plugin listening for {@link PlayerMoveEvent} never sees an ender pearl or a {@code /tp}.
 */
public final class MovementListener implements Listener {

    private final RegionQuery query;
    private final AccessService access;
    private final Messenger messenger;
    private final Supplier<MessagesConfig> messages;

    public MovementListener(
            @NotNull RegionQuery query,
            @NotNull AccessService access,
            @NotNull Messenger messenger,
            @NotNull Supplier<MessagesConfig> messages) {
        this.query = query;
        this.access = access;
        this.messenger = messenger;
        this.messages = messages;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }

        this.handleCrossing(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(@NotNull PlayerTeleportEvent event) {
        this.handleCrossing(event);
    }

    private void handleCrossing(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        List<Region> before = this.query.at(event.getFrom());
        List<Region> after = this.query.at(event.getTo());

        if (!before.equals(after) && this.crossBorders(event, player, before, after)) {
            return;
        }

        if (player.isGliding() && this.access.denies(player, event.getTo(), Flags.ELYTRA)) {
            player.setGliding(false);
        }
    }

    /** Runs the entry and exit checks for one step, and reports whether the step was stopped. */
    private boolean crossBorders(PlayerMoveEvent event, Player player, List<Region> before, List<Region> after) {
        List<Region> entered =
                after.stream().filter(region -> !before.contains(region)).toList();
        List<Region> left =
                before.stream().filter(region -> !after.contains(region)).toList();

        if (this.blocks(player, entered, Flags.ENTRY, this.messages.get().protection.entry)
                || this.blocks(player, left, Flags.EXIT, this.messages.get().protection.exit)) {
            event.setCancelled(true);
            return true;
        }

        if (fires(entered, region -> new RegionEnterEvent(player, region))
                || fires(left, region -> new RegionLeaveEvent(player, region))) {
            event.setCancelled(true);
            return true;
        }

        this.announce(player, left, Flags.FAREWELL);
        this.announce(player, entered, Flags.GREETING);
        return false;
    }

    private boolean blocks(Player player, List<Region> crossed, Flag<State> flag, String message) {
        for (Region region : crossed) {
            if (!this.access.denies(player, region, flag)) {
                continue;
            }
            this.messenger.send(
                    player, message, Placeholder.unparsed("region", region.id().value()));
            return true;
        }
        return false;
    }

    private static boolean fires(List<Region> crossed, Function<Region, PlayerRegionEvent> factory) {
        for (Region region : crossed) {
            if (!factory.apply(region).callEvent()) {
                return true;
            }
        }
        return false;
    }

    private void announce(Player player, List<Region> crossed, Flag<String> flag) {
        for (Region region : crossed) {
            region.flag(flag)
                    .ifPresent(text -> player.sendMessage(messenger.render(
                            text,
                            Placeholder.unparsed("region", region.id().value()),
                            Placeholder.unparsed("player", player.getName()))));
        }
    }
}
