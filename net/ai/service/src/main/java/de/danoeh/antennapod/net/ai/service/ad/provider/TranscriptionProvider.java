package de.danoeh.antennapod.net.ai.service.ad.provider;

import android.os.Build;
import androidx.annotation.RequiresApi;
import java.nio.file.Path;

/**
 * Interface for a component that can transcribe audio files.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public interface TranscriptionProvider extends AutoCloseable {

    /**
     * Transcribes a single chunk of audio, retrying up to the supplied maximum
     * attempts.
     */
    String transcribeChunk(Path chunkPath, int chunkIndex, int totalChunks, int maxRetries) throws Exception;

    /**
     * Maximum audio size in bytes that can be sent to the provider per request.
     */
    long getMaxAudioBytes();

    /**
     * Maximum number of chunks that may be transcribed concurrently. Cloud
     * providers return the user-configured limit; local providers leave it
     * unbounded so the CPU and memory policy decides.
     */
    default int getMaxConcurrency() {
        return Integer.MAX_VALUE;
    }

    /**
     * Returns true if the given exception indicates a permanent error and work
     * should not be retried.
     */
    boolean shouldNotRetry(Throwable throwable);

    /**
     * Human-readable error message to persist when transcription fails.
     */
    String buildErrorMessage(Throwable throwable);
}
