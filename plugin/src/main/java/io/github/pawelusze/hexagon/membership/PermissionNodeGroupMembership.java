package io.github.pawelusze.hexagon.membership;

import java.util.Collection;
import java.util.List;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Fallback used without LuckPerms: relies on the {@code group.<name>} permission convention that
 * most permission plugins follow. Group names cannot be enumerated.
 */
final class PermissionNodeGroupMembership implements GroupMembership {

    @Override
    public boolean isInGroup(@NotNull Player player, @NotNull String group) {
        return player.hasPermission("group." + group);
    }

    @Override
    public @NotNull Collection<String> knownGroups() {
        return List.of();
    }
}
