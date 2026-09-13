package de.danoeh.antennapod.net.ai.service.ad;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.openai.errors.UnauthorizedException;

import org.greenrobot.eventbus.EventBus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.model.ad.AdAnalysisResult;
import de.danoeh.antennapod.model.ad.AdSegment;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.ai.service.ad.provider.AdAnalysisProviderFactory;
import de.danoeh.antennapod.net.ai.service.ad.provider.TranscriptAnalysisProvider;
import de.danoeh.antennapod.net.ai.service.ad.provider.TranscriptionProvider;
import de.danoeh.antennapod.storage.database.AdSegmentStore;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.ui.i18n.R;
import de.danoeh.antennapod.ui.notifications.NotificationUtils;
import de.danoeh.antennapod.ui.transcript.TranscriptUtils;

/**
 * Combined worker that performs both transcription and transcript analysis.
 * This worker will first transcribe the audio, then analyze the transcript for ad segments.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class AdAnalysisWorker extends Worker {
    public static final String DATA_FEED_ITEM_ID = "feedItemId";
    /** When true, reuse an existing stored transcript and run only the ad-analysis step. */
    public static final String DATA_ANALYSIS_ONLY = "analysisOnly";
    private static final String TAG = "AdAnalysisWorker";
    private static final int FOREGROUND_NOTIFICATION_ID = 0x0AD0A11;

    private String foregroundEpisodeTitle = "";

    public AdAnalysisWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        long feedItemId = getInputData().getLong(DATA_FEED_ITEM_ID, -1);
        AdAnalysisRunObserver runObserver = new AdAnalysisRunObserver(feedItemId);

        if (feedItemId <= 0) {
            runObserver.finished("invalid_feed_item_id");
            return Result.failure();
        }

        FeedItem item = DBReader.getFeedItem(feedItemId);
        if (item == null || item.getMedia() == null) {
            runObserver.finished("missing_feed_item_or_media");
            return Result.failure();
        }
        FeedMedia media = item.getMedia();
        if (TextUtils.isEmpty(media.getLocalFileUrl())) {
            runObserver.finished("no_local_media");
            return Result.success();
        }

        foregroundEpisodeTitle = item.getTitle();

        // Analysis-only mode reuses the transcript already stored for this episode and skips the
        // (expensive) transcription step. Falls back to a full run if no transcript is available.
        boolean analysisOnly = getInputData().getBoolean(DATA_ANALYSIS_ONLY, false);
        String existingTranscript = analysisOnly ? loadExistingTranscript(media) : null;
        boolean skipTranscription = !TextUtils.isEmpty(existingTranscript);

        startForegroundNotification(skipTranscription
                ? AdAnalysisStages.ANALYZING : AdAnalysisStages.TRANSCRIBING, 0, 0, 0);

        AdAnalysisProgressSink progressSink = new WorkManagerAdAnalysisProgressSink(this,
                this::updateForegroundNotification);

        String transcript;
        if (skipTranscription) {
            Log.i(TAG, "Analysis-only: reusing existing transcript for feedItemId=" + feedItemId
                    + ", length=" + existingTranscript.length());
            transcript = existingTranscript;
        } else {
            if (analysisOnly) {
                Log.w(TAG, "Analysis-only requested but no transcript found; running full analysis");
            }
            transcript = performTranscription(item, media, progressSink, runObserver);
        }
        if (TextUtils.isEmpty(transcript)) {
            runObserver.finished(skipTranscription ? "analysis_failed" : "transcription_failed");
            return Result.failure();
        }

        Result result = performAnalysis(feedItemId, item, transcript, progressSink, runObserver);
        runObserver.finished("analysis_completed");
        return result;
    }

    @NonNull
    @Override
    public ListenableFuture<ForegroundInfo> getForegroundInfoAsync() {
        return Futures.immediateFuture(createForegroundInfo(AdAnalysisStages.TRANSCRIBING, 0, 0, 0));
    }

    private void startForegroundNotification(String stage, int percent, int chunksDone, int chunksTotal) {
        try {
            setForegroundAsync(createForegroundInfo(stage, percent, chunksDone, chunksTotal)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "Interrupted while promoting ad analysis to foreground", e);
        } catch (Exception e) {
            Log.w(TAG, "Failed to promote ad analysis to foreground", e);
        }
    }

    private void updateForegroundNotification(String stage, int percent, int chunksDone, int chunksTotal) {
        if (!isStopped()) {
            setForegroundAsync(createForegroundInfo(stage, percent, chunksDone, chunksTotal));
        }
    }

    @NonNull
    private ForegroundInfo createForegroundInfo(String stage, int percent, int chunksDone, int chunksTotal) {
        Context context = getApplicationContext();
        NotificationUtils.createChannels(context);
        Notification notification = createForegroundNotification(context, stage, percent, chunksDone, chunksTotal);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new ForegroundInfo(FOREGROUND_NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        }
        return new ForegroundInfo(FOREGROUND_NOTIFICATION_ID, notification);
    }

    @NonNull
    private Notification createForegroundNotification(Context context, String stage, int percent,
            int chunksDone, int chunksTotal) {
        String status = getForegroundStatusText(context, stage, percent, chunksDone, chunksTotal);
        String title = context.getString(R.string.ad_analysis_notification_title);
        String episodeTitle = TextUtils.isEmpty(foregroundEpisodeTitle)
                ? context.getString(R.string.action_complete_analysis)
                : foregroundEpisodeTitle;
        String bigText = status + "\n" + episodeTitle;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                NotificationUtils.CHANNEL_ID_AD_ANALYSIS)
                .setTicker(title)
                .setContentTitle(title)
                .setContentText(status)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setSmallIcon(de.danoeh.antennapod.ui.notifications.R.drawable.ic_notification_sync)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setWhen(0)
                .setLocalOnly(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(de.danoeh.antennapod.ui.notifications.R.drawable.ic_notification_cancel,
                        context.getString(R.string.cancel_label),
                        WorkManager.getInstance(context).createCancelPendingIntent(getId()));

        PendingIntent launchIntent = createLaunchPendingIntent(context);
        if (launchIntent != null) {
            builder.setContentIntent(launchIntent);
        }

        if (percent >= 0) {
            builder.setProgress(100, Math.min(100, Math.max(0, percent)), false);
        } else {
            builder.setProgress(0, 0, true);
        }
        return builder.build();
    }

    private PendingIntent createLaunchPendingIntent(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent == null) {
            return null;
        }
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(context, FOREGROUND_NOTIFICATION_ID, intent, flags);
    }

    private String getForegroundStatusText(Context context, String stage, int percent,
            int chunksDone, int chunksTotal) {
        String label;
        if (AdAnalysisStages.ANALYZING.equalsIgnoreCase(stage)
                || AdAnalysisStages.TRANSCRIPTION_DONE.equalsIgnoreCase(stage)
                || AdAnalysisStages.DONE.equalsIgnoreCase(stage)) {
            label = context.getString(R.string.ad_analysis_analyzing);
        } else {
            label = context.getString(R.string.ad_analysis_transcribing);
        }
        if (chunksTotal > 0) {
            return label + " (" + chunksDone + "/" + chunksTotal + ")";
        }
        if (percent >= 0) {
            return label + " (" + Math.min(100, Math.max(0, percent)) + "%)";
        }
        return label;
    }

    /**
     * Reads the transcript already stored for this episode, or null if none exists.
     * Used by analysis-only runs so the transcription step can be skipped.
     */
    private String loadExistingTranscript(FeedMedia media) {
        if (media == null || TextUtils.isEmpty(media.getTranscriptFileUrl())) {
            return null;
        }
        try {
            Path path = Paths.get(media.getTranscriptFileUrl());
            if (!Files.exists(path)) {
                return null;
            }
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return TextUtils.isEmpty(content) ? null : content;
        } catch (Exception e) {
            Log.w(TAG, "Failed to read existing transcript", e);
            return null;
        }
    }

    private String performTranscription(FeedItem item, FeedMedia media, AdAnalysisProgressSink progressSink,
            AdAnalysisRunObserver runObserver) {
        runObserver.phaseStarted("transcription");
        progressSink.report(AdAnalysisStages.TRANSCRIBING, 0);

        TranscriptionProvider transcriptionProvider = null;
        String modelOverride = null;
        String languageOverride = null;
        if (item.getFeed() != null && item.getFeed().getPreferences() != null) {
            modelOverride = item.getFeed().getPreferences().getTranscriptionModel();
            languageOverride = item.getFeed().getPreferences().getTranscriptionLanguage();
        }

        try {
            transcriptionProvider = AdAnalysisProviderFactory.createTranscriptionProvider(getApplicationContext(),
                    modelOverride, languageOverride);
        } catch (Exception e) {
            Log.e(TAG, "Transcription provider could not be created", e);
            saveError(item.getId(), e.getMessage(), "transcription", "");
            if (isMemoryError(e)) {
                notifyInsufficientMemory(e);
            }
            closeTranscriptionProvider(transcriptionProvider);
            return null;
        }

        try {
            Log.i(TAG, "Transcription started for feedItemId=" + item.getId()
                    + ", title=" + item.getTitle());

            String transcript = transcribeInChunks(transcriptionProvider, media, progressSink);
            Log.i(TAG, "Transcription complete, length=" + transcript.length());

            // Store transcript
            try {
                TranscriptUtils.storeTranscript(media, transcript);
                Log.i(TAG, "Transcript stored successfully");
            } catch (Exception e) {
                Log.w(TAG, "Failed to store transcript", e);
                saveError(item.getId(), e.getMessage(), "transcription", transcript);
                return null;
            }

            progressSink.report(AdAnalysisStages.TRANSCRIPTION_DONE,
                    AdAnalysisConfig.TRANSCRIPTION_PROGRESS_WEIGHT_PERCENT);
            runObserver.phaseFinished("transcription");
            return transcript;
        } catch (Exception e) {
            runObserver.failed("transcription", e);
            Log.e(TAG, "Transcription failed", e);
            if (isUnauthorized(e)) {
                AdSegmentStore.clear(getApplicationContext(), item.getId());
                notifyInvalidApiKey();
            } else {
                saveError(item.getId(), transcriptionProvider.buildErrorMessage(e), "transcription", "");
            }
            return null;
        } finally {
            closeTranscriptionProvider(transcriptionProvider);
        }
    }

    private Result performAnalysis(long feedItemId, FeedItem item, String transcript,
            AdAnalysisProgressSink progressSink, AdAnalysisRunObserver runObserver) {
        runObserver.phaseStarted("analysis");
        progressSink.report(AdAnalysisStages.ANALYZING, AdAnalysisConfig.TRANSCRIPTION_PROGRESS_WEIGHT_PERCENT);

        TranscriptAnalysisProvider analysisProvider = null;

        try {
            analysisProvider = AdAnalysisProviderFactory.createAnalysisProvider(getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, "Analysis provider could not be created", e);
            saveError(feedItemId, e.getMessage(), null, transcript);
            closeAnalysisProvider(analysisProvider);
            return Result.failure();
        }

        try {
            Log.i(TAG, "Ad analysis started for feedItemId=" + feedItemId
                    + ", title=" + item.getTitle());

            // Check if transcript needs to be split
            List<String> transcriptChunks = TranscriptChunker.split(transcript,
                    AdAnalysisConfig.MAX_TRANSCRIPT_CHARS_PER_CHUNK);
            int totalChunks = transcriptChunks.size();
            Log.i(TAG, "Analyzing transcript in " + totalChunks + " chunk(s)");

            List<AdSegment> allSegments;
            if (totalChunks == 1) {
                // Single chunk - no need for parallel execution
                String content = analysisProvider.analyzeTranscript(transcriptChunks.get(0), percent -> {
                    // Map 0-100% analysis progress to 50-100% overall progress
                    int overallPercent = 50 + (percent / 2);
                    progressSink.report(AdAnalysisStages.ANALYZING, overallPercent);
                });
                allSegments = AdSegmentJsonParser.parse(content);
            } else {
                // Multiple chunks - analyze in parallel
                allSegments = analyzeChunksInParallel(analysisProvider, transcriptChunks, progressSink);
            }

            List<AdSegment> mergedSegments = AdSegmentMerger.merge(allSegments);
            Log.i(TAG, "Ad analysis finished: " + mergedSegments.size() + " segment(s) detected");
            AdSegmentStore.save(getApplicationContext(), feedItemId,
                    new AdAnalysisResult(mergedSegments, System.currentTimeMillis(),
                            analysisProvider.getModelName(), "", transcript));
            progressSink.report(AdAnalysisStages.DONE, 100);
            runObserver.phaseFinished("analysis");
            return Result.success();
        } catch (Exception e) {
            runObserver.failed("analysis", e);
            Log.e(TAG, "Ad analysis failed", e);
            if (isUnauthorized(e)) {
                AdSegmentStore.clear(getApplicationContext(), feedItemId);
                notifyInvalidApiKey();
            }
            if (!isUnauthorized(e)) {
                saveError(feedItemId, e.getMessage(),
                        analysisProvider.getModelName(),
                        transcript);
            }
            return Result.failure();
        } finally {
            closeAnalysisProvider(analysisProvider);
        }
    }

    private String transcribeInChunks(TranscriptionProvider provider, FeedMedia media,
            AdAnalysisProgressSink progressSink) throws Exception {
        List<Path> chunkPaths = AudioChunkUtils.createAudioChunks(getApplicationContext(),
                media.getLocalFileUrl(), AdAnalysisConfig.TRANSCRIPTION_CHUNK_SECONDS);
        Log.i(TAG, "Transcribing " + chunkPaths.size() + " chunk(s) target="
                + AdAnalysisConfig.TRANSCRIPTION_CHUNK_SECONDS + "s each");
        validateChunkSizes(provider, chunkPaths);

        // Parallel execution setup
        int availableProcessors = Runtime.getRuntime().availableProcessors();

        long maxMemory = Runtime.getRuntime().maxMemory();
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long availableMemory = maxMemory - usedMemory;
        int requestedThreads = provider.getMaxConcurrency();
        int threadCount = TranscriptionThreadCountPolicy.chooseThreadCount(
                availableProcessors, maxMemory, usedMemory, requestedThreads);

        Log.i(TAG, "Memory stats: Max=" + (maxMemory / 1024 / 1024) + "MB, Used=" + (usedMemory / 1024 / 1024)
                + "MB, Avail=" + (availableMemory / 1024 / 1024) + "MB. Threads: ByCPU=" + availableProcessors
                + ", RequestedMax=" + requestedThreads + " -> Using " + threadCount + " threads");

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        List<java.util.concurrent.Future<String>> futures = new java.util.ArrayList<>();

        final int totalChunks = chunkPaths.size();
        final double totalProgressParts = totalChunks * 2; // request + success per chunk
        java.util.concurrent.atomic.AtomicInteger doneCount =
                new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger lastReportedPercent =
                new java.util.concurrent.atomic.AtomicInteger(0);

        try {
            // Submit all chunks
            for (int i = 0; i < chunkPaths.size(); i++) {
                // Check if work was cancelled before submitting next chunk
                if (isStopped()) {
                    Log.i(TAG, "Work cancelled, stopping chunk submission");
                    executor.shutdownNow();
                    throw new InterruptedException("Work cancelled");
                }

                final int chunkIndex = i;
                final Path chunkPath = chunkPaths.get(i);

                futures.add(executor.submit(() -> {
                    try {
                        // Check cancellation at start of chunk processing
                        if (Thread.currentThread().isInterrupted() || isStopped()) {
                            Log.i(TAG, "Chunk " + (chunkIndex + 1) + " cancelled");
                            throw new InterruptedException("Chunk processing cancelled");
                        }

                        if (chunkPath == null || !Files.exists(chunkPath)) {
                            Log.e(TAG, "Chunk " + (chunkIndex + 1) + " missing on disk; skipping section");
                            int currentDone = doneCount.incrementAndGet();
                            updateProgressIfIncreased(progressSink, lastReportedPercent, currentDone,
                                    totalProgressParts, totalChunks);
                            return "";
                        }

                        long sizeBytes = Files.size(chunkPath);
                        Log.i(TAG, "Transcribing chunk " + (chunkIndex + 1) + "/" + totalChunks
                                + ": " + chunkPath.getFileName() + " (" + formatBytes(sizeBytes) + ")");

                        // Update progress (started part)
                        int currentDone = doneCount.incrementAndGet();
                        updateProgressIfIncreased(progressSink, lastReportedPercent, currentDone,
                                totalProgressParts, totalChunks);

                        // Transcribe
                        String transcription = provider.transcribeChunk(chunkPath, chunkIndex, totalChunks, 2);
                        Log.d(TAG,
                                "Chunk transcription " + (chunkIndex + 1) + " done, length=" + transcription.length());

                        double offsetSeconds = chunkIndex * AdAnalysisConfig.TRANSCRIPTION_CHUNK_SECONDS;
                        String adjusted = VttTimestampAdjuster.applyOffset(transcription, offsetSeconds);

                        // Update progress (completed part)
                        currentDone = doneCount.incrementAndGet();
                        updateProgressIfIncreased(progressSink, lastReportedPercent, currentDone,
                                totalProgressParts, totalChunks);

                        return adjusted;
                    } catch (Exception e) {
                        Log.e(TAG, "Chunk " + (chunkIndex + 1) + " failed", e);
                        // Forward exception to be caught in main thread
                        throw e;
                    }
                }));
            }

            // Collect results in order
            StringBuilder combined = new StringBuilder();
            Exception firstException = null;

            for (int i = 0; i < futures.size(); i++) {
                // Check if work was cancelled before processing next result
                if (isStopped()) {
                    Log.i(TAG, "Work cancelled while collecting results");
                    executor.shutdownNow();
                    throw new InterruptedException("Work cancelled");
                }

                try {
                    combined.append(futures.get(i).get());
                } catch (java.util.concurrent.ExecutionException e) {
                    // Unwrap the exception
                    Throwable cause = e.getCause();
                    if (isUnauthorized(cause)) {
                        // Immediately stop and rethrow if unauthorized
                        executor.shutdownNow();
                        throw (Exception) cause;
                    }
                    if (cause instanceof InterruptedException) {
                        // Chunk was cancelled
                        executor.shutdownNow();
                        throw new InterruptedException("Chunk cancelled");
                    }
                    if (firstException == null && cause instanceof Exception) {
                        firstException = (Exception) cause;
                    }
                    Log.e(TAG, "Failed to get result for chunk " + (i + 1), e);
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                    throw new java.io.IOException("Transcription interrupted", e);
                }
            }

            if (firstException != null) {
                throw firstException;
            }

            return combined.toString();

        } finally {
            executor.shutdownNow();
            for (Path chunkPath : chunkPaths) {
                try {
                    Files.deleteIfExists(chunkPath);
                } catch (Exception ignored) {
                    // Best-effort cleanup
                }
            }
        }
    }

    private void validateChunkSizes(TranscriptionProvider provider, List<Path> chunkPaths) throws java.io.IOException {
        long maxBytes = provider.getMaxAudioBytes();
        if (maxBytes <= 0) {
            return;
        }
        for (Path chunkPath : chunkPaths) {
            long size = Files.size(chunkPath);
            if (size > maxBytes) {
                Log.e(TAG, "Chunk too large for provider (" + formatBytes(size) + "): " + chunkPath);
                throw new java.io.IOException("Audio chunk exceeds provider limit: " + chunkPath.getFileName());
            }
        }
    }

    private String formatBytes(long bytes) {
        double mb = bytes / (1024.0 * 1024.0);
        return String.format(Locale.US, "%.2f MB", mb);
    }

    private int calculatePercent(int completedParts, double totalParts) {
        if (totalParts <= 0) {
            return 0;
        }
        return (int) Math.min(100, Math.max(0, Math.round((completedParts / totalParts) * 100)));
    }

    /**
     * Updates progress only if the new percentage is higher than the last reported percentage.
     * This prevents the progress bar from going backward when chunks complete out of order.
     */
    private void updateProgressIfIncreased(AdAnalysisProgressSink progressSink,
            java.util.concurrent.atomic.AtomicInteger lastReportedPercent, int currentDone,
            double totalProgressParts, int totalChunks) {
        int newPercent = calculatePercent(currentDone, totalProgressParts);
        int oldPercent = lastReportedPercent.get();

        // Only update if progress increased
        if (newPercent > oldPercent) {
            // Use compareAndSet to avoid race conditions
            if (lastReportedPercent.compareAndSet(oldPercent, newPercent)) {
                // Map transcription progress (0-100%) to overall progress (0-50%)
                int overallPercent = newPercent / 2;
                // Calculate completed chunks (each chunk contributes 2 to doneCount)
                int completedChunks = currentDone / 2;
                progressSink.report(AdAnalysisStages.TRANSCRIBING, overallPercent, completedChunks, totalChunks);
            }
        }
    }

    private List<AdSegment> analyzeChunksInParallel(TranscriptAnalysisProvider provider, List<String> chunks,
            AdAnalysisProgressSink progressSink) throws Exception {
        final int totalChunks = chunks.size();
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(
                Math.min(totalChunks, AdAnalysisConfig.MAX_PARALLEL_ANALYSIS_REQUESTS));
        List<java.util.concurrent.Future<List<AdSegment>>> futures = new java.util.ArrayList<>();
        java.util.concurrent.atomic.AtomicInteger completedChunks = new java.util.concurrent.atomic.AtomicInteger(0);

        try {
            // Submit all chunks for analysis
            for (int i = 0; i < totalChunks; i++) {
                final int chunkIndex = i;
                final String chunk = chunks.get(i);

                futures.add(executor.submit(() -> {
                    Log.i(TAG, "Analyzing chunk " + (chunkIndex + 1) + "/" + totalChunks);
                    String content = provider.analyzeTranscript(chunk, null); // No per-chunk progress for parallel
                    List<AdSegment> segments = AdSegmentJsonParser.parse(content);

                    // Update progress when chunk completes (map to 50-100% overall)
                    int completed = completedChunks.incrementAndGet();
                    int analysisPercent = (completed * 100) / totalChunks;
                    int overallPercent = 50 + (analysisPercent / 2);
                    progressSink.report(AdAnalysisStages.ANALYZING, overallPercent, completed, totalChunks);

                    Log.i(TAG, "Chunk " + (chunkIndex + 1) + " complete, found " + segments.size() + " segment(s)");
                    return segments;
                }));
            }

            // Collect results
            List<AdSegment> allSegments = new ArrayList<>();
            for (java.util.concurrent.Future<List<AdSegment>> future : futures) {
                try {
                    allSegments.addAll(future.get());
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof Exception) {
                        throw (Exception) cause;
                    }
                    throw new Exception("Analysis failed", e);
                }
            }

            return allSegments;
        } finally {
            executor.shutdownNow();
        }
    }

    private void closeTranscriptionProvider(TranscriptionProvider tp) {
        if (tp != null) {
            try {
                tp.close();
            } catch (Exception ignored) {
                Log.w(TAG, "Failed to close transcription provider", ignored);
            }
        }
    }

    private void closeAnalysisProvider(TranscriptAnalysisProvider ap) {
        if (ap != null) {
            try {
                ap.close();
            } catch (Exception ignored) {
                Log.w(TAG, "Failed to close analysis provider", ignored);
            }
        }
    }

    private void saveError(long feedItemId, String error, String model, String transcript) {
        String persistedError = TextUtils.isEmpty(error) ? "Unknown ad analysis error" : error;
        String persistedModel = TextUtils.isEmpty(model) ? "unknown" : model;
        AdSegmentStore.save(getApplicationContext(), feedItemId,
                new AdAnalysisResult(Collections.emptyList(), System.currentTimeMillis(),
                        persistedModel, persistedError, transcript));
    }

    private boolean isUnauthorized(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        if (throwable instanceof UnauthorizedException) {
            return true;
        }
        String message = throwable.getMessage();
        if (message != null) {
            String normalized = message.toLowerCase(Locale.US);
            if (normalized.contains("unauthorized") || normalized.contains("401")) {
                return true;
            }
        }
        return isUnauthorized(throwable.getCause());
    }

    private void notifyInvalidApiKey() {
        try {
            EventBus.getDefault().post(new MessageEvent(
                    getApplicationContext().getString(R.string.ad_analysis_invalid_key)));
        } catch (Exception e) {
            Log.w(TAG, "Failed to notify user about invalid OpenAI API key", e);
        }
    }

    private boolean isMemoryError(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        String message = throwable.getMessage();
        if (message != null) {
            String normalized = message.toLowerCase(Locale.US);
            if (normalized.contains("not enough memory") || normalized.contains("insufficient memory")
                    || normalized.contains("not enough system ram")) {
                return true;
            }
        }
        return isMemoryError(throwable.getCause());
    }

    private void notifyInsufficientMemory(Throwable throwable) {
        try {
            String message = getApplicationContext().getString(R.string.transcription_insufficient_memory_error);

            // Try to extract the model name and required memory from the error message
            String errorMsg = throwable.getMessage();
            if (errorMsg != null && errorMsg.contains("Required:")) {
                message = errorMsg; // Use the detailed error message
            }

            EventBus.getDefault().post(new MessageEvent(message));
        } catch (Exception e) {
            Log.w(TAG, "Failed to notify user about insufficient memory", e);
        }
    }
}
