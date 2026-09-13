package de.danoeh.antennapod.net.ai.service.ad.provider;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;

import de.danoeh.antennapod.storage.preferences.LocalAiPreferences;

/**
 * Factory to construct providers for the ad analysis workflow.
 * Creates both transcription providers (audio → text) and transcript analysis providers (text → ads).
 * Supports cloud-based (OpenAI or Azure AI Foundry) and local (Vosk) providers.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public final class AdAnalysisProviderFactory {
    private static final String TAG = "AdAnalysisProvFactory";
    public static final String CLOUD_TRANSCRIPTION_OVERRIDE = "cloud:configured_provider";

    private AdAnalysisProviderFactory() {
    }

    /**
     * Creates the appropriate transcription provider based on user preferences.
     *
     * @param modelOverride Optional model ID to override global preference
     * @param languageOverride Optional language code for cloud transcription
     */
    public static TranscriptionProvider createTranscriptionProvider(Context context, String modelOverride,
            String languageOverride) {
        if (usesCloudTranscription(context, modelOverride)) {
            Log.i(TAG, "Creating CloudTranscriptionProvider with language: " + languageOverride);
            return new CloudTranscriptionProvider(context, languageOverride);
        }

        Log.i(TAG, "Creating LocalTranscriptionProvider with override: " + modelOverride);
        try {
            return new LocalTranscriptionProvider(context, modelOverride);
        } catch (IOException e) {
            Log.e(TAG, "Failed to create local transcription provider", e);
            throw new IllegalStateException("Failed to initialize local transcription: " + e.getMessage(), e);
        }
    }

    /**
     * Resolves the effective transcription provider. A per-feed selection
     * overrides the global setting in both directions.
     */
    public static boolean usesCloudTranscription(Context context, String modelOverride) {
        return usesCloudTranscription(modelOverride, LocalAiPreferences.isLocalTranscriptionEnabled(context));
    }

    static boolean usesCloudTranscription(String modelOverride, boolean localTranscriptionEnabled) {
        if (modelOverride != null && !modelOverride.isEmpty()) {
            return modelOverride.startsWith("cloud:");
        }
        return !localTranscriptionEnabled;
    }

    /**
     * Creates the configured cloud transcript analysis provider.
     * Local ad analysis has been removed - always use cloud-based analysis.
     */
    public static TranscriptAnalysisProvider createAnalysisProvider(Context context) {
        Log.i(TAG, "Creating CloudTranscriptAnalysisProvider");
        return new CloudTranscriptAnalysisProvider(context);
    }
}
