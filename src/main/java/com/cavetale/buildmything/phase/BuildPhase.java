package com.cavetale.buildmything.phase;

import com.cavetale.buildmything.Game;
import java.time.Duration;
import lombok.Getter;

@Getter
public final class BuildPhase extends TimedPhase {
    private final Game game;

    public BuildPhase(final Game game, final Duration duration) {
        super(duration);
        this.game = game;
    }

    @Override
    public void start() {
        super.start();
        game.setBuildingAllowed(true);
    }

    @Override
    public void tick() {
        super.tick();
        if (isFinished()) {
            game.setBuildingAllowed(false);
        }
    }
}
