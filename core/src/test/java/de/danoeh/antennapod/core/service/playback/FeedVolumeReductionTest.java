package de.danoeh.antennapod.core.service.playback;

import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.feed.VolumeReductionSetting;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeedVolumeReductionTest {

    @Test
    public void noReductionIfTurnedOff() {
        FeedPreferences feedPreferences = mock(FeedPreferences.class);
        when(feedPreferences.getVolumeReductionSetting()).thenReturn(VolumeReductionSetting.OFF);

        FeedVolumeReduction feedVolumeReduction = new FeedVolumeReduction();
        float reductionFactor = feedVolumeReduction.getReductionFactor(feedPreferences);
        assertEquals(1.0f, reductionFactor, 0.01f );
    }

    @Test
    public void lightReductionYieldsHigherValueThanHeavyReduction() {
        FeedPreferences feedPreferences = mock(FeedPreferences.class);
        FeedVolumeReduction feedVolumeReduction = new FeedVolumeReduction();

        when(feedPreferences.getVolumeReductionSetting()).thenReturn(VolumeReductionSetting.LIGHT);
        float lightReductionFactor = feedVolumeReduction.getReductionFactor(feedPreferences);

        when(feedPreferences.getVolumeReductionSetting()).thenReturn(VolumeReductionSetting.HEAVY);
        float heavyReductionFactor = feedVolumeReduction.getReductionFactor(feedPreferences);

        assertTrue("Light reduction must have higher factor than heavy reduction", lightReductionFactor > heavyReductionFactor);
    }
}