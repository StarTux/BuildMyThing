package com.cavetale.buildmything.phase;

import com.cavetale.buildmything.BuildArea;
import com.cavetale.buildmything.Game;
import com.cavetale.buildmything.GamePlayer;
import java.util.List;
import java.util.function.Consumer;
import lombok.Data;
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
@Data
@RequiredArgsConstructor
public final class WarpPlayerToBuildAreaPhase implements GamePhase {
    private final Game game;
    private final List<BuildArea> buildAreas;
    private int waiting;
    private Consumer<BuildArea> areaCallback;

    @Override
    public void start() {
        waiting = buildAreas.size();
        for (BuildArea buildArea : buildAreas) {
            buildArea.loadChunks(chunks -> chunkCallback(buildArea));
        }
    }

    private void chunkCallback(BuildArea buildArea) {
        waiting -= 1;
        final GamePlayer gp = buildArea.getOwningPlayer();
        if (gp == null) return;
        final Player player = gp.getPlayer();
        if (player == null) return;
        buildArea.bringBuilder(player);
        player.setGameMode(GameMode.CREATIVE);
        if (areaCallback != null) {
            areaCallback.accept(buildArea);
        }
    }

    @Override
    public void tick() { }

    @Override
    public boolean isFinished() {
        return waiting <= 0;
    }
}
