package de.danoeh.antennapod.core.util.gui;


import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;

public class NotificationUtils {
    public static final String CHANNEL_ID_USER_ACTION = "user_action";
    public static final String CHANNEL_ID_DOWNLOADING = "downloading";
    public static final String CHANNEL_ID_PLAYING = "playing";
    public static final String CHANNEL_ID_DOWNLOAD_ERROR = "error";
    public static final String CHANNEL_ID_SYNC_ERROR = "sync_error";
    public static final String CHANNEL_ID_AUTO_DOWNLOAD = "auto_download";
    public static final String CHANNEL_ID_EPISODE_NOTIFICATIONS = "episode_notifications";

    public static final String GROUP_ID_ERRORS = "group_errors";
    public static final String GROUP_ID_NEWS = "group_news";

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (mNotificationManager != null) {
            mNotificationManager.createNotificationChannelGroup(createGroupErrors(context));
            mNotificationManager.createNotificationChannelGroup(createGroupNews(context));

            mNotificationManager.createNotificationChannel(createChannelUserAction(context));
            mNotificationManager.createNotificationChannel(createChannelDownloading(context));
            mNotificationManager.createNotificationChannel(createChannelPlaying(context));
            mNotificationManager.createNotificationChannel(createChannelError(context));
            mNotificationManager.createNotificationChannel(createChannelSyncError(context));
            mNotificationManager.createNotificationChannel(createChannelAutoDownload(context));
            mNotificationManager.createNotificationChannel(createChannelEpisodeNotification(context));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createChannelUserAction(Context c) {
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID_USER_ACTION,
                c.getString(R.string.notification_channel_user_action), NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.setDescription(c.getString(R.string.notification_channel_user_action_description));
        notificationChannel.setGroup(GROUP_ID_ERRORS);
        return notificationChannel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createChannelDownloading(Context c) {
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID_DOWNLOADING,
                c.getString(R.string.notification_channel_downloading), NotificationManager.IMPORTANCE_LOW);
        notificationChannel.setDescription(c.getString(R.string.notification_channel_downloading_description));
        notificationChannel.setShowBadge(false);
        return notificationChannel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createChannelPlaying(Context c) {
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID_PLAYING,
                c.getString(R.string.notification_channel_playing), NotificationManager.IMPORTANCE_LOW);
        notificationChannel.setDescription(c.getString(R.string.notification_channel_playing_description));
        notificationChannel.setShowBadge(false);
        return notificationChannel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createChannelError(Context c) {
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID_DOWNLOAD_ERROR,
                c.getString(R.string.notification_channel_download_error), NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.setDescription(c.getString(R.string.notification_channel_download_error_description));
        notificationChannel.setGroup(GROUP_ID_ERRORS);

        if (!UserPreferences.getShowDownloadReportRaw()) {
            // Migration from app managed setting: disable notification
            notificationChannel.setImportance(NotificationManager.IMPORTANCE_NONE);
        }
        return notificationChannel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createChannelSyncError(Context c) {
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID_SYNC_ERROR,
                c.getString(R.string.notification_channel_sync_error), NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.setDescription(c.getString(R.string.notification_channel_sync_error_description));
        notificationChannel.setGroup(GROUP_ID_ERRORS);

        if (!UserPreferences.getGpodnetNotificationsEnabledRaw()) {
            // Migration from app managed setting: disable notification
            notificationChannel.setImportance(NotificationManager.IMPORTANCE_NONE);
        }
        return notificationChannel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createChannelAutoDownload(Context c) {
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID_AUTO_DOWNLOAD,
                c.getString(R.string.notification_channel_auto_download), NotificationManager.IMPORTANCE_NONE);
        notificationChannel.setDescription(c.getString(R.string.notification_channel_episode_auto_download));
        notificationChannel.setGroup(GROUP_ID_NEWS);

        if (UserPreferences.getShowAutoDownloadReportRaw()) {
            // Migration from app managed setting: enable notification
            notificationChannel.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
        }
        return notificationChannel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createChannelEpisodeNotification(Context c) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID_EPISODE_NOTIFICATIONS,
                c.getString(R.string.notification_channel_new_episode), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(c.getString(R.string.notification_channel_new_episode_description));
        channel.setGroup(GROUP_ID_NEWS);
        return channel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannelGroup createGroupErrors(Context c) {
        return new NotificationChannelGroup(GROUP_ID_ERRORS,
                c.getString(R.string.notification_group_errors));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannelGroup createGroupNews(Context c) {
        return new NotificationChannelGroup(GROUP_ID_NEWS,
                c.getString(R.string.notification_group_news));
    }
}
