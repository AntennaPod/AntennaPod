package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import de.danoeh.antennapod.core.service.playback.PlaybackService;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Activity for controlling the remote playback on a Cast device.
 */
public class CastplayerActivity extends MediaplayerInfoActivity {
    private static final String TAG = "CastPlayerActivity";

    private final AtomicBoolean isSetup = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PlaybackService.isCasting()) {
            Intent intent = PlaybackService.getPlayerActivityIntent(this);
            if (!intent.getComponent().getClassName().equals(CastplayerActivity.class.getName())) {
                finish();
                startActivity(intent);
            }
        }
    }

    @Override
    protected void setupGUI() {
        if (isSetup.getAndSet(true)) {
            return;
        }
        super.setupGUI();
        if (butPlaybackSpeed != null) {
            butPlaybackSpeed.setVisibility(View.GONE);
            txtvPlaybackSpeed.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        if (!PlaybackService.isCasting()) {
            Intent intent = PlaybackService.getPlayerActivityIntent(this);
            if (!intent.getComponent().getClassName().equals(CastplayerActivity.class.getName())) {
                saveCurrentFragment();
                finish();
                startActivity(intent);
            }
        }
        super.onResume();
    }

    @Override
    protected void onBufferStart() {
        //sbPosition.setIndeterminate(true);
        sbPosition.setEnabled(false);
    }

    @Override
    protected void onBufferEnd() {
        //sbPosition.setIndeterminate(false);
        sbPosition.setEnabled(true);
    }
}
