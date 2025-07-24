package com.cavetale.buildmything.mode;

import com.cavetale.buildmything.Game;
import com.cavetale.buildmything.GamePlayer;
import com.cavetale.buildmything.phase.BuildPhase;
import com.cavetale.buildmything.phase.CountdownPhase;
import com.cavetale.buildmything.phase.GamePhase;
import com.cavetale.buildmything.phase.RankingPhase;
import com.cavetale.buildmything.phase.RatePhase;
import com.cavetale.buildmything.phase.TimedPhase;
import com.cavetale.buildmything.phase.WarpPlayerToBuildAreaPhase;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static net.kyori.adventure.title.Title.Times.times;

/**
 * In this mode, all players build one and the same item, and then
 * rate each other.
 *
 * I had ideas for improvement but do not remember them now.
 */
public final class BuildMyThingMode implements GameplayMode {
    private Game game;
    private State state = State.INIT;
    private String itemName;
    private long seconds;
    private final EnumMap<State, GamePhase> stateMap = new EnumMap<>(State.class);
    private float progress = 0f;
    private boolean revealItemName = false;

    @Override
    public GameplayType getType() {
        return GameplayType.BUILD_MY_THING;
    }

    @Override
    public void enable(final Game theGame) {
        this.game = theGame;
        game.createOneBuildAreaPerPlayer(16, 16, 16);
        stateMap.put(State.WARP, new WarpPlayerToBuildAreaPhase(game));
        stateMap.put(State.COUNTDOWN, new CountdownPhase(game, Duration.ofSeconds(10)));
        stateMap.put(State.BUILD, new BuildPhase(game, Duration.ofMinutes(3)));
        stateMap.put(State.RATE, new RatePhase(game, Duration.ofSeconds(60)));
        stateMap.put(State.RANKING, new RankingPhase(game));
        itemName = game.getPlugin().getWordList().randomWord();
        setState(State.WARP);
    }

    @Override
    public void disable() {
    }

    private void setState(State newState) {
        game.getPlugin().getLogger().info("[" + game.getName() + "] " + state + " => " + newState);
        final State oldState = state;
        // Exit old state
        switch (oldState) {
        case WARP:
            for (GamePlayer gp : game.getAllGamePlayers()) {
                if (gp.getBuildArea() == null) continue;
                gp.getBuildArea().placeFrame();
                gp.getBuildArea().createOutline();
            }
            break;
        case BUILD:
            for (GamePlayer gp : game.getAllGamePlayers()) {
                if (gp.getBuildArea() == null) continue;
                gp.getBuildArea().removeFrame();
                gp.getBuildArea().removeOutline();
            }
            break;
        default: break;
        }
        if (newState == null) {
            game.setFinished(true);
            return;
        }
        // Enter new state
        switch (newState) {
        case BUILD:
            revealItemName = true;
            final Title title = Title.title(text(itemName, GREEN),
                                            text("Time to Build", GRAY, ITALIC),
                                            times(Duration.ofSeconds(2),
                                                  Duration.ofSeconds(3),
                                                  Duration.ofSeconds(1)));
            final Component message = textOfChildren(text("Time to build: ", GRAY),
                                                     text(itemName, GREEN));
            for (Player player : game.getPresentPlayers()) {
                player.showTitle(title);
                player.sendMessage(empty());
                player.sendMessage(message);
                player.sendMessage(empty());
            }
            break;
        default: break;
        }
        state = newState;
        stateMap.get(state).start();
    }

    @Override
    public void tick() {
        if (state == State.INIT) throw new IllegalStateException("state=INIT");
        final GamePhase currentPhase = stateMap.get(state);
        currentPhase.tick();
        if (currentPhase instanceof TimedPhase timedPhase) {
            seconds = timedPhase.getSecondsRemaining();
            progress = timedPhase.getProgress();
        }
        if (currentPhase.isFinished()) {
            setState(state.next());
        }
    }

    public void skip() {
        if (stateMap.get(state) instanceof TimedPhase phase) {
            phase.setFinished(true);
        }
    }

    @Override
    public void onPlayerSidebar(Player player, List<Component> sidebar) {
        sidebar.add(textOfChildren(text(tiny("time "), DARK_GRAY),
                                   text(seconds, GREEN)));
        if (revealItemName) {
            sidebar.add(textOfChildren(text(tiny("item "), DARK_GRAY),
                                       text(itemName, GREEN)));
        } else {
            sidebar.add(textOfChildren(text(tiny("item "), DARK_GRAY),
                                       text("???", DARK_GREEN)));
        }
    }

    @Override
    public BossBar getBossBar(Player player) {
        if (!revealItemName || !game.isPlaying(player)) return null;
        return BossBar.bossBar(text(itemName, GREEN), progress, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
    }

    @Override
    public void onRateCommand(Player player) {
        if (state != State.RATE) return;
        if (stateMap.get(State.RATE) instanceof RatePhase phase) {
            phase.onRateCommand(player);
        }
    }

    private enum State {
        INIT,
        WARP,
        COUNTDOWN,
        BUILD,
        RATE,
        RANKING,
        ;

        public State next() {
            final State[] vs = values();
            final int i = ordinal();
            if (i + 1 >= vs.length) return null;
            return vs[i + 1];
        }
    }
}
