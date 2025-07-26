package com.cavetale.buildmything;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.util.Json;
import com.cavetale.fam.trophy.Highscore;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
    }

    public void updateHighscoreLater() {
        doUpdateHighscore = true;
    }
}
