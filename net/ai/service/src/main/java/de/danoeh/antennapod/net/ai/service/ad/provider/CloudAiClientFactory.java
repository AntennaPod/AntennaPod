package de.danoeh.antennapod.net.ai.service.ad.provider;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import com.openai.azure.AzureOpenAIServiceVersion;
import com.openai.azure.credential.AzureApiKeyCredential;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.audio.AudioModel;

import de.danoeh.antennapod.storage.preferences.AzureAiPreferences;
import de.danoeh.antennapod.storage.preferences.CloudAiPreferences;
import de.danoeh.antennapod.storage.preferences.OpenAiPreferences;

/**
 * Builds an {@link OpenAIClient} for the cloud provider selected by the user:
 * either the public OpenAI API or Azure AI Foundry. Foundry exposes an
 * OpenAI-compatible v1 endpoint for chat models from any publisher, while
 * audio transcription deployments may still require the versioned Azure
 * OpenAI-compatible endpoint.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public final class CloudAiClientFactory {

    private CloudAiClientFactory() {
    }

    public static OpenAIClient createAnalysisClient(Context context) {
        if (CloudAiPreferences.isAzure(context)) {
            String endpoint = AzureAiPreferences.getEndpoint(context);
            String apiKey = AzureAiPreferences.getApiKey(context);
            if (TextUtils.isEmpty(endpoint) || TextUtils.isEmpty(apiKey)) {
                throw new IllegalStateException("Missing Azure AI Foundry endpoint or API key");
            }
            return OpenAIOkHttpClient.builder()
                    .baseUrl(toFoundryV1BaseUrl(endpoint))
                    .apiKey(apiKey)
                    .build();
        }
        return createOpenAiClient(context);
    }

    public static OpenAIClient createTranscriptionClient(Context context) {
        if (CloudAiPreferences.isAzure(context)) {
            String endpoint = AzureAiPreferences.getEndpoint(context);
            String apiKey = AzureAiPreferences.getApiKey(context);
            if (TextUtils.isEmpty(endpoint) || TextUtils.isEmpty(apiKey)) {
                throw new IllegalStateException("Missing Azure AI Foundry endpoint or API key");
            }
            return OpenAIOkHttpClient.builder()
                    .baseUrl(toAzureAudioEndpoint(endpoint))
                    .credential(AzureApiKeyCredential.create(apiKey))
                    .azureServiceVersion(AzureOpenAIServiceVersion.fromString(
                            AzureAiPreferences.getApiVersion(context)))
                    .build();
        }
        return createOpenAiClient(context);
    }

    private static OpenAIClient createOpenAiClient(Context context) {
        String apiKey = OpenAiPreferences.getApiKey(context);
        if (TextUtils.isEmpty(apiKey)) {
            throw new IllegalStateException("Missing OpenAI API key");
        }
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * Model used for transcript ad analysis: the configured OpenAI model, or
     * any Chat Completions-compatible Foundry deployment.
     */
    public static String getAnalysisModelName(Context context) {
        if (CloudAiPreferences.isAzure(context)) {
            return AzureAiPreferences.getChatDeployment(context);
        }
        return OpenAiPreferences.getAnalysisModel(context);
    }

    /**
     * Model used for audio transcription: the configured OpenAI model, or an
     * Audio Transcriptions-compatible Foundry deployment.
     */
    public static AudioModel getTranscriptionModel(Context context) {
        if (CloudAiPreferences.isAzure(context)) {
            return AudioModel.of(AzureAiPreferences.getTranscriptionDeployment(context));
        }
        return AudioModel.of(OpenAiPreferences.getTranscriptionModel(context));
    }

    static String toFoundryV1BaseUrl(String endpoint) {
        String normalized = stripTrailingSlashes(endpoint.trim());
        if (normalized.endsWith("/openai/v1")) {
            return normalized;
        }
        return normalized + "/openai/v1";
    }

    static String toAzureAudioEndpoint(String endpoint) {
        String normalized = stripTrailingSlashes(endpoint.trim());
        int openAiPath = normalized.indexOf("/openai/");
        if (openAiPath >= 0) {
            normalized = normalized.substring(0, openAiPath);
        }
        return normalized.replace(".services.ai.azure.com", ".openai.azure.com");
    }

    private static String stripTrailingSlashes(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
