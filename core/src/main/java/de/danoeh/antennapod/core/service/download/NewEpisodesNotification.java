package de.danoeh.antennapod.core.service.download;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;

public class NewEpisodesNotification {
    final static int SUMMARY_ID = 0;
    final static String GROUP_KEY = "de.danoeh.antennapod.EPISODES";

    private final Map<Long, Long> lastItemsMap = new HashMap<>();

    private final Context context;

    public NewEpisodesNotification(Context context) {
        this.context = context;

        for (Feed feed : DBReader.getFeedList()) {
            FeedPreferences prefs = feed.getPreferences();
            if (prefs.getKeepUpdated() && prefs.getShowEpisodeNotification()) {
                List<FeedItem> outdatedFeedItems = DBReader.getFeedItemList(feed);

                Long newestEpisodeId = null;
                if (!outdatedFeedItems.isEmpty()) {
                    newestEpisodeId = outdatedFeedItems.get(0).getId();
                }
                lastItemsMap.put(feed.getId(), newestEpisodeId);
            }
        }
    }

    public void showNotifications() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        String text = "";
        int episodeSum = 0;
        int podcastsWithEpisodes = 0;

        for (Long feedId : lastItemsMap.keySet()) {
            Feed feed = DBReader.getFeed(feedId);
            List<FeedItem> feedItems = DBReader.getFeedItemList(feed);

            int newEpisodes;
            if (feedId != null) {
                long lastKnownFeedItemIds = lastItemsMap.get(feedId);
                FeedItem lastKnownFeedItems = DBReader.getFeedItem(lastKnownFeedItemIds);

                newEpisodes = feedItems.indexOf(lastKnownFeedItems);
            } else {
                newEpisodes = feedItems.size();
            }

            if (newEpisodes > 0) {
                text = showNotification(newEpisodes, feed, context, notificationManager);
                style.addLine(text);
                episodeSum += newEpisodes;
                podcastsWithEpisodes++;
            }
        }

        if (episodeSum > 0) {
            Resources res = context.getResources();
            String title = res.getQuantityString(R.plurals.new_episode_notification_title, episodeSum);

            if (podcastsWithEpisodes > 1) {
                text = res.getString(R.string.new_episode_notification_group_text, podcastsWithEpisodes, episodeSum);
            }

            style.setBigContentTitle(title).setSummaryText(title);

            Notification summaryNotification =
                    new NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID_EPISODE_NOTIFICATIONS)
                            .setContentTitle(title)
                            .setContentText(text)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setStyle(style)
                            .setGroup(GROUP_KEY)
                            .setGroupSummary(true)
                            .build();

            notificationManager.notify(NotificationUtils.CHANNEL_ID_EPISODE_NOTIFICATIONS, SUMMARY_ID, summaryNotification);
        }
    }

    static private String showNotification(int newEpisodes, Feed feed, Context context, NotificationManagerCompat notificationManager) {
        Resources res = context.getResources();
        String text = res.getQuantityString(R.plurals.new_episode_notification_message, newEpisodes, newEpisodes, feed.getTitle());
        String title = res.getQuantityString(R.plurals.new_episode_notification_title, newEpisodes);

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context, "de.danoeh.antennapod.activity.MainActivity"));

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("fragment_feed_id", feed.getId());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID_EPISODE_NOTIFICATIONS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setGroup(GROUP_KEY)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(NotificationUtils.CHANNEL_ID_EPISODE_NOTIFICATIONS, feed.hashCode(), notification);

        return text;
    }
}
