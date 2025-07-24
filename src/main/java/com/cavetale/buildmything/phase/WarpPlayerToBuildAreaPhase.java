package com.cavetale.buildmything.phase;

import com.cavetale.buildmything.BuildArea;
import com.cavetale.buildmything.Game;
import com.cavetale.buildmything.GamePlayer;
import lombok.RequiredArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/**
 * This phase loads the chunks of every build area and warps its
 * player there.
 *
 * When this phase is finished, all areas will be loaded and the
 * player, provided they are there, be present.
 */
@RequiredArgsConstructor
public final class WarpPlayerToBuildAreaPhase implements GamePhase {
    private final Game game;
    private int waiting;

    @Override
    public void start() {
        for (GamePlayer gp : game.getAllGamePlayers()) {
            waiting += 1;
            final BuildArea buildArea = gp.getBuildArea();
            buildArea.loadChunks(chunks -> {
                    waiting -= 1;
                    final Player player = gp.getPlayer();
                    if (player != null) {
                        buildArea.bring(player);
                        player.setGameMode(GameMode.CREATIVE);
                    }
                });
        }
    }

    @Override
    public void tick() { }

    @Override
    public boolean isFinished() {
        return waiting <= 0;
    }
}
