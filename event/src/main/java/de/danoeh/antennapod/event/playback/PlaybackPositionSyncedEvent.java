package de.danoeh.antennapod.event.playback;

public class PlaybackPositionSyncedEvent {
    private final long mediaId;
    private final int position;

    public PlaybackPositionSyncedEvent(long mediaId, int position) {
        this.mediaId = mediaId;
        this.position = position;
    }

    public long getMediaId() {
        return mediaId;
    }

    public int getPosition() {
        return position;
    }
}
