package de.danoeh.antennapod.event.playback;

public class PlaybackHistoryEvent {

    private PlaybackHistoryEvent() {
    }

    public static PlaybackHistoryEvent listUpdated() {
        return new PlaybackHistoryEvent();
    }

    @Override
    public String toString() {
        return "PlaybackHistoryEvent";
    }
}
