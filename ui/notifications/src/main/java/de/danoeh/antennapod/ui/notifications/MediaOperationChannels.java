package de.danoeh.antennapod.ui.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import de.danoeh.antennapod.ui.i18n.R;

public class MediaOperationChannels {
    public static final String MIGRATION_CHANNEL_ID = "media_migration";
    public static final String RELOCATION_CHANNEL_ID = "media_relocation";

    public static void createMigrationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel(
                    MIGRATION_CHANNEL_ID, context.getString(R.string.migration_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.migration_channel_description));
            manager.createNotificationChannel(channel);
        }
    }

    public static void createRelocationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel(
                    RELOCATION_CHANNEL_ID, context.getString(R.string.relocation_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(context.getString(R.string.relocation_channel_description));
            manager.createNotificationChannel(channel);
        }
    }
}
