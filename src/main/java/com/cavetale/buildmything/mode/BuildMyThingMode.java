package com.cavetale.buildmything.mode;

import com.cavetale.buildmything.Game;
import com.cavetale.buildmything.GamePlayer;
import com.cavetale.buildmything.GameRegion;
import com.cavetale.buildmything.phase.BuildPhase;
import com.cavetale.buildmything.phase.CountdownPhase;
import com.cavetale.buildmything.phase.TimedPhase;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.struct.Cuboid;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextColor.color;

public final class BuildMyThingMode implements GameplayMode {
    private Game game;
    private State state = null;
    private String itemName;
    private long seconds;
    private final EnumMap<State, TimedPhase> stateMap = new EnumMap<>(State.class);
    private float progress = 0f;

    @Override
    public GameplayType getType() {
        return GameplayType.BUILD_MY_THING;
    }

    @Override
    public void enable(final Game theGame) {
        this.game = theGame;
        stateMap.put(State.COUNTDOWN, new CountdownPhase(game, Duration.ofSeconds(10)));
        stateMap.put(State.BUILD, new BuildPhase(game, Duration.ofSeconds(60)));
        stateMap.put(State.RATE, new TimedPhase(Duration.ofSeconds(60)));
        for (GamePlayer gp : game.getAllGamePlayers()) {
            final GameRegion region = game.getRegionAllocator().allocateRegion();
            gp.setCurrentRegion(region);
            region.setCurrentPlayer(gp.getUuid());
            final Cuboid buildArea = region.createCentralSelection(16, 16, 16);
            gp.getBuildAreas().add(buildArea);
            game.getPlugin().getLogger().info("[" + game.getName() + "] Loading area: " + gp.getName());
            GameplayUtil.loadArea(game.getWorld(), buildArea, list -> {
                    game.getPlugin().getLogger().info("[" + game.getName() + "] Area loaded: " + gp.getName());
                    GameplayUtil.outlineArea(game.getWorld(), buildArea);
                    final Player player = gp.getPlayer();
                    if (player != null) {
                        GameplayUtil.teleportToViewingArea(player, game.getWorld(), buildArea);
                    }
                });
        }
        itemName = "Water Park"; // TODO
        setState(State.COUNTDOWN);
    }

    @Override
    public void disable() {
    }

    private void setState(State newState) {
        game.getPlugin().getLogger().info("[" + game.getName() + "] " + state + " => " + newState);
        state = newState;
        stateMap.get(state).start();
    }

    @Override
    public void tick() {
        if (state == null) throw new NullPointerException("state=null");
        final TimedPhase currentPhase = stateMap.get(state);
        currentPhase.tick();
        seconds = currentPhase.getSecondsRemaining();
        progress = currentPhase.getProgress();
        if (currentPhase.isFinished()) {
            setState(state.next());
        }
    }

    @Override
    public void onPlayerHud(PlayerHudEvent event) {
        final List<Component> sidebar = new ArrayList<>();
        final TextColor hotpink = color(0xff69b4);
        sidebar.add(game.getPlugin().getTitle());
        sidebar.add(text(tiny("We all build the same item"), hotpink));
        sidebar.add(text(tiny("and then judge every build"), hotpink));
        sidebar.add(textOfChildren(text(tiny("item "), DARK_GRAY),
                                   text(itemName, GREEN)));
        sidebar.add(textOfChildren(text(tiny("time "), DARK_GRAY),
                                   text(seconds, GREEN)));
        event.sidebar(PlayerHudPriority.HIGH, sidebar);
        event.bossbar(PlayerHudPriority.HIGH, text(itemName, GREEN), BossBar.Color.GREEN, BossBar.Overlay.PROGRESS, progress);
    }

    private enum State {
        COUNTDOWN,
        BUILD,
        RATE,
        RESULTS,
        ;

        public State next() {
            return values()[ordinal() + 1];
        }
    }
}
