package de.danoeh.antennapod.core.service;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.core.util.download.AutoUpdateManager;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;

public class FeedUpdateWorker extends Worker {

    private static final String TAG = "FeedUpdateWorker";

    public static final String PARAM_RUN_ONCE = "runOnce";

    public FeedUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        final boolean isRunOnce = getInputData().getBoolean(PARAM_RUN_ONCE, false);
        Log.d(TAG, "doWork() : isRunOnce = " + isRunOnce);
        ClientConfig.initialize(getApplicationContext());

        if (NetworkUtils.networkAvailable() && NetworkUtils.isFeedRefreshAllowed()) {
            List<Feed> feeds = new ArrayList<>();
            Map<Long, FeedItem> lastItemsMap = new HashMap<>();

            for (Feed feed : DBReader.getFeedList()) {
                FeedPreferences prefs = feed.getPreferences();
                if (prefs.getKeepUpdated() && prefs.getShowNotification()) {
                    List<FeedItem> outdatedFeedItems = DBReader.getFeedItemList(feed);
                    if (!outdatedFeedItems.isEmpty()) {
                        FeedItem newestEpisode = outdatedFeedItems.get(0);

                        lastItemsMap.put(feed.getId(), newestEpisode);
                    }
                    feeds.add(feed);
                }
            }

            boolean refreshed = DBTasks.refreshAllFeeds(getApplicationContext(), false);

            if (refreshed) {
                for (Feed feed : feeds) {
                    List<FeedItem> feedItems = DBReader.getFeedItemList(feed);

                    int newEpisodes;
                    if (lastItemsMap.containsKey(feed.getId())) {
                        FeedItem lastKnownFeedItems = lastItemsMap.get(feed.getId());

                        newEpisodes = feedItems.indexOf(lastKnownFeedItems);
                    } else {
                        newEpisodes = feedItems.size();
                    }

                    if (newEpisodes > 0) {
                        showNotification(newEpisodes, feed);
                    }
                }
            }
        } else {
            Log.d(TAG, "Blocking automatic update: no wifi available / no mobile updates allowed");
        }

        if (!isRunOnce && UserPreferences.isAutoUpdateTimeOfDay()) {
            // WorkManager does not allow to set specific time for repeated tasks.
            // We repeatedly schedule a OneTimeWorkRequest instead.
            AutoUpdateManager.restartUpdateAlarm(getApplicationContext());
        }

        return Result.success();
    }

    private void showNotification(int newEpisodes, Feed feed) {
        Context context = getApplicationContext();

        Resources res = context.getResources();
        String text = res.getQuantityString(R.plurals.new_episode_message, newEpisodes, newEpisodes, feed.getTitle());

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context, "de.danoeh.antennapod.activity.MainActivity"));

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("fragment_feed_id", feed.getId());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID_EPISODE_NOTIFY)
                .setSmallIcon(R.drawable.ic_notification_bell_white)
                .setContentTitle("New Episode")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(R.string.notification_channel_new_episode * feed.hashCode(), builder.build());
    }
}
