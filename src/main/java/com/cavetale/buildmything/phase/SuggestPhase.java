package com.cavetale.buildmything.phase;

import com.cavetale.buildmything.GamePlayer;
import com.cavetale.mytems.Mytems;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import static com.cavetale.buildmything.BuildMyThingPlugin.buildMyThingPlugin;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.event.ClickEvent.suggestCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@Getter
public final class SuggestPhase extends TimedPhase {
    private final Map<UUID, String> words = new HashMap<>();
    private final Map<UUID, GamePlayer> validPlayers = new HashMap<>();

    public SuggestPhase(final Duration duration, final List<GamePlayer> gamePlayers) {
        super(duration);
        for (GamePlayer gp : gamePlayers) {
            validPlayers.put(gp.getUuid(), gp);
        }
    }

    @Override
    public void start() {
        super.start();
        final Component message = textOfChildren(Mytems.MOUSE_LEFT,
                                                 text(" Click here to choose a word", GREEN, BOLD))
            .hoverEvent(showText(text("Choose a word to build", GRAY)))
            .clickEvent(suggestCommand("/bmt choose "));
        for (GamePlayer gp : validPlayers.values()) {
            final Player player = gp.getPlayer();
            if (player == null) continue;
            player.sendMessage(empty());
            player.sendMessage(message);
            player.sendMessage(textOfChildren(text("  For example, ", GRAY), text(buildMyThingPlugin().getWordList().randomWord(), WHITE)));
            player.sendMessage(empty());
        }
    }

    @Override
    public void tick() {
        super.tick();
    }

    public void onGuessCommand(Player player, String word) {
        if (isFinished()) return;
        if (!validPlayers.containsKey(player.getUniqueId())) return;
        words.put(player.getUniqueId(), word);
        player.sendMessage(textOfChildren(text("Word chosen: ", GREEN),
                                          text(word, DARK_GREEN, UNDERLINED)));
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1f, 1f);
        if (words.size() >= validPlayers.size()) {
            setFinished(true);
        }
    }
}
