package de.danoeh.antennapod.net.ai.service.ad.provider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AdAnalysisProviderFactoryTest {

    @Test
    public void cloudOverrideTakesPrecedenceOverGlobalLocalSetting() {
        assertTrue(AdAnalysisProviderFactory.usesCloudTranscription("cloud:configured_provider", true));
    }

    @Test
    public void cloudOverrideRemainsCloudWhenGlobalSettingIsCloud() {
        assertTrue(AdAnalysisProviderFactory.usesCloudTranscription("cloud:configured_provider", false));
    }

    @Test
    public void localOverrideTakesPrecedenceOverGlobalCloudSetting() {
        assertFalse(AdAnalysisProviderFactory.usesCloudTranscription("small", false));
    }

    @Test
    public void localOverrideRemainsLocalWhenGlobalSettingIsLocal() {
        assertFalse(AdAnalysisProviderFactory.usesCloudTranscription("small", true));
    }

    @Test
    public void missingOverrideUsesGlobalLocalSetting() {
        assertFalse(AdAnalysisProviderFactory.usesCloudTranscription(null, true));
    }

    @Test
    public void missingOverrideUsesGlobalCloudSetting() {
        assertTrue(AdAnalysisProviderFactory.usesCloudTranscription(null, false));
    }

    @Test
    public void emptyOverrideUsesGlobalSetting() {
        assertTrue(AdAnalysisProviderFactory.usesCloudTranscription("", false));
        assertFalse(AdAnalysisProviderFactory.usesCloudTranscription("", true));
    }
}
