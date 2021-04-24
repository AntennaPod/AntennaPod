package de.danoeh.antennapod.net.sync.model;


import androidx.annotation.NonNull;

import java.util.List;

public class EpisodeActionChanges {

    private final List<EpisodeAction> episodeActions;
    private final long timestamp;

    public EpisodeActionChanges(@NonNull List<EpisodeAction> episodeActions, long timestamp) {
        this.episodeActions = episodeActions;
        this.timestamp = timestamp;
    }

    public List<EpisodeAction> getEpisodeActions() {
        return this.episodeActions;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    @NonNull
    @Override
    public String toString() {
        return "EpisodeActionGetResponse{"
                + "episodeActions=" + episodeActions
                + ", timestamp=" + timestamp
                + '}';
    }
}
