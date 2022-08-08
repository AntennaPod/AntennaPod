package de.danoeh.antennapod.core.service;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.RequiresApi;

import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.core.service.playback.PlaybackService;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QuickSettingsTileService extends TileService {
    /**
     * Logging tag
     * Missing final 'e' because of max 23 characters for log tags
     */
    private static final String TAG = "QuickSettingsTileServic";

    // Initialize and update status when tile is added
    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile();
    }

    // Play/pause playback by faking a Media Button press when clicking the tile
    @Override
    public void onClick() {
        super.onClick();
        Intent intent = new Intent(this, MediaButtonReceiver.class);
        intent.setAction(MediaButtonReceiver.NOTIFY_BUTTON_RECEIVER);
        intent.putExtra(Intent.EXTRA_KEY_EVENT,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        sendBroadcast(intent);
    }

    // Update the tile status when TileService.requestListeningState() is called elsewhere
    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    // Update tile status on binding
    // (Without this, the tile may not be in the correct state after boot)
    @Override
    public IBinder onBind(Intent intent) {
        TileService.requestListeningState(this,
                new ComponentName(this, QuickSettingsTileService.class));
        return super.onBind(intent);
    }

    // Change the active/inactive state of the tile to match current playback status
    public void updateTile() {
        Tile qsTile = getQsTile();
        if (qsTile == null) {
            Log.d(TAG, "Ignored call to update QS tile: getQsTile() returned null.");
        } else {
            // Get current playback status
            boolean isPlaying = PlaybackService.isRunning
                    && PlaybackPreferences.getCurrentPlayerStatus()
                        == PlaybackPreferences.PLAYER_STATUS_PLAYING;
            // Change the active/inactive state of the tile
            qsTile.setState(isPlaying ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            // Apply the above changes
            qsTile.updateTile();
        }
    }
}