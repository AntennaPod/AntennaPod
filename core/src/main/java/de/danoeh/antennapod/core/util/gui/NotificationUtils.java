package de.danoeh.antennapod.core.util.gui;

import android.content.Context;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationChannelGroupCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public class NotificationUtils {
    public static final String CHANNEL_ID_USER_ACTION = "user_action";
    public static final String CHANNEL_ID_DOWNLOADING = "downloading";
    public static final String CHANNEL_ID_PLAYING = "playing";
    public static final String CHANNEL_ID_DOWNLOAD_ERROR = "error";
    public static final String CHANNEL_ID_SYNC_ERROR = "sync_error";
    public static final String CHANNEL_ID_EPISODE_NOTIFICATIONS = "episode_notifications";

    public static final String GROUP_ID_ERRORS = "group_errors";
    public static final String GROUP_ID_NEWS = "group_news";

    public static void createChannels(final Context context) {
        final NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(context);

        final List<NotificationChannelGroupCompat> channelGroups = Arrays.asList(
                createGroupErrors(context),
                createGroupNews(context));
        mNotificationManager.createNotificationChannelGroupsCompat(channelGroups);

        final List<NotificationChannelCompat> channels = Arrays.asList(
                createChannelUserAction(context),
                createChannelDownloading(context),
                createChannelPlaying(context),
                createChannelError(context),
                createChannelSyncError(context),
                createChannelEpisodeNotification(context));
        mNotificationManager.createNotificationChannelsCompat(channels);
    }

    private static NotificationChannelCompat createChannelUserAction(final Context c) {
        return new NotificationChannelCompat.Builder(
                        CHANNEL_ID_USER_ACTION, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(c.getString(R.string.notification_channel_user_action))
                .setDescription(c.getString(R.string.notification_channel_user_action_description))
                .setGroup(GROUP_ID_ERRORS)
                .build();
    }

    private static NotificationChannelCompat createChannelDownloading(final Context c) {
        return new NotificationChannelCompat.Builder(
                        CHANNEL_ID_DOWNLOADING, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(c.getString(R.string.notification_channel_downloading))
                .setDescription(c.getString(R.string.notification_channel_downloading_description))
                .setShowBadge(false)
                .build();
    }

    private static NotificationChannelCompat createChannelPlaying(final Context c) {
        return new NotificationChannelCompat.Builder(
                        CHANNEL_ID_PLAYING, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(c.getString(R.string.notification_channel_playing))
                .setDescription(c.getString(R.string.notification_channel_playing_description))
                .setShowBadge(false)
                .build();
    }

    private static NotificationChannelCompat createChannelError(final Context c) {
        final NotificationChannelCompat.Builder notificationChannel = new NotificationChannelCompat.Builder(
                        CHANNEL_ID_DOWNLOAD_ERROR, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(c.getString(R.string.notification_channel_download_error))
                .setDescription(c.getString(R.string.notification_channel_download_error_description))
                .setGroup(GROUP_ID_ERRORS);

        if (!UserPreferences.getShowDownloadReportRaw()) {
            // Migration from app managed setting: disable notification
            notificationChannel.setImportance(NotificationManagerCompat.IMPORTANCE_NONE);
        }
        return notificationChannel.build();
    }

    private static NotificationChannelCompat createChannelSyncError(final Context c) {
        final NotificationChannelCompat.Builder notificationChannel = new NotificationChannelCompat.Builder(
                        CHANNEL_ID_SYNC_ERROR, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(c.getString(R.string.notification_channel_sync_error))
                .setDescription(c.getString(R.string.notification_channel_sync_error_description))
                .setGroup(GROUP_ID_ERRORS);

        if (!UserPreferences.getGpodnetNotificationsEnabledRaw()) {
            // Migration from app managed setting: disable notification
            notificationChannel.setImportance(NotificationManagerCompat.IMPORTANCE_NONE);
        }
        return notificationChannel.build();
    }

    private static NotificationChannelCompat createChannelEpisodeNotification(final Context c) {
        return new NotificationChannelCompat.Builder(
                        CHANNEL_ID_EPISODE_NOTIFICATIONS, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(c.getString(R.string.notification_channel_new_episode))
                .setDescription(c.getString(R.string.notification_channel_new_episode_description))
                .setGroup(GROUP_ID_NEWS)
                .build();
    }

    private static NotificationChannelGroupCompat createGroupErrors(final Context c) {
        return new NotificationChannelGroupCompat.Builder(GROUP_ID_ERRORS)
                .setName(c.getString(R.string.notification_group_errors))
                .build();
    }

    private static NotificationChannelGroupCompat createGroupNews(final Context c) {
        return new NotificationChannelGroupCompat.Builder(GROUP_ID_NEWS)
                .setName(c.getString(R.string.notification_group_news))
                .build();
    }
}
