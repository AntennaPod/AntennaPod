package de.danoeh.antennapod.model;

import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class VolumeAdaptionSettingTest {

    @Before
    public void setUp() throws Exception {
        VolumeAdaptionSetting.setBoostSupported(false);
    }

    @After
    public void tearDown() throws Exception {
        VolumeAdaptionSetting.setBoostSupported(null);
    }

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
    public void mapLightBoostToInteger() {
        VolumeAdaptionSetting setting = VolumeAdaptionSetting.LIGHT_BOOST;

        assertThat(setting.toInteger(), is(equalTo(3)));
    }

    @Test
    public void mapMediumBoostToInteger() {
        VolumeAdaptionSetting setting = VolumeAdaptionSetting.MEDIUM_BOOST;

        assertThat(setting.toInteger(), is(equalTo(4)));
    }

    @Test
    public void mapHeavyBoostToInteger() {
        VolumeAdaptionSetting setting = VolumeAdaptionSetting.HEAVY_BOOST;

        assertThat(setting.toInteger(), is(equalTo(5)));
    }

    @Test
    public void mapIntegerToVolumeAdaptionSetting() {
        assertThat(VolumeAdaptionSetting.fromInteger(0), is(equalTo(VolumeAdaptionSetting.OFF)));
        assertThat(VolumeAdaptionSetting.fromInteger(1), is(equalTo(VolumeAdaptionSetting.LIGHT_REDUCTION)));
        assertThat(VolumeAdaptionSetting.fromInteger(2), is(equalTo(VolumeAdaptionSetting.HEAVY_REDUCTION)));
        assertThat(VolumeAdaptionSetting.fromInteger(3), is(equalTo(VolumeAdaptionSetting.LIGHT_BOOST)));
        assertThat(VolumeAdaptionSetting.fromInteger(4), is(equalTo(VolumeAdaptionSetting.MEDIUM_BOOST)));
        assertThat(VolumeAdaptionSetting.fromInteger(5), is(equalTo(VolumeAdaptionSetting.HEAVY_BOOST)));
    }

    @Test(expected =  IllegalArgumentException.class)
    public void cannotMapNegativeValues() {
        VolumeAdaptionSetting.fromInteger(-1);
    }

    @Test(expected =  IllegalArgumentException.class)
    public void cannotMapValuesOutOfRange() {
        VolumeAdaptionSetting.fromInteger(6);
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

    @Test
    public void lightBoostYieldsHigherValueThanLightReduction() {
        float lightReductionFactor = VolumeAdaptionSetting.LIGHT_REDUCTION.getAdaptionFactor();

        float lightBoostFactor = VolumeAdaptionSetting.LIGHT_BOOST.getAdaptionFactor();

        assertTrue("Light boost must have higher factor than light reduction", lightBoostFactor > lightReductionFactor);
    }

    @Test
    public void mediumBoostYieldsHigherValueThanLightBoost() {
        float lightBoostFactor = VolumeAdaptionSetting.LIGHT_BOOST.getAdaptionFactor();

        float mediumBoostFactor = VolumeAdaptionSetting.MEDIUM_BOOST.getAdaptionFactor();

        assertTrue("Medium boost must have higher factor than light boost", mediumBoostFactor > lightBoostFactor);
    }

    @Test
    public void heavyBoostYieldsHigherValueThanMediumBoost() {
        float mediumBoostFactor = VolumeAdaptionSetting.MEDIUM_BOOST.getAdaptionFactor();

        float heavyBoostFactor = VolumeAdaptionSetting.HEAVY_BOOST.getAdaptionFactor();

        assertTrue("Heavy boost must have higher factor than medium boost", heavyBoostFactor > mediumBoostFactor);
    }
}
