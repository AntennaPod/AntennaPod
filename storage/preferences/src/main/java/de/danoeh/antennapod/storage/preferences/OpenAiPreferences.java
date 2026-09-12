package de.danoeh.antennapod.storage.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Secure storage for the user-supplied OpenAI API key.
 */
public final class OpenAiPreferences {
    private static final String TAG = "OpenAiPreferences";
    private static final String PREF_NAME = "openai_secure";
    private static final String PREF_API_KEY = "pref_openai_api_key";
    private static final String PREF_ANALYSIS_MODEL = "pref_openai_model";
    private static final String PREF_TRANSCRIPTION_MODEL = "pref_openai_transcription_model";
    public static final String DEFAULT_ANALYSIS_MODEL = "gpt-5-nano";
    public static final String DEFAULT_TRANSCRIPTION_MODEL = "whisper-1";

    private OpenAiPreferences() {
    }

    /**
     * Returns true if an OpenAI API key is required for ad analysis.
     * Always returns true since local ad analysis has been removed.
     */
    public static boolean isApiKeyRequired(Context context) {
        return true;
    }

    @Nullable
    public static String getApiKey(Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs == null) {
            return null;
        }
        return prefs.getString(PREF_API_KEY, null);
    }

    public static void setApiKey(Context context, @Nullable String apiKey) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs == null) {
            return;
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            prefs.edit().remove(PREF_API_KEY).apply();
        } else {
            prefs.edit().putString(PREF_API_KEY, apiKey.trim()).apply();
        }
    }

    public static String getAnalysisModel(Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs == null) {
            return DEFAULT_ANALYSIS_MODEL;
        }
        return prefs.getString(PREF_ANALYSIS_MODEL, DEFAULT_ANALYSIS_MODEL);
    }

    public static void setAnalysisModel(Context context, @Nullable String model) {
        setModel(context, PREF_ANALYSIS_MODEL, model);
    }

    public static String getTranscriptionModel(Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs == null) {
            return DEFAULT_TRANSCRIPTION_MODEL;
        }
        return prefs.getString(PREF_TRANSCRIPTION_MODEL, DEFAULT_TRANSCRIPTION_MODEL);
    }

    public static void setTranscriptionModel(Context context, @Nullable String model) {
        setModel(context, PREF_TRANSCRIPTION_MODEL, model);
    }

    private static void setModel(Context context, String preferenceKey, @Nullable String model) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs == null) {
            return;
        }
        if (model == null || model.trim().isEmpty()) {
            prefs.edit().remove(preferenceKey).apply();
        } else {
            prefs.edit().putString(preferenceKey, model.trim()).apply();
        }
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
