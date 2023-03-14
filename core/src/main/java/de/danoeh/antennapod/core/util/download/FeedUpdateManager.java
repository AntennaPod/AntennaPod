package de.danoeh.antennapod.core.util.download;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.service.FeedUpdateWorker;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import java.util.concurrent.TimeUnit;

public class FeedUpdateManager {
    public static final String WORK_TAG_FEED_UPDATE = "feedUpdate";
    private static final String WORK_ID_FEED_UPDATE = "de.danoeh.antennapod.core.service.FeedUpdateWorker";
    private static final String WORK_ID_FEED_UPDATE_MANUAL = "feedUpdateManual";
    public static final String EXTRA_FEED_ID = "feed_id";
    public static final String EXTRA_NEXT_PAGE = "next_page";
    private static final String TAG = "AutoUpdateManager";

    private FeedUpdateManager() {

    }

    /**
     * Start / restart periodic auto feed refresh
     * @param context Context
     */
    public static void restartUpdateAlarm(Context context, boolean replace) {
        if (UserPreferences.isAutoUpdateDisabled()) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_ID_FEED_UPDATE);
        } else {
            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                    FeedUpdateWorker.class, UserPreferences.getUpdateInterval(), TimeUnit.HOURS)
                    .setConstraints(getConstraints())
                    .build();
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_ID_FEED_UPDATE,
                    replace ? ExistingPeriodicWorkPolicy.REPLACE : ExistingPeriodicWorkPolicy.KEEP, workRequest);
        }
    }

    public static void runOnce(Context context) {
        runOnce(context, null, false);
    }

    public static void runOnce(Context context, Feed feed) {
        runOnce(context, feed, false);
    }

    public static void runOnce(Context context, Feed feed, boolean nextPage) {
        OneTimeWorkRequest.Builder workRequest = new OneTimeWorkRequest.Builder(FeedUpdateWorker.class)
                .setInitialDelay(0L, TimeUnit.MILLISECONDS)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(WORK_TAG_FEED_UPDATE);
        if (feed != null) {
            Data.Builder builder = new Data.Builder();
            builder.putLong(EXTRA_FEED_ID, feed.getId());
            builder.putBoolean(EXTRA_NEXT_PAGE, nextPage);
            workRequest.setInputData(builder.build());
        }
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_ID_FEED_UPDATE_MANUAL,
                ExistingWorkPolicy.REPLACE, workRequest.build());
    }

    public static void runOnceOrAsk(@NonNull Context context) {
        Log.d(TAG, "Run auto update immediately in background.");
        if (!NetworkUtils.networkAvailable()) {
            Log.d(TAG, "Ignoring: No network connection.");
        } else if (NetworkUtils.isFeedRefreshAllowed()) {
            runOnce(context);
        } else {
            confirmMobileAllFeedsRefresh(context);
        }
    }

    private static void confirmMobileAllFeedsRefresh(final Context context) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.feed_refresh_title)
                .setMessage(R.string.confirm_mobile_feed_refresh_dialog_message)
                .setPositiveButton(R.string.confirm_mobile_streaming_button_once,
                        (dialog, which) -> runOnce(context))
                .setNeutralButton(R.string.confirm_mobile_streaming_button_always, (dialog, which) -> {
                    UserPreferences.setAllowMobileFeedRefresh(true);
                    runOnce(context);
                })
                .setNegativeButton(R.string.no, null);
        builder.show();
    }

    private static Constraints getConstraints() {
        Constraints.Builder constraints = new Constraints.Builder();

        if (UserPreferences.isAllowMobileFeedRefresh()) {
            constraints.setRequiredNetworkType(NetworkType.CONNECTED);
        } else {
            constraints.setRequiredNetworkType(NetworkType.UNMETERED);
        }
        return constraints.build();
    }

}
