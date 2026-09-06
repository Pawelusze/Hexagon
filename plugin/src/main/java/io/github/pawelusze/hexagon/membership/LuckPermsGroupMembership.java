package io.github.pawelusze.hexagon.membership;

import java.util.Collection;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.platform.PlayerAdapter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Resolves groups through the LuckPerms API, including inherited groups. */
final class LuckPermsGroupMembership implements GroupMembership {

    private final LuckPerms luckPerms;

    LuckPermsGroupMembership(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    @Override
    public boolean isInGroup(@NotNull Player player, @NotNull String group) {
        PlayerAdapter<Player> adapter = this.luckPerms.getPlayerAdapter(Player.class);
        User user = adapter.getUser(player);
        return user.getInheritedGroups(adapter.getQueryOptions(player)).stream()
                .map(Group::getName)
                .anyMatch(group::equals);
    }

    @Override
    public @NotNull Collection<String> knownGroups() {
        return this.luckPerms.getGroupManager().getLoadedGroups().stream()
                .map(Group::getName)
                .sorted()
                .toList();
    }

    static @NotNull GroupMembership create() {
        return new LuckPermsGroupMembership(LuckPermsProvider.get());
    }
}
