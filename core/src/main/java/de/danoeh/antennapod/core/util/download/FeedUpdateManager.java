package de.danoeh.antennapod.core.util.download;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.TimeUnit;

public class FeedUpdateManager {
    public static final String WORK_TAG_FEED_UPDATE = "feedUpdate";
    private static final String WORK_ID_FEED_UPDATE = "de.danoeh.antennapod.core.service.FeedUpdateWorker";
    private static final String WORK_ID_FEED_UPDATE_MANUAL = "feedUpdateManual";
    public static final String EXTRA_FEED_ID = "feed_id";
    public static final String EXTRA_NEXT_PAGE = "next_page";
    public static final String EXTRA_EVEN_ON_MOBILE = "even_on_mobile";
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
                    .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(UserPreferences.isAllowMobileFeedRefresh()
                            ? NetworkType.CONNECTED : NetworkType.UNMETERED).build())
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
        if (feed == null || !feed.isLocalFeed()) {
            workRequest.setConstraints(new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED).build());
        }
        Data.Builder builder = new Data.Builder();
        builder.putBoolean(EXTRA_EVEN_ON_MOBILE, true);
        if (feed != null) {
            builder.putLong(EXTRA_FEED_ID, feed.getId());
            builder.putBoolean(EXTRA_NEXT_PAGE, nextPage);
        }
        workRequest.setInputData(builder.build());
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_ID_FEED_UPDATE_MANUAL,
                ExistingWorkPolicy.REPLACE, workRequest.build());
    }

    public static void runOnceOrAsk(@NonNull Context context) {
        runOnceOrAsk(context, null);
    }

    public static void runOnceOrAsk(@NonNull Context context, @Nullable Feed feed) {
        Log.d(TAG, "Run auto update immediately in background.");
        if (feed != null && feed.isLocalFeed()) {
            runOnce(context, feed);
        } else if (!NetworkUtils.networkAvailable()) {
            EventBus.getDefault().post(new MessageEvent(context.getString(R.string.download_error_no_connection)));
        } else if (NetworkUtils.isFeedRefreshAllowed()) {
            runOnce(context, feed);
        } else {
            confirmMobileRefresh(context, feed);
        }
    }

    private static void confirmMobileRefresh(final Context context, @Nullable Feed feed) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.feed_refresh_title)
                .setPositiveButton(R.string.confirm_mobile_streaming_button_once,
                        (dialog, which) -> runOnce(context, feed))
                .setNeutralButton(R.string.confirm_mobile_streaming_button_always, (dialog, which) -> {
                    UserPreferences.setAllowMobileFeedRefresh(true);
                    runOnce(context, feed);
                })
                .setNegativeButton(R.string.no, null);
        if (NetworkUtils.isNetworkRestricted() && NetworkUtils.isVpnOverWifi()) {
            builder.setMessage(R.string.confirm_mobile_feed_refresh_dialog_message_vpn);
        } else {
            builder.setMessage(R.string.confirm_mobile_feed_refresh_dialog_message);
        }
        builder.show();
    }
}
