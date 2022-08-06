package de.danoeh.antennapod.core.service;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.view.KeyEvent;

import androidx.annotation.RequiresApi;

import de.danoeh.antennapod.core.receiver.MediaButtonReceiver;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QSTileService extends TileService {

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        Intent intent = new Intent(this, MediaButtonReceiver.class);
        intent.setAction(MediaButtonReceiver.NOTIFY_BUTTON_RECEIVER);
        intent.putExtra(Intent.EXTRA_KEY_EVENT,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        sendBroadcast(intent);
    }

    // TODO call TileService.requestListeningState() elsewhere whenever playback status changes
    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    // Update tile status at boot
    @Override
    public IBinder onBind(Intent intent) {
        TileService.requestListeningState(this,
                new ComponentName(this, QSTileService.class));
        return super.onBind(intent);
    }

    public void updateTile() {
        boolean isPlaying = true; // TODO replace with best way to retrieve playback status
        Tile qsTile = getQsTile();
        qsTile.setState(isPlaying ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        qsTile.updateTile();
    }
}