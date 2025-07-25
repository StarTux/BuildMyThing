package com.cavetale.buildmything.mode;

import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface GameplayMode {
    /**
     * The type is unchanging for each implementing class and must
     * correspond with the supplier that is held by the type.
     */
    GameplayType getType();

    default Component getTitle() {
        return getType().getTitle();
    }

    default String getDescription() {
        return getType().getDescription();
    }

    /**
     * This is called right after the Game created a world.
     */
    void enable();

    /**
     * The owning game will warp players back to the lobby and delete
     * the world.  This method must only take care of the rest, which
     * is nothing in many cases.
     */
    void disable();

    /**
     * Called once per tick by the Game.
     */
    void tick();

    /**
     * The skip command has been issued.
     */
    void skip();

    void onPlayerSidebar(Player player, List<Component> sidebar);

    BossBar getBossBar(Player player);

    default void onRateCommand(Player player) { }

    default void onGuessCommand(Player player, String word) { }
}
