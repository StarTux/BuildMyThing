package com.cavetale.buildmything.mode;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum GameplayType {
    BUILD_MY_THING(BuildMyThingMode::new),
    ;

    private final Supplier<GameplayMode> gameModeSupplier;

    public static GameplayType random() {
        final GameplayType[] all = values();
        return all[ThreadLocalRandom.current().nextInt(all.length)];
    }

    public GameplayMode createGameplayMode() {
        return gameModeSupplier.get();
    }
}
