package de.danoeh.antennapod.core.service.download;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;

public class NewEpisodesNotification {
    static final String GROUP_KEY = "de.danoeh.antennapod.EPISODES";

    private final Long feedId;
    private final Long lastEpisodeID;
    private final boolean dontShowNotification;

    public NewEpisodesNotification(Long feedId) {
        Feed feed = DBReader.getFeed(feedId);

        FeedPreferences prefs = feed.getPreferences();
        if (!prefs.getKeepUpdated() || !prefs.getShowEpisodeNotification()) {
            dontShowNotification = true;

            this.feedId = 0L;
            lastEpisodeID = null;

            return;
        }

        List<FeedItem> outdatedFeedItems = DBReader.getFeedItemList(feed);

        Long newestEpisodeId = null;
        if (!outdatedFeedItems.isEmpty()) {
            newestEpisodeId = outdatedFeedItems.get(0).getId();
        }

        dontShowNotification = false;
        this.feedId = feedId;

        // newestEpisodeId is null if the feed does not have an an episode yet
        lastEpisodeID = newestEpisodeId;
    }

    public void showNotification(Context context) {
        if (dontShowNotification) {
            return;
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        Feed feed = DBReader.getFeed(feedId);
        List<FeedItem> feedItems = DBReader.getFeedItemList(feed);

        int newEpisodes;
        if (lastEpisodeID != null) { // the feed does not have an an episode yet
            FeedItem lastKnownFeedItems = DBReader.getFeedItem(lastEpisodeID);

            newEpisodes = feedItems.indexOf(lastKnownFeedItems);
        } else {
            newEpisodes = feedItems.size();
        }

        if (newEpisodes > 0) {
            showNotification(newEpisodes, feed, context, notificationManager);
        }
    }

    private static void showNotification(int newEpisodes, Feed feed, Context context,
                                         NotificationManagerCompat notificationManager) {
        Resources res = context.getResources();
        String text = res.getQuantityString(
                R.plurals.new_episode_notification_message, newEpisodes, newEpisodes, feed.getTitle()
        );
        String title = res.getQuantityString(R.plurals.new_episode_notification_title, newEpisodes);

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context, "de.danoeh.antennapod.activity.MainActivity"));

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("fragment_feed_id", feed.getId());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(
                context, NotificationUtils.CHANNEL_ID_EPISODE_NOTIFICATIONS
        )
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setGroup(GROUP_KEY)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(NotificationUtils.CHANNEL_ID_EPISODE_NOTIFICATIONS, feed.hashCode(), notification);
    }
}
