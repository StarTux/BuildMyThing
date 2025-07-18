package com.cavetale.buildmything;

import com.cavetale.buildmything.mode.GameplayMode;
import com.cavetale.buildmything.mode.GameplayType;
import com.winthier.creative.file.Files;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import static com.cavetale.buildmything.BuildMyThingPlugin.buildMyThingPlugin;

/**
 * Represents one game.  To launch a game, run the following steps:
 *
 * - Create this object
 * - Add all the players
 * - call enable()
 * - Add it to the Game list in the plugin object
 *
 * Games are ticked as long as they are in the game list.
 *
 * To end a game:
 *
 * - Call disable()
 * - Remove it from the Game list in the plugin object
 */
@Getter
@RequiredArgsConstructor
public final class Game {
    private final BuildMyThingPlugin plugin;
    private final String name; // for debugging
    private RegionAllocator regionAllocator;
    private World world;
    @Setter private GameplayMode mode;
    private final Map<UUID, GamePlayer> players = new HashMap<>();
    @Setter private boolean buildingAllowed;

    public GamePlayer addPlayer(Player player) {
        plugin.getLogger().info("[" + name + "] Adding player: " + player.getName());
        final GamePlayer gamePlayer = new GamePlayer(player);
        gamePlayer.setPlaying(true);
        players.put(gamePlayer.getUuid(), gamePlayer);
        return gamePlayer;
    }

    public GamePlayer getPlayer(Player player) {
        return players.get(player.getUniqueId());
    }

    public void enable() {
        world = createWorld();
        if (mode == null) {
            mode = GameplayType.random().createGameplayMode();
        }
        mode.enable(this);
    }

    public void disable() {
        if (mode != null) {
            mode.disable();
            mode = null;
        }
        if (world != null) {
            world.removePluginChunkTickets(plugin);
            for (Player player : world.getPlayers()) {
                player.eject();
                player.leaveVehicle();
                player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
                player.setGameMode(GameMode.ADVENTURE);
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
            Files.deleteWorld(world);
            world = null;
        }
    }

    private World createWorld() {
        String worldName;
        int suffix = 0;
        do {
            worldName = String.format("tmp_%03d", suffix++);
        } while (new File(Bukkit.getWorldContainer(), worldName).exists());
        plugin.getLogger().info("[" + name + "] Creating world " + worldName);
        final WorldCreator creator = WorldCreator.name(worldName);
        creator.generator("VoidGenerator");
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(false);
        creator.seed(0L);
        creator.type(WorldType.NORMAL);
        creator.keepSpawnLoaded(TriState.FALSE);
        regionAllocator = new RegionAllocator(this, 512);
        return creator.createWorld();
    }

    /**
     * Called by BuildMyThingPlugin.
     */
    protected void tick() {
        if (mode == null) {
            throw new IllegalStateException("mode = null");
        }
        mode.tick();
    }

    public List<GamePlayer> getAllGamePlayers() {
        return List.copyOf(players.values());
    }

    public List<Player> getOnlinePlayers() {
        final List<Player> result = new ArrayList<>();
        for (UUID uuid : players.keySet()) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                result.add(player);
            }
        }
        return result;
    }

    public boolean playerCanBuild(Player player, Block block) {
        if (!buildingAllowed) return false;
        final GamePlayer gp = getPlayer(player);
        if (gp == null || !gp.isPlaying()) return false;
        if (!gp.buildAreasContain(block)) return false;
        return true;
    }

    public static Game in(World inWorld) {
        for (Game game : buildMyThingPlugin().getGames()) {
            if (inWorld.equals(game.world)) return game;
        }
        return null;
    }
}
