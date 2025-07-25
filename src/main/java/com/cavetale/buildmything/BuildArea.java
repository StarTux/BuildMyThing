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
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.potion.PotionEffect;
import org.bukkit.structure.Structure;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import static com.cavetale.buildmything.BuildMyThingPlugin.buildMyThingPlugin;

@Data
@RequiredArgsConstructor
public final class BuildArea {
    private final GameRegion region; // game, world
    private final Cuboid area;
    private GamePlayer owningPlayer;
    private GamePlayer guessingPlayer;
    private String itemName;
    private String guessName;
    private boolean chunksLoaded;
    // Framing
    private final List<Block> frameBlocks = new ArrayList<>();
    private CuboidOutline outline;
    private TextDisplay textLabel;
    // Rating
    private Map<UUID, Integer> ratings = new HashMap<>();
    private double finalRating;

    public Game getGame() {
        return region.getGame();
    }

    public World getWorld() {
        return region.getGame().getWorld();
    }

    public void cloneDataFrom(BuildArea other) {
        owningPlayer = other.owningPlayer;
        guessingPlayer = other.guessingPlayer;
        itemName = other.itemName;
        guessName = other.guessName;
        ratings = other.ratings;
        finalRating = other.finalRating;
    }

    public void loadChunks(Consumer<List<Chunk>> callback) {
        final List<Vec2i> chunkVectorList = area.blockToChunk().enumerateHorizontally();
        final List<Chunk> chunkList = new ArrayList<>();
        for (Vec2i chunkVector : chunkVectorList) {
            getWorld().getChunkAtAsync(chunkVector.x, chunkVector.z, (Consumer<Chunk>) chunk -> {
                    chunksLoaded = true;
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

    public void placePodium() {
        final World world = getWorld();
        for (int z = area.az; z <= area.bz; z += 1) {
            for (int x = area.ax; x <= area.bx; x += 1) {
                final Block block = world.getBlockAt(x, area.ay - 2, z);
                block.setType(Material.GRASS_BLOCK);
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

    public void createTextLabel(Component label) {
        final Location location = area.getFaceCenterExact(BlockFace.UP).add(0.0, 1.0, 0.0).toLocation(region.getGame().getWorld());
        textLabel = location.getWorld().spawn(location, TextDisplay.class, e -> {
                e.text(label);
                e.setShadowed(true);
                e.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                e.setBillboard(TextDisplay.Billboard.VERTICAL);
                final float scale = 4f;
                e.setTransformation(new Transformation(new Vector3f(0f, 0f, 0f),
                                                       new AxisAngle4f(0f, 0f, 0f, 0f),
                                                       new Vector3f(scale, scale, scale),
                                                       new AxisAngle4f(0f, 0f, 0f, 0f)));
            });
    }

    public void removeTextLabel() {
        if (textLabel == null) return;
        textLabel.remove();
        textLabel = null;
    }

    /**
     * Teleport the player so they can nicely overlook the given build
     * area.  This corresponds with outlineArea in a way that the
     * outline does not obfuscate this viewpoint.
     */
    public void bringBuilder(Player player) {
        final Vec3d origin = area.getCenterExact();
        final Vec3d playerVector = origin.add(0.0, 0.0, (area.getSizeZ() / 2) + 4);
        final Location location = playerVector.toLocation(getWorld());
        location.setDirection(origin.subtract(playerVector).toVector());
        player.eject();
        player.leaveVehicle();
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.teleport(location);
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    public void bringViewer(Player player) {
        final Vec3d origin = area.getCenterExact();
        final double angle = ThreadLocalRandom.current().nextDouble() * 2.0 * Math.PI;
        final double radius = 4.0 + 0.5 * Math.sqrt(area.getSizeX() * area.getSizeX() + area.getSizeZ() + area.getSizeZ());
        final Vec3d playerVector = origin.add(Math.cos(angle) * radius,
                                              0.0,
                                              Math.sin(angle) * radius);
        final Location location = playerVector.toLocation(getWorld());
        location.setDirection(origin.subtract(playerVector).toVector());
        player.eject();
        player.leaveVehicle();
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.teleport(location);
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    public void calculateRating(double fairnessFactor) {
        if (ratings.isEmpty()) {
            return;
        }
        int total = 0;
        for (int r : ratings.values()) {
            total += r;
        }
        finalRating = ((double) total) / ((double) ratings.size()) * fairnessFactor;
        region.getGame().getPlugin().getLogger().info("[" + region.getGame().getName() + "] Score " + owningPlayer.getName() + ": " + finalRating);
    }

    public void copyTo(BuildArea dest) {
        final World origWorld = getWorld();
        final World destWorld = dest.getWorld();
        final Structure structure = Bukkit.getStructureManager().createStructure();
        final boolean includeEntities = true;
        structure.fill(area.getMin().toLocation(origWorld),
                       area.getMax().add(1, 1, 1).toLocation(origWorld),
                       includeEntities);
        final int palette = 0;
        final float integrity = 1f; // pristine
        structure.place(dest.getArea().getMin().toLocation(destWorld),
                        includeEntities,
                        StructureRotation.NONE, Mirror.NONE,
                        palette,
                        integrity,
                        ThreadLocalRandom.current());
    }
}
