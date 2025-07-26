package com.cavetale.buildmything.phase;

import com.cavetale.buildmything.Game;
import java.time.Duration;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Getter
public final class CountdownPhase extends TimedPhase {
    private final Game game;

    public CountdownPhase(final Game game, final Duration duration) {
        super(duration);
        this.game = game;
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void onSecondsRemainingChanged() {
        final Component message = text(getSecondsRemaining(), GRAY);
        for (Player player : game.getPresentPlayers()) {
            player.sendActionBar(message);
        }
    }
}
