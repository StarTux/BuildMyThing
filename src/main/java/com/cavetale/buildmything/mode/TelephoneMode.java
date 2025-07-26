package com.cavetale.buildmything.mode;

import com.cavetale.buildmything.BuildArea;
import com.cavetale.buildmything.Game;
import com.cavetale.buildmything.GamePlayer;
import com.cavetale.buildmything.phase.BuildPhase;
import com.cavetale.buildmything.phase.CountdownPhase;
import com.cavetale.buildmything.phase.GamePhase;
import com.cavetale.buildmything.phase.GuessPhase;
import com.cavetale.buildmything.phase.PausePhase;
import com.cavetale.buildmything.phase.RankingPhase;
import com.cavetale.buildmything.phase.SuggestPhase;
import com.cavetale.buildmything.phase.TimedPhase;
import com.cavetale.buildmything.phase.WarpPlayerToBuildAreaPhase;
import com.cavetale.core.struct.Vec3i;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.title.Title.Times.times;

@RequiredArgsConstructor
public final class TelephoneMode implements GameplayMode {
    private final Vec3i areaSize = Vec3i.of(16, 16, 16);
    private final Game game;
    private State state = State.INIT;
    private GamePhase currentPhase;
    private final List<Chain> chains = new ArrayList<>();
    // Length of each chain.
    private int chainLength;
    private int currentChainIndex;
    private int currentChainRank;

    enum State {
        INIT,
        COUNTDOWN,
        SUGGEST,
        // Begin Loop
        PREPARE_BUILD,
        BUILD,
        POST_BUILD,
        GUESS,
        // End Loop
        POST_LOOP,
        RANK, // Loop on itself
        END,
        ;

        public State next() {
            final State[] vs = values();
            final int i = ordinal();
            if (i + 1 >= vs.length) return null;
            return vs[i + 1];
        }
    }

    @Data
    private final class Chain {
        private final List<GamePlayer> players;
        private final List<BuildArea> areas = new ArrayList<>();
        private final List<String> words = new ArrayList<>();
        private int nextPlayerIndex;
        private GamePlayer currentPlayer;
        private BuildArea currentArea;
        private String currentWord;

        public GamePlayer nextPlayer() {
            currentPlayer = players.get(nextPlayerIndex++);
            return currentPlayer;
        }

        public void pushArea(BuildArea newArea) {
            areas.add(newArea);
            currentArea = newArea;
        }

        public void pushWord(String newWord) {
            words.add(newWord);
            currentWord = newWord;
        }
    }

    @Override
    public GameplayType getType() {
        return GameplayType.TELEPHONE;
    }

    @Override
    public void enable() {
        final List<GamePlayer> fixedPlayerList = new ArrayList<>(game.getPlayingGamePlayers());
        Collections.shuffle(fixedPlayerList);
        chainLength = fixedPlayerList.size();
        for (int i = 0; i < chainLength; i += 1) {
            final List<GamePlayer> chainList = new ArrayList<>();
            for (int j = 0; j < chainLength; j += 1) {
                final int index = (i + j) % chainLength;
                chainList.add(fixedPlayerList.get(index));
            }
            chains.add(new Chain(chainList));
        }
        final Title title = Title.title(getTitle(), empty(), times(Duration.ofSeconds(2), Duration.ofSeconds(3), Duration.ofSeconds(1)));
        game.bringAllPlayers(player -> {
                player.setGameMode(GameMode.ADVENTURE);
                player.showTitle(title);
                player.sendMessage(empty());
                player.sendMessage(getTitle());
                player.sendMessage(text(getDescription(), WHITE));
                player.sendMessage(empty());
            });
        setState(State.COUNTDOWN);
    }

    @Override
    public void disable() {
    }

    private void setState(State newState) {
        final State oldState = state;
        game.getPlugin().getLogger().info("[" + game.getName() + "] State " + oldState + " => " + newState);
        // Disable
        switch (oldState) {
        case SUGGEST:
            currentChainIndex += 1;
            if (currentPhase instanceof SuggestPhase suggestPhase) {
                for (Chain chain : chains) {
                    String word = suggestPhase.getWords().get(chain.getCurrentPlayer().getUuid());
                    if (word == null) {
                        word = game.getPlugin().getWordList().randomWord();
                    } else if (game.isEvent()) {
                        game.addScore(chain.getCurrentPlayer(), 10);
                    }
                    chain.pushWord(word);
                }
            }
            break;
        case GUESS:
            currentChainIndex += 1;
            if (currentPhase instanceof GuessPhase guessPhase) {
                for (Chain chain : chains) {
                    String word = guessPhase.getWords().get(chain.getCurrentPlayer().getUuid());
                    if (word == null) {
                        word = game.getPlugin().getWordList().randomWord();
                    } else if (game.isEvent()) {
                        game.addScore(chain.getCurrentPlayer(), 10);
                    }
                    chain.pushWord(word);
                    chain.currentArea.setGuessName(word);
                }
            }
            break;
        case BUILD:
            currentChainIndex += 1;
            for (Chain chain : chains) {
                chain.currentArea.removeFrame();
                if (game.isEvent()) {
                    if (chain.currentArea.countBlocks() > 10) {
                        game.addScore(chain.currentPlayer, 10);
                    }
                }
            }
            break;
        default:
            break;
        }
        if (newState == null) {
            game.setFinished(true);
            return;
        }
        // Set the new phase and enable it
        state = newState;
        switch (newState) {
        case INIT: throw new IllegalStateException("newState=INIT");
        case COUNTDOWN:
            currentPhase = new CountdownPhase(game, Duration.ofSeconds(10));
            break;
        case SUGGEST: {
            final List<GamePlayer> gamePlayerList = new ArrayList<>();
            for (Chain chain : chains) {
                gamePlayerList.add(chain.nextPlayer());
            }
            currentPhase = new SuggestPhase(Duration.ofSeconds(30), gamePlayerList);
            break;
        }
        case PREPARE_BUILD: {
            final List<GamePlayer> gamePlayerList = new ArrayList<>(); // needed?
            for (Chain chain : chains) {
                gamePlayerList.add(chain.nextPlayer());
            }
            final List<BuildArea> buildAreas = game.createOneBuildAreaPerPlayer(areaSize);
            for (BuildArea buildArea : buildAreas) {
                final Chain chain = findChainWithCurrentPlayer(buildArea.getOwningPlayer());
                if (chain == null) throw new IllegalStateException("No chain: " + buildArea.getOwningPlayer().getName());
                chain.pushArea(buildArea);
                buildArea.setItemName(chain.currentWord);
            }
            final WarpPlayerToBuildAreaPhase warpPhase = new WarpPlayerToBuildAreaPhase(game, buildAreas);
            warpPhase.setAreaCallback(buildArea -> {
                    buildArea.placeFrame();
                    buildArea.createOutline();
                });
            currentPhase = warpPhase;
            break;
        }
        case BUILD: {
            final BuildPhase buildPhase = new BuildPhase(game, Duration.ofMinutes(2));
            currentPhase = buildPhase;
            break;
        }
        case POST_BUILD:
        case POST_LOOP:
            currentPhase = new PausePhase(Duration.ofSeconds(5));
            break;
        case GUESS: {
            final List<GamePlayer> gamePlayerList = new ArrayList<>();
            final List<BuildArea> buildAreaList = new ArrayList<>();
            for (Chain chain : chains) {
                final GamePlayer gp = chain.nextPlayer();
                gamePlayerList.add(gp);
                chain.currentArea.setGuessingPlayer(gp);
                gp.setGuessArea(chain.currentArea);
                buildAreaList.add(chain.currentArea);
            }
            currentPhase = new GuessPhase(Duration.ofSeconds(30), buildAreaList);
            break;
        }
        case RANK: {
            final Chain chain = chains.get(currentChainRank++);
            final RankingPhase rankingPhase = new RankingPhase(game, chain.areas, areaSize, 4, game.getRegionAllocator().allocateRegion());
            rankingPhase.setUseRanks(false);
            currentPhase = rankingPhase;
            break;
        }
        case END:
            currentPhase = new PausePhase(Duration.ofSeconds(60));
            break;
        default:
            throw new IllegalStateException("newState=" + newState);
        }
        currentPhase.start();
    }

    @Override
    public void tick() {
        if (state == State.INIT) throw new IllegalStateException("state=INIT");
        currentPhase.tick();
        if (currentPhase.isFinished()) {
            // The guess and build states loop until we have finished all the
            // chains.
            if ((state == State.SUGGEST || state == State.GUESS || state == State.BUILD) && currentChainIndex + 1 >= chainLength) {
                // Exit loop
                setState(State.POST_LOOP);
            } else if (state == State.RANK && currentChainRank < chains.size()) {
                // Loop the rank
                setState(State.RANK);
            } else if (state == State.GUESS && currentChainIndex + 1 < chainLength) {
                // Loop back
                setState(State.PREPARE_BUILD);
            } else {
                setState(state.next());
            }
        }
    }

    @Override
    public void skip() {
        if (currentPhase instanceof TimedPhase timed) {
            timed.setFinished(true);
        }
    }

    @Override
    public void onPlayerSidebar(Player player, List<Component> sidebar) {
        final GamePlayer gp = game.getGamePlayer(player);
        if (gp == null || !gp.isPlaying()) return;
        if (!(currentPhase instanceof TimedPhase timed)) return;
        switch (state) {
        case SUGGEST:
        case BUILD:
        case GUESS:
            sidebar.add(textOfChildren(text(tiny("time "), DARK_GRAY), text(timed.getSecondsRemaining(), WHITE)));
        default: break;
        }
        if (state == State.BUILD) {
            sidebar.add(textOfChildren(text(tiny("build "), DARK_GRAY), text(gp.getBuildArea().getItemName(), WHITE)));
        }
    }

    @Override
    public BossBar getBossBar(Player player) {
        final GamePlayer gp = game.getGamePlayer(player);
        if (gp == null || !gp.isPlaying()) return null;
        if (!(currentPhase instanceof TimedPhase timed)) return null;
        switch (state) {
        case SUGGEST:
            return BossBar.bossBar(text("Choose a world", LIGHT_PURPLE), timed.getProgress(), BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
        case GUESS:
            return BossBar.bossBar(text("Guess the build world", LIGHT_PURPLE), timed.getProgress(), BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
        case BUILD:
            return BossBar.bossBar(textOfChildren(text(tiny("build "), DARK_GRAY), text(gp.getBuildArea().getItemName(), GREEN)),
                                   timed.getProgress(), BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        default:
            return null;
        }
    }

    @Override
    public void onGuessCommand(Player player, String word) {
        if (currentPhase instanceof SuggestPhase suggestPhase) {
            suggestPhase.onGuessCommand(player, word);
        } else if (currentPhase instanceof GuessPhase guessPhase) {
            guessPhase.onGuessCommand(player, word);
        }
    }

    private Chain findChainWithCurrentPlayer(GamePlayer gp) {
        for (Chain chain : chains) {
            if (chain.currentPlayer == gp) return chain;
        }
        return null;
    }
}
