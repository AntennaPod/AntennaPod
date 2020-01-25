package de.danoeh.antennapod.core.event.settings;

import de.danoeh.antennapod.core.feed.VolumeAdaptionSetting;

public class VolumeAdaptionChangedEvent {
    private final VolumeAdaptionSetting volumeAdaptionSetting;
    private final long feedId;

    public VolumeAdaptionChangedEvent(VolumeAdaptionSetting volumeAdaptionSetting, long feedId) {
        this.volumeAdaptionSetting = volumeAdaptionSetting;
        this.feedId = feedId;
    }

    public VolumeAdaptionSetting getVolumeAdaptionSetting() {
        return volumeAdaptionSetting;
    }

    public long getFeedId() {
        return feedId;
    }
}
