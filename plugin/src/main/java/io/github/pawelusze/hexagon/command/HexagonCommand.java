package io.github.pawelusze.hexagon.command;

import static io.github.pawelusze.hexagon.command.CommandPlaceholders.accepts;
import static io.github.pawelusze.hexagon.command.CommandPlaceholders.flag;
import static io.github.pawelusze.hexagon.command.CommandPlaceholders.flags;
import static io.github.pawelusze.hexagon.command.CommandPlaceholders.gameRule;
import static io.github.pawelusze.hexagon.command.CommandPlaceholders.input;
import static io.github.pawelusze.hexagon.command.CommandPlaceholders.page;
import static io.github.pawelusze.hexagon.command.CommandPlaceholders.pages;
import static io.github.pawelusze.hexagon.command.CommandPlaceholders.priority;
import static io.github.pawelusze.hexagon.command.CommandPlaceholders.region;
import static io.github.pawelusze.hexagon.command.CommandPlaceholders.role;
import static io.github.pawelusze.hexagon.command.CommandPlaceholders.size;
import static io.github.pawelusze.hexagon.command.CommandPlaceholders.trustee;
import static io.github.pawelusze.hexagon.command.CommandPlaceholders.value;
import static io.github.pawelusze.hexagon.command.CommandPlaceholders.world;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.context.Sender;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.optional.OptionalArg;
import dev.rollczi.litecommands.annotations.permission.Permission;
import io.github.pawelusze.hexagon.api.HexagonApi;
import io.github.pawelusze.hexagon.api.flag.Flag;
import io.github.pawelusze.hexagon.api.flag.FlagRegistry;
import io.github.pawelusze.hexagon.api.flag.FlagValueException;
import io.github.pawelusze.hexagon.api.flag.GameRuleFlags;
import io.github.pawelusze.hexagon.api.region.BlockPoint;
import io.github.pawelusze.hexagon.api.region.Bounds;
import io.github.pawelusze.hexagon.api.region.Region;
import io.github.pawelusze.hexagon.api.region.RegionId;
import io.github.pawelusze.hexagon.api.region.RegionQuery;
import io.github.pawelusze.hexagon.api.region.RegionService;
import io.github.pawelusze.hexagon.api.region.Role;
import io.github.pawelusze.hexagon.api.region.Trustee;
import io.github.pawelusze.hexagon.command.argument.FlagValue;
import io.github.pawelusze.hexagon.configuration.MessagesConfig;
import io.github.pawelusze.hexagon.configuration.PluginConfig;
import io.github.pawelusze.hexagon.selection.WorldEditSelection;
import io.github.pawelusze.hexagon.text.Messenger;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Every {@code /hexagon} subcommand, grouped by what it is for: looking regions up, listing what
 * can be set on them, the lifecycle of a region, flags, game rules, roles, and maintenance.
 *
 * <p>A command that changes a region starts with {@link #deniesManagement}, which answers whether
 * the sender may touch it and tells them when they may not.
 */
@Command(name = "hexagon", aliases = "hx")
public final class HexagonCommand {

    /** Reading a region: its bounds, its flags, and the lists of what can be set on one. */
    private static final String VIEW = "hexagon.view";

    /** Changing any region, not only the ones you own. */
    private static final String MANAGE = "hexagon.manage";

    private static final String EVERY_WORLD = "every world";

    private final Server server;
    private final RegionService regions;
    private final RegionQuery query;
    private final FlagRegistry flags;
    private final Messenger messenger;
    private final Supplier<PluginConfig> config;
    private final Supplier<MessagesConfig> messages;
    private final Runnable reload;

    public HexagonCommand(
            @NotNull Server server,
            @NotNull HexagonApi hexagon,
            @NotNull Messenger messenger,
            @NotNull Supplier<PluginConfig> config,
            @NotNull Supplier<MessagesConfig> messages,
            @NotNull Runnable reload) {
        this.server = server;
        this.regions = hexagon.regions();
        this.query = hexagon.query();
        this.flags = hexagon.flags();
        this.messenger = messenger;
        this.config = config;
        this.messages = messages;
        this.reload = reload;
    }

    // Looking at regions

    @Execute
    @Permission(VIEW)
    public void executeRoot(@Sender CommandSender sender) {
        this.executeHelp(sender);
    }

    @Execute(name = "help")
    @Permission(VIEW)
    public void executeHelp(@Sender CommandSender sender) {
        this.messages.get().help.forEach(line -> this.messenger.sendUnprefixed(sender, line));
    }

    @Execute(name = "info")
    @Permission(VIEW)
    public void executeInfo(@Sender CommandSender sender, @OptionalArg @Nullable Region region) {
        Region target = region != null ? region : this.regionAt(sender);
        if (target == null) {
            return;
        }

        MessagesConfig.RegionMessages texts = this.messages.get().region;
        Bounds bounds = target.bounds();
        this.messenger.send(sender, texts.infoHeader, region(target));
        this.messenger.send(sender, texts.infoWorld, world(target.world().asString()));
        this.messenger.send(
                sender,
                texts.infoBounds,
                Placeholder.unparsed("min", bounds.min().toString()),
                Placeholder.unparsed("max", bounds.max().toString()),
                size(bounds));
        this.messenger.send(sender, texts.infoPriority, priority(target.priority()));

        this.messenger.send(sender, texts.infoTrusteesHeader);
        if (target.trustees().isEmpty()) {
            this.messenger.send(sender, texts.infoNoTrustees);
        }
        target.trustees().entrySet().stream()
                .sorted(Map.Entry.<Trustee, Role>comparingByValue().reversed())
                .forEach(entry -> this.messenger.send(
                        sender, texts.infoTrustee, trustee(this.server, entry.getKey()), role(entry.getValue())));

        this.messenger.send(sender, texts.infoFlagsHeader);
        if (target.flags().isEmpty()) {
            this.messenger.send(sender, texts.infoNoFlags);
        }
        target.flags().flags().stream()
                .sorted(Comparator.comparing(Flag::name))
                .forEach(set -> this.messenger.send(
                        sender,
                        texts.infoFlag,
                        flag(set),
                        value(target.flags().serialized(set).orElse(""))));
    }

    @Execute(name = "list")
    @Permission(VIEW)
    public void executeList(@Sender CommandSender sender, @OptionalArg @Nullable Integer page) {
        if (sender instanceof Player player) {
            this.sendPage(
                    sender,
                    player.getWorld().getName(),
                    this.regions.in(player.getWorld().key()),
                    page);
            return;
        }

        this.sendPage(sender, EVERY_WORLD, this.regions.all(), page);
    }

    @Execute(name = "list")
    @Permission(VIEW)
    public void executeListInWorld(
            @Sender CommandSender sender, @Arg World world, @OptionalArg @Nullable Integer page) {
        this.sendPage(sender, world.getName(), this.regions.in(world.key()), page);
    }

    @Execute(name = "select")
    @Permission(VIEW)
    public void executeSelect(@Context Player player, @Arg Region region) {
        World world = this.worldOf(player, region);
        if (world == null) {
            return;
        }

        WorldEditSelection.select(player, world, region.bounds());
        this.messenger.send(player, this.messages.get().selection.loaded, region(region));
    }

    @Execute(name = "teleport", aliases = "tp")
    @Permission("hexagon.teleport")
    public void executeTeleport(@Context Player player, @Arg Region region) {
        World world = this.worldOf(player, region);
        if (world == null) {
            return;
        }

        BlockPoint center = region.bounds().center();
        Location target =
                new Location(world, center.x() + 0.5, region.bounds().max().y() + 1.0, center.z() + 0.5);
        player.teleportAsync(target)
                .thenRun(() -> this.messenger.send(player, this.messages.get().region.teleported, region(region)));
    }

    // What can be set on a region

    @Execute(name = "flags")
    @Permission(VIEW)
    public void executeFlagList(@Sender CommandSender sender) {
        this.messenger.send(sender, this.messages.get().flag.listHeader);

        for (Flag<?> registered : this.flags.all()) {
            if (GameRuleFlags.isGameRule(registered)) {
                continue;
            }
            this.messenger.send(
                    sender,
                    this.messages.get().flag.listEntry,
                    flag(registered),
                    accepts(registered),
                    Placeholder.unparsed(
                            "scope",
                            registered.scope().name().toLowerCase(Locale.ROOT).replace('_', ' ')));
        }
    }

    @Execute(name = "gamerules")
    @Permission(VIEW)
    public void executeGameRuleList(@Sender CommandSender sender) {
        this.messenger.send(sender, this.messages.get().gameRule.listHeader);

        for (GameRule<Boolean> rule : GameRuleFlags.supportedRules()) {
            this.messenger.send(sender, this.messages.get().gameRule.listEntry, gameRule(rule));
        }
    }

    // Lifecycle

    @Execute(name = "create", aliases = "define")
    @Permission(MANAGE)
    public void executeCreate(@Context Player player, @Arg("name") String id) {
        if (!RegionId.isValid(id)) {
            this.messenger.send(player, this.messages.get().region.invalidId, input(id));
            return;
        }

        RegionId regionId = RegionId.of(id);
        if (this.regions.find(regionId).isPresent()) {
            this.messenger.send(player, this.messages.get().region.alreadyExists, region(regionId));
            return;
        }

        Optional<Bounds> bounds = WorldEditSelection.cuboidOf(player);
        if (bounds.isEmpty()) {
            this.messenger.send(player, this.messages.get().selection.missing);
            return;
        }

        Region created = this.regions.create(regionId, player.getWorld().key(), bounds.get());
        this.messenger.send(
                player, this.messages.get().region.created, region(regionId), size(created.bounds()), flags(created));
    }

    @Execute(name = "delete")
    public void executeDelete(@Sender CommandSender sender, @Arg Region region) {
        if (this.deniesManagement(sender, region)) {
            return;
        }

        if (!this.regions.delete(region.id())) {
            this.messenger.send(
                    sender,
                    this.messages.get().region.notFound,
                    input(region.id().value()));
            return;
        }

        this.messenger.send(sender, this.messages.get().region.deleted, region(region));
    }

    @Execute(name = "resize", aliases = "redefine")
    public void executeResize(@Context Player player, @Arg Region region) {
        if (this.deniesManagement(player, region)) {
            return;
        }

        Optional<Bounds> bounds = WorldEditSelection.cuboidOf(player);
        if (bounds.isEmpty()) {
            this.messenger.send(player, this.messages.get().selection.missing);
            return;
        }

        Region updated = region.withBounds(bounds.get());
        this.regions.update(updated);
        this.messenger.send(player, this.messages.get().region.resized, region(region), size(updated.bounds()));
    }

    @Execute(name = "priority")
    public void executePriority(@Sender CommandSender sender, @Arg Region region, @Arg int priority) {
        if (this.deniesManagement(sender, region)) {
            return;
        }

        this.regions.update(region.withPriority(priority));
        this.messenger.send(sender, this.messages.get().region.prioritySet, region(region), priority(priority));
    }

    // Flags

    @Execute(name = "flag")
    public void executeFlagShow(@Sender CommandSender sender, @Arg Region region, @Arg Flag<?> flag) {
        if (this.deniesManagement(sender, region)) {
            return;
        }

        String current = region.flags().serialized(flag).orElse(null);
        if (current == null) {
            this.messenger.send(sender, this.messages.get().flag.unset, region(region), flag(flag), accepts(flag));
            return;
        }

        this.messenger.send(sender, this.messages.get().flag.current, region(region), flag(flag), value(current));
    }

    @Execute(name = "flag")
    public void executeFlagSet(
            @Sender CommandSender sender, @Arg Region region, @Arg Flag<?> flag, @Arg("value") FlagValue value) {
        if (this.deniesManagement(sender, region)) {
            return;
        }

        Region updated;
        try {
            updated = withParsedFlag(region, flag, value.text());
        } catch (FlagValueException exception) {
            this.messenger.send(
                    sender,
                    this.messages.get().flag.invalidValue,
                    flag(flag),
                    Placeholder.unparsed("reason", String.valueOf(exception.getMessage())));
            return;
        }

        this.regions.update(updated);
        String stored = updated.flags().serialized(flag).orElse(value.text());
        this.messenger.send(sender, this.messages.get().flag.set, region(region), flag(flag), value(stored));
    }

    @Execute(name = "unflag")
    public void executeFlagClear(@Sender CommandSender sender, @Arg Region region, @Arg Flag<?> flag) {
        if (this.deniesManagement(sender, region)) {
            return;
        }

        this.regions.update(region.withoutFlag(flag));
        this.messenger.send(sender, this.messages.get().flag.cleared, region(region), flag(flag));
    }

    // Game rules

    @Execute(name = "gamerule")
    public void executeGameRuleShow(@Sender CommandSender sender, @Arg Region region, @Arg GameRule<Boolean> rule) {
        if (this.deniesManagement(sender, region)) {
            return;
        }

        String current = region.flags().serialized(GameRuleFlags.of(rule)).orElse(null);
        if (current == null) {
            this.messenger.send(sender, this.messages.get().gameRule.unset, region(region), gameRule(rule));
            return;
        }

        this.messenger.send(
                sender, this.messages.get().gameRule.current, region(region), gameRule(rule), value(current));
    }

    @Execute(name = "gamerule")
    public void executeGameRuleSet(
            @Sender CommandSender sender,
            @Arg Region region,
            @Arg GameRule<Boolean> rule,
            @Arg("value") boolean enabled) {
        if (this.deniesManagement(sender, region)) {
            return;
        }

        this.regions.update(region.withFlag(GameRuleFlags.of(rule), enabled));
        this.messenger.send(
                sender,
                this.messages.get().gameRule.set,
                region(region),
                gameRule(rule),
                value(Boolean.toString(enabled)));
    }

    @Execute(name = "ungamerule")
    public void executeGameRuleClear(@Sender CommandSender sender, @Arg Region region, @Arg GameRule<Boolean> rule) {
        if (this.deniesManagement(sender, region)) {
            return;
        }

        this.regions.update(region.withoutFlag(GameRuleFlags.of(rule)));
        this.messenger.send(sender, this.messages.get().gameRule.cleared, region(region), gameRule(rule));
    }

    // Roles

    @Execute(name = "trust")
    public void executeTrust(
            @Sender CommandSender sender,
            @Arg Region region,
            @Arg("player|group") Trustee party,
            @OptionalArg @Nullable Role role) {
        if (this.deniesManagement(sender, region)) {
            return;
        }

        Role granted = role != null ? role : Role.MEMBER;
        this.regions.update(region.withTrustee(party, granted));
        this.messenger.send(
                sender, this.messages.get().trust.granted, region(region), trustee(this.server, party), role(granted));
    }

    @Execute(name = "untrust")
    public void executeUntrust(@Sender CommandSender sender, @Arg Region region, @Arg("player|group") Trustee party) {
        if (this.deniesManagement(sender, region)) {
            return;
        }

        if (region.roleOf(party).isEmpty()) {
            this.messenger.send(
                    sender, this.messages.get().trust.notTrusted, region(region), trustee(this.server, party));
            return;
        }

        this.regions.update(region.withoutTrustee(party));
        this.messenger.send(sender, this.messages.get().trust.revoked, region(region), trustee(this.server, party));
    }

    // Maintenance

    @Execute(name = "reload")
    @Permission("hexagon.admin")
    public void executeReload(@Sender CommandSender sender) {
        this.reload.run();
        this.messenger.send(sender, this.messages.get().reloaded);
    }

    private void sendPage(CommandSender sender, String worldName, Collection<Region> found, @Nullable Integer page) {
        MessagesConfig.RegionMessages texts = this.messages.get().region;
        if (found.isEmpty()) {
            this.messenger.send(sender, texts.listEmpty);
            return;
        }

        List<Region> sorted = found.stream()
                .sorted(Comparator.comparing(listed -> listed.id().value()))
                .toList();
        int pageSize = Math.max(1, this.config.get().listPageSize);
        int pageCount = Math.ceilDiv(sorted.size(), pageSize);
        int current = page == null ? 1 : Math.clamp(page, 1, pageCount);

        this.messenger.send(sender, texts.listHeader, world(worldName), page(current), pages(pageCount));
        int from = (current - 1) * pageSize;
        for (Region listed : sorted.subList(from, Math.min(from + pageSize, sorted.size()))) {
            this.messenger.send(
                    sender, texts.listEntry, region(listed), priority(listed.priority()), size(listed.bounds()));
        }
    }

    private @Nullable Region regionAt(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            this.messenger.send(sender, this.messages.get().playerOnly);
            return null;
        }

        Region region = this.query.topAt(player.getLocation()).orElse(null);
        if (region == null) {
            this.messenger.send(player, this.messages.get().region.noneHere);
        }
        return region;
    }

    private @Nullable World worldOf(CommandSender sender, Region region) {
        World world = this.server.getWorld(region.world());
        if (world == null) {
            this.messenger.send(
                    sender,
                    this.messages.get().region.worldNotLoaded,
                    region(region),
                    world(region.world().asString()));
        }
        return world;
    }

    /** Tells the sender why they may not touch the region, and reports that the command should stop. */
    private boolean deniesManagement(CommandSender sender, Region region) {
        if (sender.hasPermission(MANAGE) || this.owns(sender, region)) {
            return false;
        }

        this.messenger.send(sender, this.messages.get().notManageable, region(region));
        return true;
    }

    private boolean owns(CommandSender sender, Region region) {
        return sender instanceof Player player
                && this.query
                        .roleOf(player, region)
                        .filter(role -> role == Role.OWNER)
                        .isPresent();
    }

    private static <T> Region withParsedFlag(Region region, Flag<T> flag, String rawValue) {
        return region.withFlag(flag, flag.type().parse(rawValue));
    }
}
