package com.cavetale.buildmything.phase;

import com.cavetale.buildmything.Game;
import java.time.Duration;
import lombok.Getter;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.title.Title.Times.times;
import static net.kyori.adventure.title.Title.title;

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
        final Title title = title(text(""),
                                  text(getSecondsRemaining(), GREEN),
                                  times(Duration.ofSeconds(0),
                                        Duration.ofMillis(500),
                                        Duration.ofMillis(500)));
        for (Player player : game.getOnlinePlayers()) {
            player.showTitle(title);
        }
    }
}
