package com.cavetale.buildmything.phase;

import com.cavetale.buildmything.BuildArea;
import com.cavetale.buildmything.Game;
import com.cavetale.buildmything.GamePlayer;
import com.cavetale.mytems.item.font.Glyph;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.title.Title.Times.times;

/**
 * In this phase, we look at every build based on their ranking.
 * This requires a previous RatePhase.
 */
@Getter
public final class RankingPhase implements GamePhase {
    private final Game game;
    private boolean finished;
    private List<BuildArea> buildAreas;
    private BuildArea currentBuildArea;
    private int buildAreaIndex;
    private Instant endTime;
    private List<Component> sidebar;

    public RankingPhase(final Game game) {
        this.game = game;
    }

    @Override
    public void start() {
        buildAreas = new ArrayList<>();
        for (GamePlayer gp : game.getPlayers().values()) {
            if (gp.getBuildArea() == null) continue;
            buildAreas.add(gp.getBuildArea());
        }
        buildAreas.sort(Comparator.comparing(BuildArea::getFinalRating));
        buildAreaIndex = -1;
        sidebar = new ArrayList<>();
    }

    @Override
    public void tick() {
        final Instant now = Instant.now();
        if (endTime != null && now.isBefore(endTime)) return;
        buildAreaIndex += 1;
        if (buildAreaIndex >= buildAreas.size()) {
            finished = true;
            return;
        }
        endTime = now.plus(Duration.ofSeconds(20));
        currentBuildArea = buildAreas.get(buildAreaIndex);
        final int rank = buildAreas.size() - buildAreaIndex;
        final Component rankNumber = Glyph.toComponent("" + rank);
        final Title title = Title.title(rankNumber,
                                        text(currentBuildArea.getOwningPlayer().getName(), BLUE),
                                        times(Duration.ofSeconds(2),
                                              Duration.ofSeconds(3),
                                              Duration.ofSeconds(1)));
        final Component message = textOfChildren(rankNumber,
                                                 text(" " + currentBuildArea.getOwningPlayer().getName(), BLUE));
        sidebar.add(0, message);
        for (Player player : game.getOnlinePlayers()) {
            currentBuildArea.bringViewer(player);
            player.setGameMode(GameMode.SPECTATOR);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.showTitle(title);
            player.sendMessage(message);
        }
    }
}
