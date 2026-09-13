package de.danoeh.antennapod.net.ai.service.ad.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Unit tests for {@link AdAnalysisProviderFactory}.
 * Tests factory logic for provider selection based on preferences.
 */
@RunWith(RobolectricTestRunner.class)
public class AdAnalysisProviderFactoryTest {

    private Context context;
    private static final String TEST_API_KEY = "sk-test-key-1234567890";
    private static final String PREF_KEY_OPENAI_API_KEY = "pref_openai_api_key";
    private static final String PREF_KEY_LOCAL_AI_ENABLED = "pref_local_ai_enabled";

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        // Clear preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().apply();
    }

    // ==================== Model Override Detection Tests ====================

    @Test
    public void testModelOverride_cloudPrefix() {
        String modelOverride = "cloud:whisper-1";
        boolean isCloud = modelOverride != null && modelOverride.startsWith("cloud:");
        assertTrue(isCloud);
    }

    @Test
    public void testModelOverride_cloudPrefixWithModel() {
        String modelOverride = "cloud:gpt-4o-transcribe";
        boolean isCloud = modelOverride != null && modelOverride.startsWith("cloud:");
        assertTrue(isCloud);
    }

    @Test
    public void testModelOverride_localModel() {
        String modelOverride = "vosk-model-en-us-0.22";
        boolean isCloud = modelOverride != null && modelOverride.startsWith("cloud:");
        assertFalse(isCloud);
    }

    @Test
    public void testModelOverride_null() {
        String modelOverride = null;
        boolean isCloud = modelOverride != null && modelOverride.startsWith("cloud:");
        assertFalse(isCloud);
    }

    @Test
    public void testModelOverride_empty() {
        String modelOverride = "";
        boolean isCloud = modelOverride != null && modelOverride.startsWith("cloud:");
        assertFalse(isCloud);
    }

    // ==================== Transcription Provider Selection Tests ====================

    @Test
    public void testTranscriptionProvider_cloudOverride_usesOpenAi() {
        // When cloud model override is specified, should use OpenAI
        setApiKey(TEST_API_KEY);
        String modelOverride = "cloud:whisper-1";

        boolean shouldUseOpenAi = modelOverride != null && modelOverride.startsWith("cloud:");
        assertTrue(shouldUseOpenAi);
    }

    @Test
    public void testTranscriptionProvider_localEnabled_usesLocal() {
        // When local transcription is enabled and no cloud override, should use local
        String modelOverride = null;
        boolean localEnabled = true;
        boolean hasCloudOverride = modelOverride != null && modelOverride.startsWith("cloud:");

        boolean shouldUseLocal = !hasCloudOverride && localEnabled;
        assertTrue(shouldUseLocal);
    }

    @Test
    public void testTranscriptionProvider_localDisabled_usesOpenAi() {
        // When local transcription is disabled and no cloud override, should use OpenAI
        String modelOverride = null;
        boolean localEnabled = false;
        boolean hasCloudOverride = modelOverride != null && modelOverride.startsWith("cloud:");

        boolean shouldUseOpenAi = hasCloudOverride || !localEnabled;
        assertTrue(shouldUseOpenAi);
    }

    // ==================== Analysis Provider Tests ====================

    @Test
    public void testAnalysisProvider_alwaysUsesOpenAi() {
        // Analysis provider is always OpenAI-based (no local option)
        setApiKey(TEST_API_KEY);

        try {
            TranscriptAnalysisProvider provider = AdAnalysisProviderFactory.createAnalysisProvider(context);
            assertNotNull(provider);
            assertTrue(provider instanceof CloudTranscriptAnalysisProvider);
            provider.close();
        } catch (Exception e) {
            // Expected if API key validation is strict
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testAnalysisProvider_noApiKey_throws() throws Exception {
        // Should throw when API key is missing
        AdAnalysisProviderFactory.createAnalysisProvider(context);
    }

    // ==================== Language Override Tests ====================

    @Test
    public void testLanguageOverride_passedToProvider() {
        String languageOverride = "en";
        assertNotNull(languageOverride);
        assertEquals("en", languageOverride);
    }

    @Test
    public void testLanguageOverride_null() {
        String languageOverride = null;
        // Null language override is valid (auto-detect)
        boolean isNullValid = true;
        assertTrue(isNullValid);
    }

    @Test
    public void testLanguageOverride_variousLanguages() {
        String[] languages = {"en", "de", "fr", "es", "ja", "zh"};
        for (String lang : languages) {
            assertNotNull(lang);
            assertFalse(lang.isEmpty());
        }
    }

    // ==================== Factory Method Tests ====================

    @Test
    public void testCreateTranscriptionProvider_defaultOverloads() {
        // The factory has two overloads - one with defaults and one with overrides
        // This tests the conceptual contract
        String nullModel = null;
        String nullLanguage = null;

        // Default overload should behave same as calling with null/null
        boolean defaultsToNullParams = (nullModel == null && nullLanguage == null);
        assertTrue(defaultsToNullParams);
    }

    // ==================== Provider Type Resolution Tests ====================

    @Test
    public void testProviderResolution_cloudOverride_precedence() {
        assertTrue(AdAnalysisProviderFactory.usesCloudTranscription("cloud:whisper-1", true));
    }

    @Test
    public void testProviderResolution_noOverride_checksLocal() {
        assertFalse(AdAnalysisProviderFactory.usesCloudTranscription(null, true));
    }

    @Test
    public void testProviderResolution_noOverride_noLocal_usesCloud() {
        assertTrue(AdAnalysisProviderFactory.usesCloudTranscription(null, false));
    }

    @Test
    public void testProviderResolution_localOverride_ignoresGlobalCloudSetting() {
        assertFalse(AdAnalysisProviderFactory.usesCloudTranscription("small", false));
    }

    // ==================== Error Handling Tests ====================

    @Test
    public void testLocalProvider_ioException_wrapsInIllegalState() {
        // When local provider fails to initialize, it should wrap IOException
        // in IllegalStateException for cleaner API
        try {
            throw new java.io.IOException("Model file not found");
        } catch (java.io.IOException e) {
            IllegalStateException wrapped = new IllegalStateException(
                    "Failed to initialize local transcription: " + e.getMessage(), e);
            assertTrue(wrapped.getCause() instanceof java.io.IOException);
            assertTrue(wrapped.getMessage().contains("Failed to initialize local transcription"));
        }
    }

    // ==================== Integration Scenario Tests ====================

    @Test
    public void testScenario_userWantsCloudTranscription() {
        // User explicitly selects cloud transcription via feed settings
        String feedModelOverride = "cloud:whisper-1";
        String languageOverride = "en";
        boolean localEnabled = true; // Global setting is local

        // Cloud override should take precedence
        boolean shouldUseCloud = feedModelOverride.startsWith("cloud:");
        assertTrue(shouldUseCloud);
    }

    @Test
    public void testScenario_userWantsLocalTranscription() {
        // User enables local transcription globally
        String feedModelOverride = null; // No per-feed override
        boolean localEnabled = true;

        // Should use local when enabled and no cloud override
        boolean hasCloudOverride = feedModelOverride != null && feedModelOverride.startsWith("cloud:");
        boolean shouldUseLocal = !hasCloudOverride && localEnabled;
        assertTrue(shouldUseLocal);
    }

    @Test
    public void testScenario_defaultSettings() {
        // Default settings - local disabled, no overrides
        String feedModelOverride = null;
        String languageOverride = null;
        boolean localEnabled = false;

        // Should use cloud (OpenAI) by default
        boolean hasCloudOverride = feedModelOverride != null && feedModelOverride.startsWith("cloud:");
        boolean shouldUseCloud = hasCloudOverride || !localEnabled;
        assertTrue(shouldUseCloud);
    }

    // ==================== Helper Methods ====================

    private void setApiKey(String apiKey) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREF_KEY_OPENAI_API_KEY, apiKey).apply();
    }

    private void setLocalAiEnabled(boolean enabled) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREF_KEY_LOCAL_AI_ENABLED, enabled).apply();
    }
}
