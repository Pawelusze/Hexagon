package io.github.pawelusze.hexagon.selection;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import io.github.pawelusze.hexagon.api.region.BlockPoint;
import io.github.pawelusze.hexagon.api.region.Bounds;
import java.util.Optional;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The player's selection, owned by WorldEdit or FastAsyncWorldEdit. Both ship the same API, so a
 * single integration covers them. This is the only place that touches WorldEdit types.
 */
public final class WorldEditSelection {

    private WorldEditSelection() {}

    /** Returns the cuboid the player has selected in the world they stand in. */
    public static @NotNull Optional<Bounds> cuboidOf(@NotNull Player player) {
        com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(player.getWorld());
        Region selection;
        try {
            selection = session(player).getSelection(world);
        } catch (IncompleteRegionException exception) {
            return Optional.empty();
        }
        if (!(selection instanceof CuboidRegion cuboid)) {
            return Optional.empty();
        }
        return Optional.of(Bounds.between(point(cuboid.getMinimumPoint()), point(cuboid.getMaximumPoint())));
    }

    /** Replaces the player's selection, so they can inspect or reshape an existing region. */
    public static void select(@NotNull Player player, @NotNull World world, @NotNull Bounds bounds) {
        com.sk89q.worldedit.world.World target = BukkitAdapter.adapt(world);
        session(player)
                .setRegionSelector(
                        target, new CuboidRegionSelector(target, vector(bounds.min()), vector(bounds.max())));
    }

    /** The WorldEdit API version behind the selections, reported once at startup. */
    public static @NotNull String version() {
        return WorldEdit.getVersion();
    }

    private static LocalSession session(Player player) {
        return WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
    }

    private static BlockPoint point(BlockVector3 vector) {
        return new BlockPoint(vector.x(), vector.y(), vector.z());
    }

    private static BlockVector3 vector(BlockPoint point) {
        return BlockVector3.at(point.x(), point.y(), point.z());
    }
}
