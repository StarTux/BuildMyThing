package com.cavetale.buildmything.phase;

import com.cavetale.buildmything.BuildArea;
import com.cavetale.buildmything.Game;
import com.cavetale.buildmything.GameRegion;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.item.font.Glyph;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.title.Title.Times.times;

/**
 * Copy all builds to one central area and present them in order.
 */
@Data
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
    private boolean useRanks = true;
    private boolean guessShown = false;

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
        if (currentBuildArea != null && !guessShown && currentBuildArea.getGuessingPlayer() != null) {
            pauseTime = now.plus(Duration.ofSeconds(10));
            guessShown = true;
            final Component guess = textOfChildren(text(currentBuildArea.getGuessingPlayer().getName(), WHITE),
                                                   text(" guessed ", GRAY),
                                                   text("\"" + currentBuildArea.getGuessName() + "\"", GOLD));
            for (Player player : game.getPresentPlayers()) {
                player.sendMessage(empty());
                player.sendMessage(guess);
                player.sendMessage(empty());
            }
            return;
        }
        buildAreaIndex += 1;
        if (buildAreaIndex >= buildAreas.size()) {
            finished = true;
            return;
        }
        currentBuildArea = targetAreas.get(buildAreaIndex);
        pauseTime = currentBuildArea.getGuessingPlayer() != null
            ? now.plus(Duration.ofSeconds(5))
            : now.plus(Duration.ofSeconds(15));
        final BuildArea origArea = buildAreas.get(buildAreaIndex);
        final int rank = buildAreas.size() - buildAreaIndex;
        final Component rankNumber = Glyph.toComponent("" + rank);
        guessShown = false;
        // Make a copy
        currentBuildArea.cloneDataFrom(origArea);
        final Title title = Title.title(useRanks ? rankNumber : empty(),
                                        text(currentBuildArea.getOwningPlayer().getName(), BLUE),
                                        times(Duration.ofSeconds(2),
                                              Duration.ofSeconds(3),
                                              Duration.ofSeconds(1)));
        final Component message;
        if (useRanks) {
            message = textOfChildren(rankNumber,
                                     text(" " + currentBuildArea.getOwningPlayer().getName(), BLUE));
        } else {
            message = textOfChildren(text(currentBuildArea.getItemName(), GOLD),
                                     text(" built by ", GRAY),
                                     text(currentBuildArea.getOwningPlayer().getName(), BLUE));
        }
        origArea.copyTo(currentBuildArea);
        if (useRanks) {
            currentBuildArea.createTextLabel(text(currentBuildArea.getOwningPlayer().getName()));
        } else {
            currentBuildArea.createTextLabel(textOfChildren(text("\"" + currentBuildArea.getItemName() + "\"", GOLD),
                                                            newline(),
                                                            text(currentBuildArea.getOwningPlayer().getName(), WHITE)));
        }
        sidebar.add(0, message);
        for (Player player : game.getPresentPlayers()) {
            currentBuildArea.bringViewer(player);
            player.setGameMode(GameMode.SPECTATOR);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.showTitle(title);
            player.sendMessage(empty());
            player.sendMessage(message);
            player.sendMessage(empty());
            if (useRanks && rank == 1) {
                player.playSound(player, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 2f);
            }
        }
    }
}
