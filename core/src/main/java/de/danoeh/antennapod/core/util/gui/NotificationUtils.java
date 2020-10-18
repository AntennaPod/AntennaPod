package de.danoeh.antennapod.core.util.gui;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;

import de.danoeh.antennapod.core.R;

public class NotificationUtils {
    public static final String CHANNEL_ID_USER_ACTION = "user_action";
    public static final String CHANNEL_ID_DOWNLOADING = "downloading";
    public static final String CHANNEL_ID_PLAYING = "playing";
    public static final String CHANNEL_ID_ERROR = "error";
    public static final String CHANNEL_ID_SYNC_ERROR = "sync_error";
    public static final String CHANNEL_ID_AUTO_DOWNLOAD = "auto_download";
    public static final String CHANNEL_ID_EPISODE_NOTIFY = "episode_notify";

    public static void createChannels(Context context) {
        if (android.os.Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (mNotificationManager != null) {
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
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID_USER_ACTION,
                c.getString(R.string.notification_channel_user_action), NotificationManager.IMPORTANCE_HIGH);
        mChannel.setDescription(c.getString(R.string.notification_channel_user_action_description));
        return mChannel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createChannelDownloading(Context c) {
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID_DOWNLOADING,
                c.getString(R.string.notification_channel_downloading), NotificationManager.IMPORTANCE_LOW);
        mChannel.setDescription(c.getString(R.string.notification_channel_downloading_description));
        mChannel.setShowBadge(false);
        return mChannel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createChannelPlaying(Context c) {
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID_PLAYING,
                c.getString(R.string.notification_channel_playing), NotificationManager.IMPORTANCE_LOW);
        mChannel.setDescription(c.getString(R.string.notification_channel_playing_description));
        mChannel.setShowBadge(false);
        return mChannel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createChannelError(Context c) {
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID_ERROR,
                c.getString(R.string.notification_channel_error), NotificationManager.IMPORTANCE_HIGH);
        mChannel.setDescription(c.getString(R.string.notification_channel_error_description));
        return mChannel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createChannelSyncError(Context c) {
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID_SYNC_ERROR,
                c.getString(R.string.notification_channel_sync_error), NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.setDescription(c.getString(R.string.notification_channel_sync_error_description));
        return notificationChannel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createChannelAutoDownload(Context c) {
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID_AUTO_DOWNLOAD,
                c.getString(R.string.notification_channel_auto_download), NotificationManager.IMPORTANCE_DEFAULT);
        mChannel.setDescription(c.getString(R.string.notification_channel_episode_auto_download));
        return mChannel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createChannelEpisodeNotification(Context c) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID_EPISODE_NOTIFY,
                c.getString(R.string.notification_channel_new_episode), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(c.getString(R.string.notification_channel_new_episode_description));
        return channel;
    }
}
