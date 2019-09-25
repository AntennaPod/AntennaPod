package de.danoeh.antennapod.core.event;

public class PlaybackPositionEvent {
    private final int position;
    private final int duration;

    public PlaybackPositionEvent(int position, int duration) {
        this.position = position;
        this.duration = duration;
    }

    public int getPosition() {
        return position;
    }

    public int getDuration() {
        return duration;
    }
}
