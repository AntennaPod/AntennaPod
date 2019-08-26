package de.danoeh.antennapod.core.service.playback;

import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.feed.VolumeReductionSetting;

public class FeedVolumeReduction {
    float getReductionFactor(FeedPreferences preferences) {
        VolumeReductionSetting volumeReductionSetting = preferences.getVolumeReductionSetting();
        if (volumeReductionSetting == VolumeReductionSetting.LIGHT) {
            return 0.5f;
        } else if (volumeReductionSetting == VolumeReductionSetting.HEAVY) {
            return 0.2f;
        }
        return 1.0f;
    }
}
