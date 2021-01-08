package de.danoeh.antennapod.core.util.playback;

/**
 * Data object for playback possition
 */
public class PlaybackPosition {
    private int newPosition;
    private long timestamp;

    public PlaybackPosition(int newPosition, long timestamp) {
        this.newPosition = newPosition;
        this.timestamp = timestamp;
    }

    public int getNewPosition() {
        return newPosition;
    }

    public void setNewPosition(int newPosition) {
        this.newPosition = newPosition;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
