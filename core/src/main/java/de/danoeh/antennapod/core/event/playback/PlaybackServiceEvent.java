package de.danoeh.antennapod.core.event.playback;

public class PlaybackServiceEvent {
    public enum Action {
        SERVICE_STARTED,
        SERVICE_SHUT_DOWN
    }

    public final Action action;

    public PlaybackServiceEvent(Action action) {
        this.action = action;
    }
}
