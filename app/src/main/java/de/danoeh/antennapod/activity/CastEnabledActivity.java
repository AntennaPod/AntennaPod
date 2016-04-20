package de.danoeh.antennapod.activity;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;

import com.google.android.gms.cast.ApplicationMetadata;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.cast.CastConsumer;
import de.danoeh.antennapod.core.cast.DefaultCastConsumer;
import de.danoeh.antennapod.core.cast.CastManager;
import de.danoeh.antennapod.core.cast.SwitchableMediaRouteActionProvider;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;

/**
 * Activity that allows for showing the MediaRouter button whenever there's a cast device in the
 * network.
 */
public abstract class CastEnabledActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = "CastEnabledActivity";

    protected CastManager castManager;
    private final Object UI_COUNTER_LOCK = new Object();
    private volatile boolean isResumed = false;
    protected SwitchableMediaRouteActionProvider mediaRouteActionProvider;
    protected volatile boolean isCastEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
                registerOnSharedPreferenceChangeListener(this);

        castManager = CastManager.getInstance();
        castManager.addCastConsumer(castConsumer);
        isCastEnabled = UserPreferences.isCastEnabled();
        onCastConnectionChanged(castManager.isConnected());
    }

    @Override
    protected void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .unregisterOnSharedPreferenceChangeListener(this);
        castManager.removeCastConsumer(castConsumer);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.cast_enabled, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mediaRouteActionProvider = castManager
                .addMediaRouterButton(menu.findItem(R.id.media_route_menu_item));
        mediaRouteActionProvider.setEnabled(isCastEnabled);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        synchronized (UI_COUNTER_LOCK) {
            isResumed = true;
            if (isCastEnabled) {
                castManager.incrementUiCounter();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        synchronized (UI_COUNTER_LOCK) {
            isResumed = false;
            if (isCastEnabled) {
                castManager.decrementUiCounter();
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (UserPreferences.PREF_CAST_ENABLED.equals(key)) {
            boolean newValue = UserPreferences.isCastEnabled();
            Log.d(TAG, "onSharedPreferenceChanged(), isCastEnabled set to " + newValue);
            synchronized (UI_COUNTER_LOCK) {
                if (isCastEnabled != newValue && isResumed) {
                    if (newValue) {
                        castManager.incrementUiCounter();
                    } else {
                        castManager.decrementUiCounter();
                    }
                }
                isCastEnabled = newValue;
            }
            mediaRouteActionProvider.setEnabled(isCastEnabled);
            // PlaybackService has its own listener, so if it's active we don't have to take action here.
            if (!isCastEnabled && !PlaybackService.isRunning) {
                CastManager.getInstance().disconnect();
            }
        }
    }

    CastConsumer castConsumer = new DefaultCastConsumer() {
        @Override
        public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId, boolean wasLaunched) {
            onCastConnectionChanged(true);
        }

        @Override
        public void onDisconnected() {
            onCastConnectionChanged(false);
        }
    };

    private void onCastConnectionChanged(boolean connected) {
        if (connected) {
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
        } else {
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }
    }
}
