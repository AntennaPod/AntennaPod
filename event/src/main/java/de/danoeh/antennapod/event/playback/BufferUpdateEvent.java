package de.danoeh.antennapod.event.playback;

public class BufferUpdateEvent {
    private static final float PROGRESS_STARTED = -1;
    private static final float PROGRESS_ENDED = -2;
    final float progress;

    private BufferUpdateEvent(float progress) {
        this.progress = progress;
    }

    public static BufferUpdateEvent started() {
        return new BufferUpdateEvent(PROGRESS_STARTED);
    }

    public static BufferUpdateEvent ended() {
        return new BufferUpdateEvent(PROGRESS_ENDED);
    }

    public static BufferUpdateEvent progressUpdate(float progress) {
        return new BufferUpdateEvent(progress);
    }

    public float getProgress() {
        return progress;
    }

    public boolean hasStarted() {
        return progress == PROGRESS_STARTED;
    }

    public boolean hasEnded() {
        return progress == PROGRESS_ENDED;
    }
}
