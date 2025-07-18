package com.cavetale.buildmything.mode;

import com.cavetale.buildmything.Game;
import com.cavetale.core.event.hud.PlayerHudEvent;

public interface GameplayMode {
    /**
     * The type is unchanging for each implementing class and must
     * correspond with the supplier that is held by the type.
     */
    GameplayType getType();

    /**
     * This is called right after the Game created a world.
     */
    void enable(Game game);

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

    void onPlayerHud(PlayerHudEvent event);
}
