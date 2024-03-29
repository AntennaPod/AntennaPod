package de.danoeh.antennapod.playback.service;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.RequiresApi;

import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.ui.appstartintent.MediaButtonStarter;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QuickSettingsTileService extends TileService {

    private static final String TAG = "QuickSettingsTileSvc";

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        sendBroadcast(MediaButtonStarter.createIntent(this, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
    }

    // Update the tile status when TileService.requestListeningState() is called elsewhere
    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    // Without this, the tile may not be in the correct state after boot
    @Override
    public IBinder onBind(Intent intent) {
        TileService.requestListeningState(this, new ComponentName(this, QuickSettingsTileService.class));
        return super.onBind(intent);
    }

    public void updateTile() {
        Tile qsTile = getQsTile();
        if (qsTile == null) {
            Log.d(TAG, "Ignored call to update QS tile: getQsTile() returned null.");
        } else {
            boolean isPlaying = PlaybackService.isRunning
                    && PlaybackPreferences.getCurrentPlayerStatus()
                        == PlaybackPreferences.PLAYER_STATUS_PLAYING;
            qsTile.setState(isPlaying ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            qsTile.updateTile();
        }
    }
}
