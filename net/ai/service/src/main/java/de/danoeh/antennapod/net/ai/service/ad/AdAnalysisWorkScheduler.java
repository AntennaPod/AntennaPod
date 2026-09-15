package de.danoeh.antennapod.net.ai.service.ad;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.preferences.CloudAiPreferences;

/**
 * Schedules combined transcription + ad analysis work.
 * Uses a single shared queue name with APPEND_OR_REPLACE policy so that only
 * one episode is processed at a time. Additional requests are queued behind the
 * currently running work, while retries recover from a failed or cancelled
 * queue chain.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public final class AdAnalysisWorkScheduler {
    /** Shared queue name — all episodes share this so they execute serially. */
    public static final String QUEUE_NAME = "ad-analysis-queue";
    /** Per-episode tag prefix for observing / cancelling individual items. */
    public static final String TAG_PREFIX = "ad-analysis-";
    private static final String TITLE_TAG_PREFIX = "ad-analysis-title-";

    private AdAnalysisWorkScheduler() {
    }

    /**
     * Extracts the feed item ID from the tag assigned to an analysis request.
     *
     * @return the feed item ID, or {@code -1} when no valid analysis tag exists
     */
    public static long getFeedItemId(Set<String> tags) {
        if (tags == null) {
            return -1;
        }
        for (String tag : tags) {
            if (tag == null || !tag.startsWith(TAG_PREFIX)) {
                continue;
            }
            try {
                return Long.parseLong(tag.substring(TAG_PREFIX.length()));
            } catch (NumberFormatException ignored) {
                // Ignore malformed tags and keep looking for a valid one.
            }
        }
        return -1;
    }

    @Nullable
    public static String getEpisodeTitle(Set<String> tags) {
        if (tags == null) {
            return null;
        }
        for (String tag : tags) {
            if (tag == null || !tag.startsWith(TITLE_TAG_PREFIX)) {
                continue;
            }
            String encodedTitle = tag.substring(TITLE_TAG_PREFIX.length());
            if (encodedTitle.isEmpty() || !encodedTitle.matches("[A-Za-z0-9_-]+")) {
                continue;
            }
            try {
                byte[] decoded = Base64.decode(encodedTitle, Base64.URL_SAFE | Base64.NO_WRAP);
                if (decoded.length == 0) {
                    continue;
                }
                return new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed title tags and keep looking for a valid one.
            }
        }
        return null;
    }

    /**
     * Queues a complete transcription and analysis run.
     *
     * @return true only when work was actually added to WorkManager
     */
    public static boolean enqueueManual(Context context, FeedMedia media) {
        return enqueue(context, media, false);
    }

    /**
     * Queues an analysis-only run that reuses the transcript already stored for the episode and
     * skips transcription. Returns false if it could not be queued (missing media or credentials).
     */
    public static boolean enqueueAnalysisOnly(Context context, FeedMedia media) {
        return enqueue(context, media, true);
    }

    private static boolean enqueue(Context context, FeedMedia media, boolean analysisOnly) {
        if (media == null || media.getItem() == null) {
            return false;
        }
        FeedItem item = media.getItem();
        if (!CloudAiPreferences.hasCredentials(context)) {
            return false;
        }
        if (TextUtils.isEmpty(media.getLocalFileUrl())) {
            return false;
        }
        Data input = new Data.Builder()
                .putLong(AdAnalysisWorker.DATA_FEED_ITEM_ID, item.getId())
                .putBoolean(AdAnalysisWorker.DATA_ANALYSIS_ONLY, analysisOnly)
                .build();

        // Only require network if not running in fully local mode
        NetworkType networkType = NetworkType.CONNECTED;

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build();

        OneTimeWorkRequest.Builder requestBuilder = new OneTimeWorkRequest.Builder(AdAnalysisWorker.class)
                .addTag(TAG_PREFIX + item.getId())
                .setConstraints(constraints)
                .setInputData(input);
        if (!TextUtils.isEmpty(item.getTitle())) {
            String encodedTitle = Base64.encodeToString(item.getTitle().getBytes(StandardCharsets.UTF_8),
                    Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            requestBuilder.addTag(TITLE_TAG_PREFIX + encodedTitle);
        }
        OneTimeWorkRequest request = requestBuilder.build();

        // APPEND_OR_REPLACE preserves serial queueing but does not attach new
        // retries to an old failed/cancelled chain where they would never run.
        WorkManager.getInstance(context).enqueueUniqueWork(
                QUEUE_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request);
        return true;
    }
}
