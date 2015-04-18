package de.danoeh.antennapod.core.gpoddernet.model;


import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

public class GpodnetEpisodeActionGetResponse {

    private final List<GpodnetEpisodeAction> episodeActions;
    private final long timestamp;

    public GpodnetEpisodeActionGetResponse(List<GpodnetEpisodeAction> episodeActions, long timestamp) {
        Validate.notNull(episodeActions);
        this.episodeActions = episodeActions;
        this.timestamp = timestamp;
    }

    public List<GpodnetEpisodeAction> getEpisodeActions() {
        return this.episodeActions;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
