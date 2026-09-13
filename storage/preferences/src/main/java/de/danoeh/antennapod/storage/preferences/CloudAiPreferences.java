package de.danoeh.antennapod.storage.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Stores which cloud AI provider (OpenAI or Azure AI Foundry) is used for
 * transcription and ad analysis. Local (on-device) transcription is configured
 * separately in {@link LocalAiPreferences}.
 */
public final class CloudAiPreferences {
    public static final String PROVIDER_OPENAI = "openai";
    public static final String PROVIDER_AZURE = "azure";

    private static final String TAG = "CloudAiPreferences";
    private static final String PREF_NAME = "openai_secure";
    private static final String PREF_PROVIDER = "pref_cloud_ai_provider";
    private static final String PREF_TRANSCRIPTION_PARALLELISM = "pref_cloud_transcription_parallelism";

    public static final int DEFAULT_TRANSCRIPTION_PARALLELISM = 5;
    public static final int MIN_TRANSCRIPTION_PARALLELISM = 1;
    public static final int MAX_TRANSCRIPTION_PARALLELISM = 10;

    private CloudAiPreferences() {
    }

    public static String getProvider(Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs == null) {
            return PROVIDER_OPENAI;
        }
        return prefs.getString(PREF_PROVIDER, PROVIDER_OPENAI);
    }

    public static void setProvider(Context context, @Nullable String provider) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs == null) {
            return;
        }
        if (PROVIDER_AZURE.equals(provider)) {
            prefs.edit().putString(PREF_PROVIDER, PROVIDER_AZURE).apply();
        } else {
            prefs.edit().putString(PREF_PROVIDER, PROVIDER_OPENAI).apply();
        }
    }

    public static boolean isAzure(Context context) {
        return PROVIDER_AZURE.equals(getProvider(context));
    }

    public static int getTranscriptionParallelism(Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs == null) {
            return DEFAULT_TRANSCRIPTION_PARALLELISM;
        }
        int value = prefs.getInt(PREF_TRANSCRIPTION_PARALLELISM, DEFAULT_TRANSCRIPTION_PARALLELISM);
        if (!isValidTranscriptionParallelism(value)) {
            Log.w(TAG, "Invalid stored transcription parallelism: " + value);
            return DEFAULT_TRANSCRIPTION_PARALLELISM;
        }
        return value;
    }

    public static void setTranscriptionParallelism(Context context, int value) {
        if (!isValidTranscriptionParallelism(value)) {
            throw new IllegalArgumentException("Transcription parallelism must be between "
                    + MIN_TRANSCRIPTION_PARALLELISM + " and " + MAX_TRANSCRIPTION_PARALLELISM);
        }
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs != null) {
            prefs.edit().putInt(PREF_TRANSCRIPTION_PARALLELISM, value).apply();
        }
    }

    public static boolean isValidTranscriptionParallelism(int value) {
        return value >= MIN_TRANSCRIPTION_PARALLELISM && value <= MAX_TRANSCRIPTION_PARALLELISM;
    }

    /**
     * Returns true if the currently selected cloud provider has the credentials
     * required to make API calls.
     */
    public static boolean hasCredentials(Context context) {
        if (isAzure(context)) {
            return !TextUtils.isEmpty(AzureAiPreferences.getEndpoint(context))
                    && !TextUtils.isEmpty(AzureAiPreferences.getApiKey(context));
        }
        return !TextUtils.isEmpty(OpenAiPreferences.getApiKey(context));
    }

    @Nullable
    private static SharedPreferences getEncryptedPrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Unable to open encrypted preferences", e);
            return null;
        }
    }
}
