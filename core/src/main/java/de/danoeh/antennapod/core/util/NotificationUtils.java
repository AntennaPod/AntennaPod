package de.danoeh.antennapod.core.util;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import de.danoeh.antennapod.core.R;

public class NotificationUtils {
    public static final String CHANNEL_ID_USER_ACTION = "user_action";
    public static final String CHANNEL_ID_DOWNLOADING = "downloading";
    public static final String CHANNEL_ID_PLAYING = "playing";
    public static final String CHANNEL_ID_ERROR = "error";

    public static void createChannelUserAction(Context c) {
        if (android.os.Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager mNotificationManager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID_USER_ACTION,
                c.getString(R.string.notification_channel_user_action), NotificationManager.IMPORTANCE_HIGH);
        mChannel.setDescription(c.getString(R.string.notification_channel_user_action_description));
        mNotificationManager.createNotificationChannel(mChannel);
    }

    public static void createChannelDownloading(Context c) {
        if (android.os.Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager mNotificationManager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID_DOWNLOADING,
                c.getString(R.string.notification_channel_downloading), NotificationManager.IMPORTANCE_DEFAULT);
        mChannel.setDescription(c.getString(R.string.notification_channel_downloading_description));
        mNotificationManager.createNotificationChannel(mChannel);
    }

    public static void createChannelPlaying(Context c) {
        if (android.os.Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager mNotificationManager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID_PLAYING,
                c.getString(R.string.notification_channel_playing), NotificationManager.IMPORTANCE_DEFAULT);
        mChannel.setDescription(c.getString(R.string.notification_channel_playing_description));
        mNotificationManager.createNotificationChannel(mChannel);
    }

    public static void createChannelError(Context c) {
        if (android.os.Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager mNotificationManager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID_ERROR,
                c.getString(R.string.notification_channel_error), NotificationManager.IMPORTANCE_HIGH);
        mChannel.setDescription(c.getString(R.string.notification_channel_error_description));
        mNotificationManager.createNotificationChannel(mChannel);
    }
}
