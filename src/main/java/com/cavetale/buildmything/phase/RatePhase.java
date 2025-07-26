package com.cavetale.buildmything.phase;

import com.cavetale.buildmything.BuildArea;
import com.cavetale.buildmything.Game;
import com.cavetale.buildmything.GamePlayer;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Gui;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

/**
 * In this phase, each player has to rate each other player's builds.
 */
public final class RatePhase extends TimedPhase {
    private final Game game;
    private final List<BuildArea> buildAreas;
    // Players who haven't finished rating yet.
    private Set<UUID> pendingPlayers = new HashSet<>();

    public RatePhase(final Game game, final List<BuildArea> buildAreas, final Duration duration) {
        super(duration);
        this.game = game;
        this.buildAreas = buildAreas;
    }

    /**
     * Build a list of players each player has yet to rate.  This
     * includes all players but themselves.  Then we teleport each
     * player to their first area to rate.
     */
    @Override
    public void start() {
        super.start();
        for (GamePlayer gp : game.getPlayingGamePlayers()) {
            pendingPlayers.add(gp.getUuid());
            final List<BuildArea> rateAreaList = new ArrayList<>(buildAreas);
            rateAreaList.removeIf(ba -> ba.getOwningPlayer() == gp);
            Collections.shuffle(rateAreaList);
            gp.setRateAreaList(rateAreaList);
            gp.setRateIndex(0);
            teleportPlayer(gp);
        }
    }

    @Override
    public void tick() {
        super.tick();
    }

    /**
     * Bring a player to their current area to rate.  This is called
     * once at the start and once again right after they rated a
     * build.
     */
    private void teleportPlayer(GamePlayer gp) {
        final Player player = gp.getPlayer();
        if (player == null) return;
        if (gp.getRateIndex() >= gp.getRateAreaList().size()) {
            pendingPlayers.remove(gp.getUuid());
            if (pendingPlayers.isEmpty()) {
                setFinished(true);
            }
            return;
        }
        gp.getRateAreaList().get(gp.getRateIndex()).bringViewer(player);
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(empty());
        player.sendMessage(textOfChildren(Mytems.MOUSE_LEFT, text(" Click Here to Rate this Build", GREEN, BOLD))
                           .hoverEvent(showText(text("Click Here to Rate this Build", GREEN)))
                           .clickEvent(runCommand("/bmt rate")));
        player.sendMessage(empty());
    }

    public void onRateCommand(Player player) {
        if (isFinished()) return;
        if (!game.isPlaying(player)) return;
        final GamePlayer gp = game.getGamePlayer(player);
        if (gp.getRateIndex() >= gp.getRateAreaList().size()) return;
        final BuildArea buildArea = gp.getRateAreaList().get(gp.getRateIndex());
        new RateMenu(player, gp, buildArea).create().open();
    }

    @RequiredArgsConstructor
    private final class RateMenu {
        private final Player player;
        private final GamePlayer gp;
        private final BuildArea buildArea;
        private Gui gui;
        private int rating;

        private RateMenu create() {
            if (gui == null) {
                gui = new Gui()
                    .size(6 * 9)
                    .layer(GuiOverlay.BLANK, BLUE)
                    .title(text("Rate this build", WHITE));
            }
            final List<Mytems> mytems = new ArrayList<>(5);
            for (int i = 1; i <= 5; i += 1) {
                mytems.add(rating >= i ? Mytems.HEART : Mytems.EMPTY_HEART);
            }
            gui.setItem(2, 1, mytems.get(0).createIcon(List.of(text("Stinks", DARK_RED))), click -> onClick(click, 1));
            gui.setItem(3, 1, mytems.get(1).createIcon(List.of(text("Needs improvement", RED))), click -> onClick(click, 2));
            gui.setItem(4, 1, mytems.get(2).createIcon(List.of(text("Good build", YELLOW))), click -> onClick(click, 3));
            gui.setItem(5, 1, mytems.get(3).createIcon(List.of(text("Great build", DARK_GREEN))), click -> onClick(click, 4));
            gui.setItem(6, 1, mytems.get(4).createIcon(List.of(text("Love it!", GREEN))), click -> onClick(click, 5));
            if (rating > 0) {
                gui.setItem(4, 4, Mytems.OK.createIcon(List.of(text("Confirm", GREEN))), this::onClickConfirm);
            }
            return this;
        }

        public void open() {
            gui.open(player);
        }

        private void onClick(InventoryClickEvent event, int newRating) {
            if (!event.isLeftClick()) return;
            if (isFinished()) return;
            if (rating == newRating) return;
            rating = newRating;
            create();
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        }

        private void onClickConfirm(InventoryClickEvent event) {
            if (!event.isLeftClick()) return;
            if (isFinished()) return;
            if (rating == 0) return;
            buildArea.getRatings().put(player.getUniqueId(), rating);
            player.sendMessage(textOfChildren(text("You rated this build with ", GREEN),
                                              text(rating, RED),
                                              Mytems.HEART));
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            player.closeInventory();
            gp.setRateIndex(gp.getRateIndex() + 1);
            teleportPlayer(gp);
        }
    }

    @Override
    public void onFinished() {
        // Calculate average rating given of all players
        final Map<UUID, Double> ratingGiven = new HashMap<>();
        final Map<UUID, Integer> timesRated = new HashMap<>();
        for (BuildArea buildArea : buildAreas) {
            for (Map.Entry<UUID, Integer> entry : buildArea.getRatings().entrySet()) {
                final UUID uuid = entry.getKey();
                final int value = entry.getValue();
                ratingGiven.put(uuid, ratingGiven.getOrDefault(uuid, 0.0) + (double) value);
                timesRated.put(uuid, timesRated.getOrDefault(uuid, 0) + 1);
            }
        }
        // Calculate average
        for (UUID uuid : timesRated.keySet()) {
            ratingGiven.put(uuid, ratingGiven.getOrDefault(uuid, 0.0) / (double) timesRated.getOrDefault(uuid, 1));
        }
        game.getPlugin().getLogger().info("[" + game.getName() + "] average ratings: " + ratingGiven);
        // Call it
        for (BuildArea buildArea : buildAreas) {
            buildArea.calculateRating(ratingGiven.getOrDefault(buildArea.getOwningPlayer().getUuid(), 1.0));
        }
        buildAreas.sort(Comparator.comparing(BuildArea::getFinalRating));
    }
}
