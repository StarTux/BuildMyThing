package com.cavetale.buildmything;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.struct.Vec3d;
import com.cavetale.mytems.item.axis.CuboidOutline;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import static com.cavetale.buildmything.BuildMyThingPlugin.buildMyThingPlugin;

@Data
@RequiredArgsConstructor
public final class BuildArea {
    private final GameRegion region; // game, world
    private final Cuboid area;
    private final List<Block> frameBlocks = new ArrayList<>();
    private CuboidOutline outline;
    private GamePlayer owningPlayer;
    private boolean chunksLoaded;
    private Map<UUID, Integer> ratings = new HashMap<>();
    private double finalRating;

    public Game getGame() {
        return region.getGame();
    }

    public World getWorld() {
        return region.getGame().getWorld();
    }

    public void loadChunks(Consumer<List<Chunk>> callback) {
        final List<Vec2i> chunkVectorList = area.blockToChunk().enumerateHorizontally();
        final List<Chunk> chunkList = new ArrayList<>();
        for (Vec2i chunkVector : chunkVectorList) {
            chunksLoaded = true;
            getWorld().getChunkAtAsync(chunkVector.x, chunkVector.z, (Consumer<Chunk>) chunk -> {
                    chunk.addPluginChunkTicket(buildMyThingPlugin());
                    chunkList.add(chunk);
                    if (chunkList.size() >= chunkVectorList.size()) {
                        callback.accept(chunkList);
                    }
                });
        }
    }

    public void placeFrame() {
        final World world = getWorld();
        final Material a = Material.CYAN_CONCRETE;
        final Material b = Material.MAGENTA_CONCRETE;
        for (int z = area.az; z <= area.bz; z += 1) {
            for (int x = area.ax; x <= area.bx; x += 1) {
                final int y = area.ay - 1;
                final Block block = world.getBlockAt(x, y, z);
                block.setType((x & 1) == (z & 1) ? a : b, false);
                frameBlocks.add(block);
            }
        }
        for (int y = area.ay; y <= area.by; y += 1) {
            for (int x = area.ax; x <= area.bx; x += 1) {
                final int z = area.az - 1;
                final Block block = world.getBlockAt(x, y, z);
                block.setType((x & 1) == (y & 1) ? a : b, false);
                frameBlocks.add(block);
            }
        }
        for (int y = area.ay; y <= area.by; y += 1) {
            for (int z = area.az; z <= area.bz; z += 1) {
                final int x = area.ax - 1;
                final Block block = world.getBlockAt(x, y, z);
                block.setType((y & 1) == (z & 1) ? a : b, false);
                frameBlocks.add(block);
            }
        }
    }

    public void removeFrame() {
        for (Block block : frameBlocks) {
            block.setType(Material.AIR, false);
        }
        frameBlocks.clear();
    }

    public void createOutline() {
        outline = new CuboidOutline(getWorld(), area);
        outline.spawn();
    }

    public void removeOutline() {
        if (outline == null) return;
        outline.remove();
        outline = null;
    }

    /**
     * Teleport the player so they can nicely overlook the given build
     * area.  This corresponds with outlineArea in a way that the
     * outline does not obfuscate this viewpoint.
     */
    public void bring(Player player) {
        final Vec3d origin = area.getCenterExact();
        final Vec3d playerVector = origin.add(0.0, 0.0, (area.getSizeZ() / 2) + 4);
        final Location location = playerVector.toLocation(getWorld());
        location.setDirection(origin.subtract(playerVector).toVector());
        player.eject();
        player.leaveVehicle();
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.teleport(location);
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    public void calculateRating() {
        if (ratings.isEmpty()) {
            return;
        }
        int total = 0;
        for (int r : ratings.values()) {
            total += r;
        }
        finalRating = ((double) total) / ((double) ratings.size());
    }
}
