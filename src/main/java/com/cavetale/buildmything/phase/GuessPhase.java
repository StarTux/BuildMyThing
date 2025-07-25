package com.cavetale.buildmything.phase;

import com.cavetale.buildmything.BuildArea;
import com.cavetale.mytems.Mytems;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.event.ClickEvent.suggestCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@Getter
public final class GuessPhase extends TimedPhase {
    private final Map<UUID, String> words = new HashMap<>();
    private final Map<UUID, BuildArea> buildAreaMap = new HashMap<>();

    public GuessPhase(final Duration duration, final List<BuildArea> buildAreas) {
        super(duration);
        for (BuildArea buildArea : buildAreas) {
            buildAreaMap.put(buildArea.getGuessingPlayer().getUuid(), buildArea);
        }
    }

    @Override
    public void start() {
        super.start();
        final Component message = textOfChildren(Mytems.MOUSE_LEFT,
                                                 text(" Click here to guess this build", GREEN, BOLD))
            .hoverEvent(showText(text("Guess this building", GRAY)))
            .clickEvent(suggestCommand("/bmt guess "));
        for (BuildArea buildArea : buildAreaMap.values()) {
            final Player player = buildArea.getGuessingPlayer().getPlayer();
            if (player == null) continue;
            buildArea.bringViewer(player);
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(empty());
            player.sendMessage(message);
            player.sendMessage(empty());
        }
    }

    @Override
    public void tick() {
        super.tick();
    }

    public void onGuessCommand(Player player, String word) {
        if (isFinished()) return;
        if (!buildAreaMap.containsKey(player.getUniqueId())) return;
        words.put(player.getUniqueId(), word);
        player.sendMessage(textOfChildren(text("Build guessed: ", GREEN),
                                          text(word, DARK_GREEN, UNDERLINED)));
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1f, 1f);
        if (words.size() >= buildAreaMap.size()) {
            setFinished(true);
        }
    }
}
