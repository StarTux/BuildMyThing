package com.cavetale.buildmything;

import com.cavetale.afk.AFKPlugin;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.util.Json;
import com.cavetale.fam.trophy.Highscore;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Getter
public final class BuildMyThingPlugin extends JavaPlugin {
    private static BuildMyThingPlugin instance;
    private final BuildMyThingCommand buildmythingCommand = new BuildMyThingCommand(this);
    private final BuildMyThingAdminCommand adminCommand = new BuildMyThingAdminCommand(this);
    private final List<Game> games = new ArrayList<>();
    private final WordList wordList = new WordList(this);
    private final Component title = textOfChildren(text("Build", GREEN),
                                                   text(tiny("my"), DARK_GRAY),
                                                   text("Thing", BLUE));
    private List<Component> highscoreLines = List.of();
    private File tagFile;
    private Tag tag;
    private boolean doUpdateHighscore;
    @Setter private GameVote gameVote;

    public BuildMyThingPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        buildmythingCommand.enable();
        adminCommand.enable();
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
        wordList.load();
        new GameListener(this).enable();
        new LobbyListener(this).enable();
        tagFile = new File(getDataFolder(), "tag.json");
        loadTag();
        computeHighscore();
    }

    @Override
    public void onDisable() {
        saveTag();
        for (Game game : games) {
            game.disable();
        }
        games.clear();
    }

    public void addGame(Game game) {
        games.add(game);
    }

    public boolean removeGame(Game game) {
        return games.remove(game);
    }

    private void tick() {
        Game removeGame = null;
        for (Game game : games) {
            if (game.isFinished()) {
                removeGame = game;
                continue;
            }
            try {
                game.tick();
            } catch (Exception e) {
                removeGame = game;
                getLogger().log(Level.SEVERE, "Ticking game " + game.getName(), e);
                break;
            }
        }
        if (removeGame != null) {
            games.remove(removeGame);
            removeGame.disable();
        }
        if (doUpdateHighscore) {
            doUpdateHighscore = false;
            computeHighscore();
        }
        // Start or stop game vote.
        int availablePlayers = 0;
        for (Player player : Bukkit.getWorlds().get(0).getPlayers()) {
            if (!player.hasPermission("buildmything.buildmything")) continue;
            if (!tag.isEvent() && AFKPlugin.isAfk(player)) continue;
            if (Game.of(player) != null) continue;
            availablePlayers += 1;
        }
        if (availablePlayers > 1 && !tag.isPause() && games.isEmpty()) {
            // Start a vote
            if (gameVote == null) {
                gameVote = new GameVote(this);
                gameVote.enable();
            }
            gameVote.tick();
        } else {
            // Stop a vote
            if (gameVote != null) {
                gameVote.disable();
                gameVote = null;
            }
        }
    }

    public static BuildMyThingPlugin buildMyThingPlugin() {
        return instance;
    }

    public void loadTag() {
        tag = Json.load(tagFile, Tag.class, Tag::new);
    }

    public void saveTag() {
        getDataFolder().mkdirs();
        Json.save(tagFile, tag, true);
    }

    public void computeHighscore() {
        highscoreLines = Highscore.sidebar(Highscore.of(tag.getScores()));
    }

    public void onPlayerHud(PlayerHudEvent event) {
        final List<Component> lines = new ArrayList<>();
        lines.add(title);
        if (tag.isEvent()) {
            lines.addAll(highscoreLines);
        }
        event.sidebar(PlayerHudPriority.DEFAULT, lines);
        if (gameVote != null) {
            gameVote.onPlayerHud(event);
        }
    }

    public void updateHighscoreLater() {
        doUpdateHighscore = true;
    }
}
