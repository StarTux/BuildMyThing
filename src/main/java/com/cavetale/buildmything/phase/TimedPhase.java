package com.cavetale.buildmything.phase;

import java.time.Duration;
import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * This is a useful type of phase which has a timer.  It runs for a
 * specific amount of time, always knows how many seconds are left,
 * and its finished value yields true when the time is up.
 */
@Getter
@RequiredArgsConstructor
public class TimedPhase implements GamePhase {
    private final Duration duration;
    private long secondsRemaining;
    private Instant startTime;
    private Instant stopTime;
    private boolean finished;
    private float progress;

    /**
     * Start sets up the timer.
     */
    @Override
    public void start() {
        this.secondsRemaining = duration.toSeconds();
        this.startTime = Instant.now();
        this.stopTime = startTime.plus(duration);
        this.finished = false;
        this.progress = 0f;
    }

    /**
     * Each tick we check if the timer moved, update the finished
     * flag, and call the changed method if necessary.
     */
    @Override
    public void tick() {
        final Duration timeRemaining = Duration.between(Instant.now(), stopTime);
        final long newSecondsRemaining = timeRemaining.toSeconds();
        progress = 1f - ((float) timeRemaining.toMillis() / (float) duration.toMillis());
        if (newSecondsRemaining <= 0L) {
            finished = true;
        }
        if (newSecondsRemaining != secondsRemaining) {
            secondsRemaining = newSecondsRemaining;
            onSecondsRemainingChanged();
        }
    }

    /**
     * This method can be override to a change to the timer.
     */
    public void onSecondsRemainingChanged() { }
}
