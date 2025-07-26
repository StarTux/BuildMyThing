package com.cavetale.buildmything.mode;

import com.cavetale.buildmything.Game;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Getter
@RequiredArgsConstructor
public enum GameplayType {
    BUILD_BATTLE(BuildBattleMode::new,
                 textOfChildren(text("Build ", BLUE), text("Battle", GOLD)),
                 "We all build the same item and then judge every build."),
    TELEPHONE(TelephoneMode::new,
              text("Telephone", LIGHT_PURPLE),
              "We build, we guess, we build again."),
    ;

    private final Function<Game, GameplayMode> gameModeSupplier;
    private final Component title;
    private final String description;

    public static GameplayType random() {
        final GameplayType[] all = values();
        return all[ThreadLocalRandom.current().nextInt(all.length)];
    }

    public GameplayMode createGameplayMode(Game game) {
        return gameModeSupplier.apply(game);
    }
}
