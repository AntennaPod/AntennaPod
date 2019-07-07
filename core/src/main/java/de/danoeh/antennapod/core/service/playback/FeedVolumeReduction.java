package de.danoeh.antennapod.core.service.playback;

import de.danoeh.antennapod.core.feed.FeedPreferences;

public class FeedVolumeReduction {
    float getReductionFactor(FeedPreferences preferences) {
        FeedPreferences.VolumeReductionSetting volumeReductionSetting = preferences.getVolumeReductionSetting();
        // TODO maxbechtold These numbers should be tested
        if (volumeReductionSetting == FeedPreferences.VolumeReductionSetting.LIGHT) {
            return 0.5f;
        } else if (volumeReductionSetting == FeedPreferences.VolumeReductionSetting.HEAVY) {
            return 0.2f;
        }
        return 1.0f;
    }
}
