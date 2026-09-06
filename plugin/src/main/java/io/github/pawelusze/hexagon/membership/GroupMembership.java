package io.github.pawelusze.hexagon.membership;

import java.util.Collection;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Answers which permission groups a player belongs to. Regions use it to grant a role to a whole
 * group, as in {@code /hx trust spawn group:vip member}.
 */
public interface GroupMembership {

    /** Picks LuckPerms when the server runs it, and the permission-node fallback otherwise. */
    static @NotNull GroupMembership detect(@NotNull Server server) {
        if (server.getPluginManager().isPluginEnabled("LuckPerms")) {
            return LuckPermsGroupMembership.create();
        }
        return new PermissionNodeGroupMembership();
    }

    boolean isInGroup(@NotNull Player player, @NotNull String group);

    /** Group names the permission plugin can enumerate, for validation and tab completion. */
    @NotNull
    Collection<String> knownGroups();
}
