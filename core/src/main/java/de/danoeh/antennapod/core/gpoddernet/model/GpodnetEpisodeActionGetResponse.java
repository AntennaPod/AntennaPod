package de.danoeh.antennapod.core.gpoddernet.model;


import android.support.annotation.NonNull;

import java.util.List;

public class GpodnetEpisodeActionGetResponse {

    @NonNull
    private final List<GpodnetEpisodeAction> episodeActions;
    private final long timestamp;

    public GpodnetEpisodeActionGetResponse(@NonNull List<GpodnetEpisodeAction> episodeActions,
                                           long timestamp) {
        this.episodeActions = episodeActions;
        this.timestamp = timestamp;
    }

    @NonNull
    public List<GpodnetEpisodeAction> getEpisodeActions() {
        return this.episodeActions;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    @NonNull
    @Override
    public String toString() {
        return "GpodnetEpisodeActionGetResponse{" +
                "episodeActions=" + episodeActions +
                ", timestamp=" + timestamp +
                '}';
    }
}
