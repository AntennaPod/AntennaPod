package de.danoeh.antennapod.playback.service.internal;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.playback.service.PlaybackServiceStarter;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

/**
 * Resumes playback when a Bluetooth/Android Auto connection is established,
 * if the user enabled the corresponding preference and playback was paused.
 */
public class BluetoothReconnectReceiver extends BroadcastReceiver {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        if (!UserPreferences.isUnpauseOnBluetoothReconnect()) {
            return;
        }
        if (!isRelevantConnection(intent)) {
            return;
        }
        if (!hasBluetoothPermission(context)) {
            return;
        }
        if (PlaybackPreferences.getCurrentPlayerStatus() != PlaybackPreferences.PLAYER_STATUS_PAUSED) {
            return;
        }
        long mediaId = PlaybackPreferences.getCurrentlyPlayingFeedMediaId();
        if (mediaId == PlaybackPreferences.NO_MEDIA_PLAYING) {
            return;
        }

        PendingResult pendingResult = goAsync();
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                FeedMedia media = DBReader.getFeedMedia(mediaId);
                if (media != null) {
                    new PlaybackServiceStarter(appContext, media)
                            .shouldStreamThisTime(!media.localFileAvailable())
                            .callEvenIfRunning(true)
                            .start();
                }
            } finally {
                pendingResult.finish();
            }
        });
    }

    private boolean isRelevantConnection(@NonNull Intent intent) {
        String action = intent.getAction();
        if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR);
            return state == BluetoothAdapter.STATE_CONNECTED;
        }
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            return true;
        }
        return false;
    }

    private boolean hasBluetoothPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }
}
