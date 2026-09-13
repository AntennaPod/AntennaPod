package de.danoeh.antennapod.net.ai.service.ad.provider;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.openai.client.OpenAIClient;
import com.openai.errors.BadRequestException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.RateLimitException;
import com.openai.models.audio.AudioModel;
import com.openai.models.audio.AudioResponseFormat;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import com.openai.models.audio.transcriptions.TranscriptionCreateResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.storage.preferences.CloudAiPreferences;

@RequiresApi(api = Build.VERSION_CODES.O)
public class CloudTranscriptionProvider implements TranscriptionProvider {
    private static final String TAG = "CloudTranscriptionProv";
    private static final long MAX_CLOUD_AUDIO_BYTES = 25L * 1024L * 1024L; // 25 MiB hard limit
    private static final int MAX_RATE_LIMIT_RETRIES = 10;
    private static final long DEFAULT_RATE_LIMIT_WAIT_SECONDS = 60L;
    private static final long MAX_RATE_LIMIT_WAIT_SECONDS = 120L;

    private final Context context;
    private final OpenAIClient client;
    private final AudioModel audioModel;
    private final String languageOverride;

    public CloudTranscriptionProvider(Context context, String languageOverride) {
        this.context = context;
        this.languageOverride = languageOverride;
        this.client = CloudAiClientFactory.createTranscriptionClient(context);
        this.audioModel = CloudAiClientFactory.getTranscriptionModel(context);
    }

    @Override
    public long getMaxAudioBytes() {
        return MAX_CLOUD_AUDIO_BYTES;
    }

    @Override
    public int getMaxConcurrency() {
        return CloudAiPreferences.getTranscriptionParallelism(context);
    }

    @Override
    public String transcribeChunk(Path chunkPath, int chunkIndex, int totalChunks, int maxRetries) throws Exception {
        int attempt = 0;
        int rateLimitRetries = 0;
        String chunkLabel = (chunkIndex + 1) + "/" + totalChunks;
        while (true) {
            if (chunkPath == null || !Files.exists(chunkPath)) {
                throw new IOException("Chunk file missing: " + chunkPath);
            }
            TranscriptionCreateParams.Builder paramsBuilder = TranscriptionCreateParams.builder()
                    .model(audioModel)
                    .file(chunkPath)
                    .responseFormat(AudioResponseFormat.VTT);

            // Add language hint if specified
            if (!TextUtils.isEmpty(languageOverride)) {
                paramsBuilder.language(languageOverride);
                Log.d(TAG, "Using language override: " + languageOverride);
            }

            TranscriptionCreateParams transcriptionParams = paramsBuilder.build();
            Log.d(TAG, "Transcription attempt " + attempt + " for chunk " + chunkLabel);
            try {
                attempt++;
                TranscriptionCreateResponse response = client.audio().transcriptions()
                        .create(transcriptionParams);
                Log.d(TAG, "Transcription " + chunkLabel + " response received OK");

                return response.asTranscription().text();
            } catch (RateLimitException e) {
                // Cloud Whisper deployments have a low per-minute call-rate limit.
                // A 429 is expected under load, not fatal: honor Retry-After and keep trying.
                // Rate-limit waits do not count against the normal I/O retry budget.
                attempt--;
                rateLimitRetries++;
                if (rateLimitRetries > MAX_RATE_LIMIT_RETRIES) {
                    Log.e(TAG, "Transcription " + chunkLabel + " gave up after "
                            + MAX_RATE_LIMIT_RETRIES + " rate-limit retries", e);
                    throw e;
                }
                long waitSeconds = parseRetryAfterSeconds(e);
                Log.w(TAG, "Transcription " + chunkLabel + " rate limited (429); waiting " + waitSeconds
                        + "s before retry " + rateLimitRetries + "/" + MAX_RATE_LIMIT_RETRIES);
                TimeUnit.SECONDS.sleep(waitSeconds);
            } catch (OpenAIIoException e) {
                boolean last = attempt > maxRetries;
                Log.w(TAG, "Transcription attempt " + attempt + " failed (" + e.getMessage() + ")", e);
                if (last) {
                    throw e;
                }
                TimeUnit.MILLISECONDS.sleep(500L * attempt);
            }
        }
    }

    /**
     * Reads the {@code Retry-After} header (in seconds) from a 429 response,
     * clamped to a sane range, falling back to a default when it is missing or
     * unparseable.
     */
    private static long parseRetryAfterSeconds(RateLimitException e) {
        try {
            java.util.List<String> values = e.headers().values("retry-after");
            if (!values.isEmpty()) {
                long seconds = Long.parseLong(values.get(0).trim());
                return Math.max(1L, Math.min(seconds, MAX_RATE_LIMIT_WAIT_SECONDS));
            }
        } catch (RuntimeException ignored) {
            // Fall through to the default wait below.
        }
        return DEFAULT_RATE_LIMIT_WAIT_SECONDS;
    }

    @Override
    public boolean shouldNotRetry(Throwable throwable) {
        String message = throwable.getMessage();
        String normalized = message == null ? "" : message.toLowerCase(Locale.US);
        if (normalized.contains("unsupported audio mime type") || normalized.contains("exceeds 25 mb")) {
            return true;
        }
        if (throwable instanceof BadRequestException) {
            return normalized.contains("could not be decoded") || normalized.contains("format is not supported");
        }
        Throwable cause = throwable.getCause();
        return cause != null && shouldNotRetry(cause);
    }

    @Override
    public String buildErrorMessage(Throwable throwable) {
        String message = throwable.getMessage() == null ? "" : throwable.getMessage();
        if (shouldNotRetry(throwable)) {
            return message + " (Cloud transcription supports limited audio formats up to 25 MB)";
        }
        return message;
    }

    @Override
    public void close() {
    }
}
