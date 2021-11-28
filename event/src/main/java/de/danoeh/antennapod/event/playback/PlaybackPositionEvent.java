package de.danoeh.antennapod.event.playback;

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
