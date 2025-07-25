package com.cavetale.buildmything.phase;

import com.cavetale.buildmything.BuildArea;
import com.cavetale.buildmything.Game;
import com.cavetale.buildmything.GamePlayer;
import com.cavetale.buildmything.GameRegion;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.item.font.Glyph;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.title.Title.Times.times;

/**
 * Copy all builds to one central area and present them in order.
 */
@Getter
@RequiredArgsConstructor
public final class RankingPhase implements GamePhase {
    private final Game game;
    private final List<BuildArea> buildAreas;
    private final Vec3i areaSize;
    private final int areaBorder;
    private final GameRegion region;
    private boolean finished;
    private BuildArea currentBuildArea;
    private int buildAreaIndex;
    private Instant pauseTime;
    private List<Component> sidebar;
    private List<BuildArea> targetAreas;
    private int waiting = 0;

    @Override
    public void start() {
        buildAreaIndex = -1;
        sidebar = new ArrayList<>();
        targetAreas = new ArrayList<>(buildAreas.size());
        for (GameRegion subregion : region.createSubregions(areaSize.x, areaSize.z, areaBorder, buildAreas.size())) {
            targetAreas.add(new BuildArea(subregion, subregion.createCentralSelection(areaSize.x, areaSize.y, areaSize.z)));
        }
        waiting = buildAreas.size();
        for (int i = 0; i < buildAreas.size(); i += 1) {
            final BuildArea area = targetAreas.get(i);
            area.loadChunks(chunks -> {
                    waiting -= 1;
                    area.placePodium();
                });
        }
    }

    @Override
    public void tick() {
        if (waiting > 0) return;
        final Instant now = Instant.now();
        if (pauseTime != null && now.isBefore(pauseTime)) return;
        buildAreaIndex += 1;
        if (buildAreaIndex >= buildAreas.size()) {
            finished = true;
            return;
        }
        pauseTime = now.plus(Duration.ofSeconds(20));
        final BuildArea origArea = buildAreas.get(buildAreaIndex);
        final int rank = buildAreas.size() - buildAreaIndex;
        final Component rankNumber = Glyph.toComponent("" + rank);
        // Make a copy
        currentBuildArea = targetAreas.get(buildAreaIndex);
        currentBuildArea.cloneDataFrom(origArea);
        final Title title = Title.title(rankNumber,
                                        text(currentBuildArea.getOwningPlayer().getName(), BLUE),
                                        times(Duration.ofSeconds(2),
                                              Duration.ofSeconds(3),
                                              Duration.ofSeconds(1)));
        final Component message = textOfChildren(rankNumber,
                                                 text(" " + currentBuildArea.getOwningPlayer().getName(), BLUE));
        origArea.copyTo(currentBuildArea);
        currentBuildArea.createTextLabel(text(currentBuildArea.getOwningPlayer().getName()));
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
