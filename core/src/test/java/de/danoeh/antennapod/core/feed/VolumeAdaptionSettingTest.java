package de.danoeh.antennapod.core.feed;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class VolumeAdaptionSettingTest {

    @Test
    public void mapOffToInteger() {
        VolumeAdaptionSetting setting = VolumeAdaptionSetting.OFF;
        assertThat(setting.toInteger(), is(equalTo(0)));
    }

    @Test
    public void mapLightReductionToInteger() {
        VolumeAdaptionSetting setting = VolumeAdaptionSetting.LIGHT_REDUCTION;

        assertThat(setting.toInteger(), is(equalTo(1)));
    }

    @Test
    public void mapHeavyReductionToInteger() {
        VolumeAdaptionSetting setting = VolumeAdaptionSetting.HEAVY_REDUCTION;

        assertThat(setting.toInteger(), is(equalTo(2)));
    }

    @Test
    public void mapIntegerToVolumeAdaptionSetting() {
        assertThat(VolumeAdaptionSetting.fromInteger(0), is(equalTo(VolumeAdaptionSetting.OFF)));
        assertThat(VolumeAdaptionSetting.fromInteger(1), is(equalTo(VolumeAdaptionSetting.LIGHT_REDUCTION)));
        assertThat(VolumeAdaptionSetting.fromInteger(2), is(equalTo(VolumeAdaptionSetting.HEAVY_REDUCTION)));
    }

    @Test(expected =  IllegalArgumentException.class)
    public void cannotMapNegativeValues() {
        VolumeAdaptionSetting.fromInteger(-1);
    }

    @Test(expected =  IllegalArgumentException.class)
    public void cannotMapValuesOutOfRange() {
        VolumeAdaptionSetting.fromInteger(3);
    }

    @Test
    public void noAdaptionIfTurnedOff() {
        float adaptionFactor = VolumeAdaptionSetting.OFF.getAdaptionFactor();
        assertEquals(1.0f, adaptionFactor, 0.01f);
    }

    @Test
    public void lightReductionYieldsHigherValueThanHeavyReduction() {
        float lightReductionFactor = VolumeAdaptionSetting.LIGHT_REDUCTION.getAdaptionFactor();

        float heavyReductionFactor = VolumeAdaptionSetting.HEAVY_REDUCTION.getAdaptionFactor();

        assertTrue("Light reduction must have higher factor than heavy reduction", lightReductionFactor > heavyReductionFactor);
    }
}