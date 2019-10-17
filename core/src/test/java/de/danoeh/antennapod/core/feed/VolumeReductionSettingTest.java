package de.danoeh.antennapod.core.feed;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class VolumeReductionSettingTest {

    @Test
    public void mapOffToInteger() {
        VolumeReductionSetting setting = VolumeReductionSetting.OFF;
        assertThat(setting.toInteger(), is(equalTo(0)));
    }

    @Test
    public void mapLightToInteger() {
        VolumeReductionSetting setting = VolumeReductionSetting.LIGHT;

        assertThat(setting.toInteger(), is(equalTo(1)));
    }

    @Test
    public void mapHeavyToInteger() {
        VolumeReductionSetting setting = VolumeReductionSetting.HEAVY;

        assertThat(setting.toInteger(), is(equalTo(2)));
    }

    @Test
    public void mapIntegerToVolumeReductionSetting() {
        assertThat(VolumeReductionSetting.fromInteger(0), is(equalTo(VolumeReductionSetting.OFF)));
        assertThat(VolumeReductionSetting.fromInteger(1), is(equalTo(VolumeReductionSetting.LIGHT)));
        assertThat(VolumeReductionSetting.fromInteger(2), is(equalTo(VolumeReductionSetting.HEAVY)));
    }

    @Test(expected =  IllegalArgumentException.class)
    public void cannotMapNegativeValues() {
        VolumeReductionSetting.fromInteger(-1);
    }

    @Test(expected =  IllegalArgumentException.class)
    public void cannotMapValuesOutOfRange() {
        VolumeReductionSetting.fromInteger(3);
    }

    @Test
    public void noReductionIfTurnedOff() {
        float reductionFactor = VolumeReductionSetting.OFF.getReductionFactor();
        assertEquals(1.0f, reductionFactor, 0.01f);
    }

    @Test
    public void lightReductionYieldsHigherValueThanHeavyReduction() {
        float lightReductionFactor = VolumeReductionSetting.LIGHT.getReductionFactor();

        float heavyReductionFactor = VolumeReductionSetting.HEAVY.getReductionFactor();

        assertTrue("Light reduction must have higher factor than heavy reduction", lightReductionFactor > heavyReductionFactor);
    }
}