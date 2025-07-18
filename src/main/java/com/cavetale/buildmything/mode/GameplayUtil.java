package com.cavetale.buildmything.mode;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.struct.Vec3d;
import com.cavetale.mytems.item.axis.CuboidOutline;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import static com.cavetale.buildmything.BuildMyThingPlugin.buildMyThingPlugin;

public final class GameplayUtil {
    private GameplayUtil() { }

    public static void loadArea(World world, Cuboid cuboid, Consumer<List<Chunk>> callback) {
        final List<Vec2i> chunkVectorList = cuboid.blockToChunk().enumerateHorizontally();
        final List<Chunk> chunkList = new ArrayList<>();
        for (Vec2i chunkVector : chunkVectorList) {
            world.getChunkAtAsync(chunkVector.x, chunkVector.z, (Consumer<Chunk>) chunk -> {
                    chunk.addPluginChunkTicket(buildMyThingPlugin());
                    chunkList.add(chunk);
                    if (chunkList.size() >= chunkVectorList.size()) {
                        callback.accept(chunkList);
                    }
                });
        }
    }

    public static void outlineArea(World world, Cuboid cuboid) {
        final Material a = Material.CYAN_CONCRETE;
        final Material b = Material.MAGENTA_CONCRETE;
        for (int z = cuboid.az; z <= cuboid.bz; z += 1) {
            for (int x = cuboid.ax; x <= cuboid.bx; x += 1) {
                final int y = cuboid.ay - 1;
                world.getBlockAt(x, y, z).setType((x & 1) == (z & 1) ? a : b);
            }
        }
        for (int y = cuboid.ay; y <= cuboid.by; y += 1) {
            for (int x = cuboid.ax; x <= cuboid.bx; x += 1) {
                final int z = cuboid.az - 1;
                world.getBlockAt(x, y, z).setType((x & 1) == (y & 1) ? a : b);
            }
        }
        for (int y = cuboid.ay; y <= cuboid.by; y += 1) {
            for (int z = cuboid.az; z <= cuboid.bz; z += 1) {
                final int x = cuboid.ax - 1;
                world.getBlockAt(x, y, z).setType((y & 1) == (z & 1) ? a : b);
            }
        }
        new CuboidOutline(world, cuboid).spawn();
    }

    /**
     * Teleport the player so they can nicely overlook the given build
     * area.  This corresponds with outlineArea in a way that the
     * outline does not obfuscate this viewpoint.
     */
    public static void teleportToViewingArea(Player player, World world, Cuboid area) {
        final Vec3d origin = area.getCenterExact();
        final Vec3d playerVector = origin.add(+(area.getSizeX() / 4), area.getSizeY() / 4, (area.getSizeZ() / 2) + 4);
        final Location location = playerVector.toLocation(world);
        location.setDirection(origin.subtract(playerVector).toVector());
        player.eject();
        player.leaveVehicle();
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.teleport(location);
        player.setGameMode(GameMode.CREATIVE);
        player.setFlying(true);
    }
}
