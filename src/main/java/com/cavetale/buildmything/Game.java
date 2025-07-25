package com.cavetale.buildmything;

import com.cavetale.buildmything.mode.GameplayMode;
import com.cavetale.buildmything.mode.GameplayType;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
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
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import static com.cavetale.buildmything.BuildMyThingPlugin.buildMyThingPlugin;
import static com.cavetale.core.font.Unicode.tiny;
import static com.cavetale.mytems.util.Text.wrapLine;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.TextColor.color;

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
    @Setter private boolean finished;

    public GamePlayer addPlayer(Player player) {
        plugin.getLogger().info("[" + name + "] Adding player: " + player.getName());
        final GamePlayer gamePlayer = new GamePlayer(player);
        gamePlayer.setPlaying(true);
        players.put(gamePlayer.getUuid(), gamePlayer);
        return gamePlayer;
    }

    public GamePlayer addSpectator(Player player) {
        plugin.getLogger().info("[" + name + "] Adding spectator: " + player.getName());
        final GamePlayer gamePlayer = new GamePlayer(player);
        gamePlayer.setPlaying(false);
        players.put(gamePlayer.getUuid(), gamePlayer);
        return gamePlayer;
    }

    public GamePlayer getGamePlayer(UUID uuid) {
        return players.get(uuid);
    }

    public GamePlayer getGamePlayer(Player player) {
        return players.get(player.getUniqueId());
    }

    public boolean isPlaying(Player player) {
        final GamePlayer gp = getGamePlayer(player);
        return gp != null && gp.isPlaying();
    }

    public void enable() {
        createWorld();
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

    private void createWorld() {
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
        world = creator.createWorld();
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, true);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.LOCATOR_BAR, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setTime(6000L);
        world.setPVP(false);
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

    public List<GamePlayer> getPlayingGamePlayers() {
        final List<GamePlayer> result = new ArrayList<>(players.size());
        for (GamePlayer gp : players.values()) {
            if (gp.isPlaying()) result.add(gp);
        }
        return result;
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

    public List<Player> getOnlinePlayingPlayers() {
        final List<Player> result = new ArrayList<>();
        for (GamePlayer gp : players.values()) {
            if (!gp.isPlaying()) continue;
            final Player player = gp.getPlayer();
            if (player == null) continue;
            result.add(player);
        }
        return result;
    }

    public List<Player> getPresentPlayers() {
        return world.getPlayers();
    }

    public boolean playerCanBuild(Player player, Block block) {
        if (!buildingAllowed) return false;
        final GamePlayer gp = getGamePlayer(player);
        if (gp == null || !gp.isPlaying()) return false;
        if (!gp.canBuildHere(block)) return false;
        return true;
    }

    public List<BuildArea> createOneBuildAreaPerPlayer(Vec3i size) {
        final List<BuildArea> result = new ArrayList<>();
        for (GamePlayer gp : players.values()) {
            if (!gp.isPlaying()) continue;
            final GameRegion region = regionAllocator.allocateRegion();
            final Cuboid area = region.createCentralSelection(size.x, size.y, size.z);
            final BuildArea buildArea = new BuildArea(region, area);
            buildArea.setOwningPlayer(gp);
            gp.setBuildArea(buildArea);
            result.add(buildArea);
        }
        return result;
    }

    public static Game in(World inWorld) {
        for (Game game : buildMyThingPlugin().getGames()) {
            if (inWorld.equals(game.world)) return game;
        }
        return null;
    }

    public void onPlayerHud(PlayerHudEvent event) {
        final Player player = event.getPlayer();
        final List<Component> sidebar = new ArrayList<>();
        final TextColor hotpink = color(0xff69b4);
        sidebar.add(mode.getTitle());
        for (String txt: wrapLine(mode.getDescription(), 28)) {
            sidebar.add(text(tiny(txt), hotpink));
        }
        mode.onPlayerSidebar(player, sidebar);
        event.sidebar(PlayerHudPriority.HIGH, sidebar);
        final BossBar bossbar = mode.getBossBar(player);
        if (bossbar != null) {
            event.bossbar(PlayerHudPriority.HIGH, bossbar);
        }
    }
}
