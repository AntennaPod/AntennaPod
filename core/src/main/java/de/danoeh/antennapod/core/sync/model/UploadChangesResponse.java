package de.danoeh.antennapod.core.sync.model;

public abstract class UploadChangesResponse {

    /**
     * timestamp/ID that can be used for requesting changes since this upload.
     */
    public final long timestamp;

    public UploadChangesResponse(long timestamp) {
        this.timestamp = timestamp;
    }
}
