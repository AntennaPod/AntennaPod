package de.danoeh.antennapod.playback.service;

public class PlaybackEndedEvent {
    public final boolean hasEnded;
    public final boolean wasSkipped;
    public final boolean shouldContinue;
    public final boolean toStoppedState;

    private PlaybackEndedEvent(boolean hasEnded, boolean wasSkipped, boolean shouldContinue, boolean toStoppedState) {
        this.hasEnded = hasEnded;
        this.wasSkipped = wasSkipped;
        this.shouldContinue = shouldContinue;
        this.toStoppedState = toStoppedState;
    }

    public static PlaybackEndedEvent ended(
            boolean hasEnded,
            boolean wasSkipped,
            boolean shouldContinue,
            boolean toStoppedState) {
        return new PlaybackEndedEvent(hasEnded, wasSkipped, shouldContinue, toStoppedState);
    }
}
