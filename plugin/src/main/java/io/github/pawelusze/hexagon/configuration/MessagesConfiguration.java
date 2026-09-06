package io.github.pawelusze.hexagon.configuration;

import java.util.List;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

/**
 * Contents of {@code messages.yml}. Every entry is MiniMessage; legacy {@code &} codes are also
 * understood. Placeholders are MiniMessage tags such as {@code <region>}.
 *
 * <p>The shipped messages use one palette: {@code <gray>} for the sentence, {@code <white>} for a
 * value the player typed or can type, and {@code <#b80c00>} for the region a message is about.
 *
 * <p>Configurate assigns the fields when the file is read, and the plugin reads them directly.
 * Accessors would only repeat the field names, so there are none.
 */
@ConfigSerializable
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"}) // Configurate assigns the fields by reflection.
public final class MessagesConfiguration {

    public static final String HEADER = """
            Hexagon messages. MiniMessage is the primary format; legacy '&' colour codes also work.
            Placeholders such as <region> or <player> are MiniMessage tags.
            An empty string silences a message. The prefix is prepended to every message.""";

    public String prefix = "<dark_gray>[<#b80c00><bold>Hexagon</bold></#b80c00><dark_gray>]</dark_gray> ";

    public String playerOnly = "<gray>Only a player can run this one, it needs a spot in the world.";
    public String missingPermission = "<gray>You don't have permission to do that.";
    public String invalidUsage = "<gray>Type it like this: <white><usage>";
    public String notManageable = "<gray>You can't manage <#b80c00><region></#b80c00>. Ask an owner to trust you.";
    public String reloaded = "<gray>Config and messages reloaded.";

    /** Shown by /hx and /hx help, one message per line, so a server can rewrite the whole thing. */
    public List<String> help = List.of(
            "<dark_gray><st>        </st> <#b80c00><bold>Hexagon</bold></#b80c00> <dark_gray><st>        </st>",
            "<gray>A region protects the box you mark with WorldEdit. Grab the wand with <white>//wand</white>, "
                    + "left click one corner, right click the other.",
            "<white>/hx create <name></white> <dark_gray>turn your selection into a region",
            "<white>/hx resize <region></white> <dark_gray>move a region onto your current selection",
            "<white>/hx delete <region></white> <dark_gray>remove a region for good",
            "<white>/hx info [region]</white> <dark_gray>size, roles and flags of a region",
            "<white>/hx list [world] [page]</white> <dark_gray>all regions, a page at a time",
            "<white>/hx select <region></white> <dark_gray>load a region back into your WorldEdit selection",
            "<white>/hx teleport <region></white> <dark_gray>stand on top of a region",
            "<white>/hx trust <region> <player> [role]</white> "
                    + "<dark_gray>a guest may enter, a member may build, an owner may run these commands",
            "<white>/hx untrust <region> <player></white> <dark_gray>take that role away again",
            "<white>/hx flag <region> <flag> <value></white> <dark_gray>set a rule, such as <white>pvp deny</white>",
            "<white>/hx unflag <region> <flag></white> <dark_gray>drop a flag, the world decides again",
            "<white>/hx flags</white> <dark_gray>every flag you can set, and what it accepts",
            "<white>/hx gamerule <region> <rule> <true|false></white> "
                    + "<dark_gray>a vanilla game rule, but only inside the region",
            "<white>/hx gamerules</white> <dark_gray>the game rules that work per region",
            "<white>/hx priority <region> <number></white> "
                    + "<dark_gray>where regions overlap the higher number wins, new ones start at 0",
            "<white>/hx reload</white> <dark_gray>read config.yml and messages.yml again");

    public SelectionMessages selection = new SelectionMessages();
    public RegionMessages region = new RegionMessages();
    public FlagMessages flag = new FlagMessages();
    public GameRuleMessages gameRule = new GameRuleMessages();
    public TrustMessages trust = new TrustMessages();
    public ProtectionMessages protection = new ProtectionMessages();

    /** Messages about the WorldEdit selection a region is built from. */
    @ConfigSerializable
    public static final class SelectionMessages {
        public String missing = "<gray>Mark an area first: <white>//wand</white>, then left click one corner "
                + "and right click the other.";
        public String loaded = "<gray>Your WorldEdit selection now matches <#b80c00><region></#b80c00>.";
    }

    /** Messages about region lifecycle, listing and info. */
    @ConfigSerializable
    public static final class RegionMessages {
        public String invalidId =
                "<gray><white><input></white> won't do as a name. Stick to letters, digits, '-' and '_'.";
        public String notFound = "<gray>There's no region called <white><input></white>.";
        public String alreadyExists = "<gray>A region called <#b80c00><region></#b80c00> is already there.";
        public String noneHere = "<gray>You're not standing in a region.";
        public String worldNotLoaded =
                "<gray>The world <white><world></white> that holds <#b80c00><region></#b80c00> isn't loaded.";
        public String created = "<gray>Created <#b80c00><region></#b80c00>, <white><size></white> blocks, "
                + "starting with <white><flags></white>.";
        public String deleted = "<gray>Deleted <#b80c00><region></#b80c00>. Nothing protects that area now.";
        public String resized = "<gray><#b80c00><region></#b80c00> now covers <white><size></white> blocks.";
        public String prioritySet = "<gray><#b80c00><region></#b80c00> now has priority <white><priority></white>. "
                + "Where regions overlap, the higher number wins.";
        public String teleported = "<gray>Here you go: <#b80c00><region></#b80c00>.";
        public String listHeader =
                "<gray>Regions in <white><world></white>, page <white><page></white> of <white><pages></white>:";
        public String listEntry = "<dark_gray>- <click:run_command:'/hx info <region>'>"
                + "<hover:show_text:'Click to see the details'><#b80c00><region></#b80c00></hover></click> "
                + "<dark_gray>priority <priority>, <size> blocks";
        public String listEmpty = "<gray>Nothing here yet.";
        public String infoHeader = "<dark_gray><st>      </st> <#b80c00><region></#b80c00> <dark_gray><st>      </st>";
        public String infoWorld = "<gray>World: <white><world>";
        public String infoBounds = "<gray>Corners: <white><min></white> to <white><max></white>, <size> blocks";
        public String infoPriority = "<gray>Priority: <white><priority>";
        public String infoTrusteesHeader = "<gray>Trusted here:";
        public String infoTrustee = "<dark_gray>- <white><trustee></white> <dark_gray>as <role>";
        public String infoFlagsHeader = "<gray>Flags:";
        public String infoFlag = "<dark_gray>- <white><flag></white> <dark_gray>is <gray><value>";
        public String infoNoTrustees = "<dark_gray>- nobody yet";
        public String infoNoFlags = "<dark_gray>- none, the world decides everything here";
    }

    /** Messages about flags. */
    @ConfigSerializable
    public static final class FlagMessages {
        public String unknown = "<gray>There's no flag called <white><input></white>. "
                + "Run <white>/hx flags</white> to see them all.";
        public String invalidValue = "<gray><white><flag></white> can't take that value: <reason>";
        public String set =
                "<gray><white><flag></white> in <#b80c00><region></#b80c00> is now " + "<white><value></white>.";
        public String cleared =
                "<gray><white><flag></white> in <#b80c00><region></#b80c00> is gone, " + "the world decides again.";
        public String current = "<gray><white><flag></white> in <#b80c00><region></#b80c00> is <white><value></white>.";
        public String unset = "<gray><white><flag></white> isn't set in <#b80c00><region></#b80c00>. "
                + "<dark_gray>It takes <accepts>.";
        public String listHeader = "<gray>Flags you can set with <white>/hx flag <region> <flag> <value></white>:";
        public String listEntry = "<dark_gray>- <white><flag></white> <dark_gray>takes <accepts>, applies to <scope>";
    }

    /** Messages about per-region game rules. */
    @ConfigSerializable
    public static final class GameRuleMessages {
        public String unknown = "<gray><white><input></white> isn't a game rule.";
        public String coveredByFlag = "<gray>Use <white>/hx flag <region> <flag></white> instead. "
                + "The flag does the same and still honours roles and the bypass permission.";
        public String worldWide = "<gray>Minecraft applies <white><rule></white> to a whole world, "
                + "so Hexagon can't keep it inside a region.";
        public String set =
                "<gray><white><rule></white> is now <white><value></white> " + "inside <#b80c00><region></#b80c00>.";
        public String cleared = "<gray><#b80c00><region></#b80c00> follows the world's <white><rule></white> again.";
        public String current =
                "<gray><white><rule></white> is <white><value></white> " + "inside <#b80c00><region></#b80c00>.";
        public String unset = "<gray><#b80c00><region></#b80c00> follows the world's <white><rule></white>.";
        public String listHeader = "<gray>Game rules you can set per region:";
        public String listEntry = "<dark_gray>- <white><rule>";
    }

    /** Messages about trustees and roles. */
    @ConfigSerializable
    public static final class TrustMessages {
        public String unknownPlayer = "<gray>No player called <white><input></white>. "
                + "For a permission group, type <white>group:<name></white>.";
        public String unknownGroup = "<gray>There's no group called <white><input></white>.";
        public String granted =
                "<gray><white><trustee></white> is now <white><role></white> in <#b80c00><region></#b80c00>.";
        public String revoked = "<gray><white><trustee></white> isn't trusted in <#b80c00><region></#b80c00> anymore.";
        public String notTrusted = "<gray><white><trustee></white> was never trusted in <#b80c00><region></#b80c00>.";
    }

    /** Messages shown to players stopped by a flag. */
    @ConfigSerializable
    public static final class ProtectionMessages {
        public String blockBreak = "<gray>You can't break blocks here.";
        public String blockPlace = "<gray>You can't place blocks here.";
        public String interact = "<gray>You can't use that here.";
        public String pvp = "<gray>PvP is off here.";
        public String entry = "<gray><#b80c00><region></#b80c00> is closed to you.";
        public String exit = "<gray>You can't leave <#b80c00><region></#b80c00> yet.";
    }
}
