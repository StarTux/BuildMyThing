package com.cavetale.buildmything.phase;

/**
 * A re-usable phase of a game.
 *
 * Implementors of GameMode will use instances to make parts of their
 * game happen.
 */
public interface GamePhase {
    /**
     * Get ready for this to get ticked.
     *
     * Called by GameMode::tick.
     */
    void start();

    /**
     * Called by the GameMode::tick.
     */
    void tick();

    /**
     * Let the caller know if this phase is now over.
     */
    boolean isFinished();
}
