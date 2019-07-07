package de.danoeh.antennapod.core.service.playback;

import de.danoeh.antennapod.core.feed.FeedPreferences;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeedVolumeReductionTest {

    @Test
    public void noReductionIfTurnedOff() {
        FeedPreferences feedPreferences = mock(FeedPreferences.class);
        when(feedPreferences.getVolumeReductionSetting()).thenReturn(FeedPreferences.VolumeReductionSetting.OFF);

        FeedVolumeReduction feedVolumeReduction = new FeedVolumeReduction();
        float reductionFactor = feedVolumeReduction.getReductionFactor(feedPreferences);
        assertEquals(1.0f, reductionFactor, 0.01f );
    }

    @Test
    public void lightReductionYieldsHigherValueThanHeavyReduction() {
        FeedPreferences feedPreferences = mock(FeedPreferences.class);
        FeedVolumeReduction feedVolumeReduction = new FeedVolumeReduction();

        when(feedPreferences.getVolumeReductionSetting()).thenReturn(FeedPreferences.VolumeReductionSetting.LIGHT);
        float lightReductionFactor = feedVolumeReduction.getReductionFactor(feedPreferences);

        when(feedPreferences.getVolumeReductionSetting()).thenReturn(FeedPreferences.VolumeReductionSetting.HEAVY);
        float heavyReductionFactor = feedVolumeReduction.getReductionFactor(feedPreferences);

        assertTrue("Light reduction must have higher factor than heavy reduction", lightReductionFactor > heavyReductionFactor);
    }
}