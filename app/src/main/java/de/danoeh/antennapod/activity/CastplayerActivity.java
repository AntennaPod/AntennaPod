package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.concurrent.atomic.AtomicBoolean;

import de.danoeh.antennapod.core.cast.CastManager;
import de.danoeh.antennapod.core.service.playback.PlaybackService;

/**
 * Activity for controlling the remote playback on a Cast device.
 */
public class CastplayerActivity extends MediaplayerInfoActivity {
    public static final String TAG = "CastPlayerActivity";

    private AtomicBoolean isSetup = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!CastManager.getInstance().isConnected()) {
            Intent intent = PlaybackService.getPlayerActivityIntent(this);
            if (!intent.getComponent().getClassName().equals(CastplayerActivity.class.getName())) {
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onReloadNotification(int notificationCode) {
        if (notificationCode == PlaybackService.EXTRA_CODE_AUDIO) {
            Log.d(TAG, "ReloadNotification received, switching to Audioplayer now");
            finish();
            startActivity(new Intent(this, AudioplayerActivity.class));
        } else {
            super.onReloadNotification(notificationCode);
        }
    }

    @Override
    protected void setupGUI() {
        if(isSetup.getAndSet(true)) {
            return;
        }
        super.setupGUI();
        if (butPlaybackSpeed != null) {
            butPlaybackSpeed.setVisibility(View.GONE);
        }
        if (butCastDisconnect != null) {
            butCastDisconnect.setOnClickListener(v -> castManager.disconnect());
            butCastDisconnect.setVisibility(View.VISIBLE);
        }
    }
}
