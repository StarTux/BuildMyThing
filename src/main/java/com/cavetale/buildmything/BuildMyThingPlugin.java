package com.cavetale.buildmything;

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
    private final List<Game> games = new ArrayList<>();
    private final Component title = textOfChildren(text("Build", GREEN),
                                                   text(tiny("my"), DARK_GRAY),
                                                   text("Thing", BLUE));

    public BuildMyThingPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        buildmythingCommand.enable();
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
        new GameListener(this).enable();
    }

    @Override
    public void onDisable() {
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
        Game faultyGame = null;
        for (Game game : games) {
            try {
                game.tick();
            } catch (Exception e) {
                faultyGame = game;
                getLogger().log(Level.SEVERE, "Ticking game " + game.getName(), e);
                break;
            }
        }
        if (faultyGame != null) {
            games.remove(faultyGame);
            faultyGame.disable();
        }
    }

    public static BuildMyThingPlugin buildMyThingPlugin() {
        return instance;
    }
}
