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

import de.danoeh.antennapod.storage.preferences.AzureOpenAiPreferences;
import de.danoeh.antennapod.storage.preferences.CloudAiPreferences;
import de.danoeh.antennapod.storage.preferences.OpenAiPreferences;

/**
 * Builds an {@link OpenAIClient} for the cloud provider selected by the user:
 * either the public OpenAI API or an Azure OpenAI resource. With Azure, the
 * model parameter of each request must be the name of a deployment created on
 * the Azure resource, so model selection is also resolved here.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public final class CloudAiClientFactory {

    private CloudAiClientFactory() {
    }

    public static OpenAIClient createClient(Context context) {
        if (CloudAiPreferences.isAzure(context)) {
            String endpoint = AzureOpenAiPreferences.getEndpoint(context);
            String apiKey = AzureOpenAiPreferences.getApiKey(context);
            if (TextUtils.isEmpty(endpoint) || TextUtils.isEmpty(apiKey)) {
                throw new IllegalStateException("Missing Azure OpenAI endpoint or API key");
            }
            return OpenAIOkHttpClient.builder()
                    .baseUrl(endpoint)
                    .credential(AzureApiKeyCredential.create(apiKey))
                    .azureServiceVersion(AzureOpenAIServiceVersion.fromString(
                            AzureOpenAiPreferences.getApiVersion(context)))
                    .build();
        }
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
     * the chat deployment name when Azure is selected.
     */
    public static String getAnalysisModelName(Context context) {
        if (CloudAiPreferences.isAzure(context)) {
            return AzureOpenAiPreferences.getChatDeployment(context);
        }
        return OpenAiPreferences.getAnalysisModel(context);
    }

    /**
     * Model used for audio transcription: the configured OpenAI model, or the
     * transcription deployment name when Azure is selected.
     */
    public static AudioModel getTranscriptionModel(Context context) {
        if (CloudAiPreferences.isAzure(context)) {
            return AudioModel.of(AzureOpenAiPreferences.getTranscriptionDeployment(context));
        }
        return AudioModel.of(OpenAiPreferences.getTranscriptionModel(context));
    }
}
