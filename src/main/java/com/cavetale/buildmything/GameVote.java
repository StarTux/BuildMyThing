package com.cavetale.buildmything;

import com.cavetale.afk.AFKPlugin;
import com.cavetale.buildmything.mode.GameplayType;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Text;
import io.papermc.paper.datacomponent.DataComponentTypes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.core.font.DefaultFont.bookmarked;
import static io.papermc.paper.datacomponent.item.WrittenBookContent.writtenBookContent;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextColor.color;

@RequiredArgsConstructor
public final class GameVote {
    private final BuildMyThingPlugin plugin;
    private final Map<UUID, GameplayType> votes = new HashMap<>();
    private final Random random = new Random();
    private Instant startTime;
    private Instant endTime;
    private long seconds = TOTAL_SECONDS;
    private static final long TOTAL_SECONDS = 60;
    private BossBar bossBar;
    private ItemStack book;
    private boolean disabled;

    public void enable() {
        for (Player player : Bukkit.getWorlds().get(0).getPlayers()) {
            remind(player);
        }
        startTime = Instant.now();
        endTime = startTime.plus(Duration.ofSeconds(TOTAL_SECONDS));
        bossBar = BossBar.bossBar(text("Game Vote", GREEN), 1f, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_20);
        book = new ItemStack(Material.WRITTEN_BOOK);
        final List<Component> lines = new ArrayList<>();
        for (GameplayType type : GameplayType.values()) {
            final List<Component> tooltip = new ArrayList<>();
            tooltip.add(type.getTitle());
            tooltip.addAll(Text.wrapLore(type.getDescription(), c -> c.color(GRAY)));
            lines.add(type.getTitle()
                      .hoverEvent(showText(join(separator(newline()), tooltip)))
                      .clickEvent(runCommand("/buildmything vote " + type.name().toLowerCase())));
        }
        book.setData(DataComponentTypes.WRITTEN_BOOK_CONTENT,
                     writtenBookContent("Game Vote", "Cavetale")
                     .addPages(toPages(lines)));
    }

    public void disable() {
        disabled = true;
    }

    /**
     * Called by BuildMyThingPlugin::tick once every tick.
     */
    public void tick() {
        if (disabled) {
            throw new IllegalStateException("GameVote disabled");
        }
        final Instant now = Instant.now();
        if (now.isAfter(endTime)) {
            plugin.setGameVote(null);
            disable();
            final List<GameplayType> options = votes.isEmpty()
                ? List.of(GameplayType.values())
                : new ArrayList<>(votes.values());
            final GameplayType type = options.get(random.nextInt(options.size()));
            plugin.getLogger().info("[GameVote] Finished: " + type + " " + votes);
            final Game game = new Game(plugin, "GameVote");
            game.setMode(type.createGameplayMode(game));
            for (Player player : Bukkit.getWorlds().get(0).getPlayers()) {
                if (!player.hasPermission("buildmything.buildmything")) continue;
                if (!plugin.getTag().isEvent() && AFKPlugin.isAfk(player)) continue;
                if (Game.of(player) != null) continue;
                game.addPlayer(player);
            }
            if (game.getPlayers().size() < 2) {
                plugin.getLogger().warning("[GameVote] Not enough players!");
                return;
            }
            game.enable();
            plugin.getGames().add(game);
        } else {
            final Duration timeLeft = Duration.between(now, endTime);
            seconds = timeLeft.toSeconds();
            final float progress = (float) seconds / (float) TOTAL_SECONDS;
            bossBar = BossBar.bossBar(text("Game Vote", GREEN), progress, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_20);
        }
    }

    public void remind(Player player) {
        if (!player.hasPermission("buildmything.buildmything")) return;
        player.sendMessage(textOfChildren(newline(),
                                          Mytems.ARROW_RIGHT,
                                          (text(" Click here to vote for the next game", GREEN)
                                           .hoverEvent(showText(text("Game Selection", GRAY)))
                                           .clickEvent(runCommand("/buildmything vote"))),
                                          newline()));
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.MASTER, 0.5f, 0.5f);
    }

    public void open(Player player) {
        player.closeInventory();
        player.openBook(book);
    }

    public void onVote(Player player, String input) {
        final GameplayType type;
        try {
            type = GameplayType.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException iae) {
            return;
        }
        votes.put(player.getUniqueId(), type);
        player.sendMessage(textOfChildren(Mytems.CHECKED_CHECKBOX,
                                          text(" You voted for ", WHITE),
                                          type.getTitle()));
    }

    /**
     * Called for all players in the lobby by
     * BuildMyThingPlugin::onPlayerHud.
     */
    public void onPlayerHud(PlayerHudEvent event) {
        event.bossbar(PlayerHudPriority.HIGH, bossBar);
    }

    private List<Component> toPages(List<Component> lines) {
        final int lineCount = lines.size();
        final int linesPerPage = 10;
        List<Component> pages = new ArrayList<>((lineCount - 1) / linesPerPage + 1);
        for (int i = 0; i < lineCount; i += linesPerPage) {
            List<Component> page = new ArrayList<>(14);
            page.add(bookmarked(color(0x333333), plugin.getTitle()));
            page.add(empty());
            page.addAll(lines.subList(i, Math.min(lines.size(), i + linesPerPage)));
            pages.add(join(separator(newline()), page));
        }
        return pages;
    }
}
