package de.danoeh.antennapod.model.download;

public class DownloadStatus {
    public static final int STATE_QUEUED = 0;
    public static final int STATE_COMPLETED = 1; // Both successful and not successful
    public static final int STATE_RUNNING = 2;

    private final int state;
    private final int progress;

    public DownloadStatus(int state, int progress) {
        this.state = state;
        this.progress = progress;
    }

    public int getState() {
        return state;
    }

    public int getProgress() {
        return progress;
    }
}
