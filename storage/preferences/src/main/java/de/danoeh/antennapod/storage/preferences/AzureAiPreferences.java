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
 * Secure storage for user-supplied Azure AI Foundry connection settings.
 * On Azure, models are addressed by the name of the deployment created in the
 * Foundry portal, not by the underlying model id.
 */
public final class AzureAiPreferences {
    private static final String TAG = "AzureAiPreferences";
    private static final String PREF_NAME = "openai_secure";
    // Keep the existing keys so users do not lose their saved configuration.
    private static final String PREF_ENDPOINT = "pref_azure_openai_endpoint";
    private static final String PREF_API_KEY = "pref_azure_openai_api_key";
    private static final String PREF_API_VERSION = "pref_azure_openai_api_version";
    private static final String PREF_CHAT_DEPLOYMENT = "pref_azure_openai_chat_deployment";
    private static final String PREF_TRANSCRIPTION_DEPLOYMENT = "pref_azure_openai_transcription_deployment";

    public static final String DEFAULT_API_VERSION = "2024-10-21";
    public static final String DEFAULT_CHAT_DEPLOYMENT = "gpt-5-nano";
    public static final String DEFAULT_TRANSCRIPTION_DEPLOYMENT = "whisper";

    private AzureAiPreferences() {
    }

    @Nullable
    public static String getEndpoint(Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs == null) {
            return null;
        }
        return prefs.getString(PREF_ENDPOINT, null);
    }

    public static void setEndpoint(Context context, @Nullable String endpoint) {
        setOrRemove(context, PREF_ENDPOINT, normalizeEndpoint(endpoint));
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
        setOrRemove(context, PREF_API_KEY, apiKey);
    }

    public static String getApiVersion(Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs == null) {
            return DEFAULT_API_VERSION;
        }
        return prefs.getString(PREF_API_VERSION, DEFAULT_API_VERSION);
    }

    public static void setApiVersion(Context context, @Nullable String apiVersion) {
        setOrRemove(context, PREF_API_VERSION, apiVersion);
    }

    public static String getChatDeployment(Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs == null) {
            return DEFAULT_CHAT_DEPLOYMENT;
        }
        return prefs.getString(PREF_CHAT_DEPLOYMENT, DEFAULT_CHAT_DEPLOYMENT);
    }

    public static void setChatDeployment(Context context, @Nullable String deployment) {
        setOrRemove(context, PREF_CHAT_DEPLOYMENT, deployment);
    }

    public static String getTranscriptionDeployment(Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs == null) {
            return DEFAULT_TRANSCRIPTION_DEPLOYMENT;
        }
        return prefs.getString(PREF_TRANSCRIPTION_DEPLOYMENT, DEFAULT_TRANSCRIPTION_DEPLOYMENT);
    }

    public static void setTranscriptionDeployment(Context context, @Nullable String deployment) {
        setOrRemove(context, PREF_TRANSCRIPTION_DEPLOYMENT, deployment);
    }

    @Nullable
    private static String normalizeEndpoint(@Nullable String endpoint) {
        if (endpoint == null) {
            return null;
        }
        String trimmed = endpoint.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static void setOrRemove(Context context, String key, @Nullable String value) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs == null) {
            return;
        }
        if (value == null || value.trim().isEmpty()) {
            prefs.edit().remove(key).apply();
        } else {
            prefs.edit().putString(key, value.trim()).apply();
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
