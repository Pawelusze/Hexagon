package io.github.pawelusze.hexagon;

import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.LiteCommandsBuilder;
import dev.rollczi.litecommands.adventure.LiteAdventureExtension;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolverBase;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import dev.rollczi.litecommands.bukkit.LiteBukkitMessages;
import dev.rollczi.litecommands.invalidusage.InvalidUsage;
import dev.rollczi.litecommands.message.LiteMessages;
import io.github.pawelusze.hexagon.api.HexagonApi;
import io.github.pawelusze.hexagon.api.flag.Flag;
import io.github.pawelusze.hexagon.api.flag.Flags;
import io.github.pawelusze.hexagon.api.flag.GameRuleFlags;
import io.github.pawelusze.hexagon.api.region.Region;
import io.github.pawelusze.hexagon.api.region.Trustee;
import io.github.pawelusze.hexagon.command.HexagonCommand;
import io.github.pawelusze.hexagon.command.argument.FlagArgument;
import io.github.pawelusze.hexagon.command.argument.FlagValue;
import io.github.pawelusze.hexagon.command.argument.FlagValueArgument;
import io.github.pawelusze.hexagon.command.argument.GameRuleArgument;
import io.github.pawelusze.hexagon.command.argument.RegionArgument;
import io.github.pawelusze.hexagon.command.argument.TrusteeArgument;
import io.github.pawelusze.hexagon.configuration.HexagonConfiguration;
import io.github.pawelusze.hexagon.flag.DefaultFlagRegistry;
import io.github.pawelusze.hexagon.membership.GroupMembership;
import io.github.pawelusze.hexagon.protection.AccessService;
import io.github.pawelusze.hexagon.protection.BlockListener;
import io.github.pawelusze.hexagon.protection.CombatListener;
import io.github.pawelusze.hexagon.protection.ExplosionListener;
import io.github.pawelusze.hexagon.protection.GameRuleListener;
import io.github.pawelusze.hexagon.protection.InteractionListener;
import io.github.pawelusze.hexagon.protection.MovementListener;
import io.github.pawelusze.hexagon.protection.PlayerAbilityListener;
import io.github.pawelusze.hexagon.region.DefaultRegionService;
import io.github.pawelusze.hexagon.region.RegionResolver;
import io.github.pawelusze.hexagon.region.YamlRegionRepository;
import io.github.pawelusze.hexagon.selection.WorldEditSelection;
import io.github.pawelusze.hexagon.text.Messenger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameRule;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wires Hexagon together and takes it apart again.
 *
 * <p>Regions are loaded on {@link ServerLoadEvent} rather than in {@link #onEnable()}, so a plugin
 * that registers its own flags while enabling still has them known by the time region files are
 * read.
 */
public final class HexagonPlugin extends JavaPlugin implements Listener {

    private static final Logger LOG = LoggerFactory.getLogger(HexagonPlugin.class);

    private final DefaultFlagRegistry flags = new DefaultFlagRegistry();
    private final Messenger messenger = new Messenger();

    private @Nullable HexagonConfiguration configuration;
    private @Nullable YamlRegionRepository repository;
    private @Nullable DefaultRegionService regions;
    private @Nullable LiteCommands<CommandSender> commands;

    @Override
    public void onEnable() {
        Flags.builtIn().forEach(this.flags::register);
        GameRuleFlags.all().forEach(this.flags::register);

        HexagonConfiguration configuration = new HexagonConfiguration(getDataPath(), this.flags);
        this.configuration = configuration;
        if (!this.applyConfiguration()) {
            LOG.error("Disabling: the configuration could not be read");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        GroupMembership groups = GroupMembership.detect(getServer());
        LOG.info(
                "Using WorldEdit {} for selections and {} for groups",
                WorldEditSelection.version(),
                groups.getClass().getSimpleName());

        YamlRegionRepository repository = new YamlRegionRepository(getDataPath().resolve("regions"), this.flags);
        this.repository = repository;
        DefaultRegionService regions =
                new DefaultRegionService(repository, getServer().getPluginManager(), configuration::regionDefaults);
        this.regions = regions;

        RegionResolver query = new RegionResolver(regions, groups);
        HexagonApi api = new DefaultHexagonApi(regions, query, this.flags);

        this.registerListeners(query, configuration);
        this.commands = this.registerCommands(api, groups, configuration);
        getServer().getServicesManager().register(HexagonApi.class, api, this, ServicePriority.Normal);
    }

    @EventHandler
    public void onServerLoad(@NotNull ServerLoadEvent event) {
        this.loadRegions();
    }

    @Override
    public void onDisable() {
        if (this.commands != null) {
            this.commands.unregister();
        }

        getServer().getServicesManager().unregisterAll(this);

        if (this.repository != null) {
            this.repository.close();
        }
    }

    private void registerListeners(RegionResolver query, HexagonConfiguration configuration) {
        AccessService access = new AccessService(query);
        PluginManager events = getServer().getPluginManager();

        events.registerEvents(this, this);
        events.registerEvents(new BlockListener(access, this.messenger, configuration::messages), this);
        events.registerEvents(new InteractionListener(access, this.messenger, configuration::messages), this);
        events.registerEvents(new CombatListener(access, this.messenger, configuration::messages), this);
        events.registerEvents(new MovementListener(query, access, this.messenger, configuration::messages), this);
        events.registerEvents(new PlayerAbilityListener(access), this);
        events.registerEvents(new ExplosionListener(query), this);
        events.registerEvents(new GameRuleListener(query), this);
    }

    private LiteCommands<CommandSender> registerCommands(
            HexagonApi api, GroupMembership groups, HexagonConfiguration configuration) {
        HexagonCommand command = new HexagonCommand(
                getServer(), api, this.messenger, configuration::plugin, configuration::messages, this::reload);

        LiteCommandsBuilder<CommandSender, ?, ?> builder = LiteBukkitFactory.builder("hexagon", this)
                .extension(
                        new LiteAdventureExtension<>(),
                        settings -> settings.miniMessage(true).legacyColor(true))
                .argument(Region.class, new RegionArgument(api.regions(), this.messenger, configuration::messages))
                .argument(FlagValue.class, new FlagValueArgument(api.flags()))
                .argument(
                        Trustee.class,
                        new TrusteeArgument(getServer(), groups, this.messenger, configuration::messages))
                .message(
                        LiteBukkitMessages.PLAYER_ONLY,
                        (_, _) -> this.messenger.message(configuration.messages().playerOnly))
                .message(
                        LiteMessages.MISSING_PERMISSIONS,
                        (_, _) -> this.messenger.message(configuration.messages().missingPermission))
                .message(LiteMessages.INVALID_USAGE, (_, usage) -> this.usageMessage(configuration, usage))
                .commands(command);
        registerGenericArgument(
                builder, Flag.class, new FlagArgument(api.flags(), this.messenger, configuration::messages));
        registerGenericArgument(builder, GameRule.class, new GameRuleArgument(this.messenger, configuration::messages));

        LiteCommands<CommandSender> commands = builder.build();
        commands.register();
        return commands;
    }

    /** LiteCommands hands over one usage line per shape the command accepts. */
    private Component usageMessage(HexagonConfiguration configuration, InvalidUsage<?> usage) {
        String template = configuration.messages().invalidUsage;
        Component lines = Component.empty();

        for (String schematic : usage.getSchematic().all()) {
            if (!lines.equals(Component.empty())) {
                lines = lines.append(Component.newline());
            }
            lines = lines.append(this.messenger.message(template, Placeholder.unparsed("usage", schematic)));
        }
        return lines;
    }

    /** Generic types such as {@code Flag<?>} cannot be written as a {@code Class}, hence the raw call. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerGenericArgument(
            LiteCommandsBuilder<CommandSender, ?, ?> builder,
            Class<?> type,
            ArgumentResolverBase<CommandSender, ?> resolver) {
        builder.argument((Class) type, (ArgumentResolverBase) resolver);
    }

    private void reload() {
        this.applyConfiguration();
        this.loadRegions();
    }

    private boolean applyConfiguration() {
        if (this.configuration == null || !this.configuration.reload()) {
            return false;
        }

        this.messenger.prefix(this.configuration.messages().prefix);
        return true;
    }

    private void loadRegions() {
        if (this.regions == null) {
            return;
        }

        long startedAt = System.nanoTime();
        int loaded = this.regions.load();
        LOG.info("Loaded {} region(s) in {} ms", loaded, (System.nanoTime() - startedAt) / 1_000_000L);
    }
}
