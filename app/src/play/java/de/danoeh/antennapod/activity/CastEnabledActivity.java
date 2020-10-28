package de.danoeh.antennapod.activity;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.ApplicationMetadata;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.cast.CastButtonVisibilityManager;
import de.danoeh.antennapod.core.cast.CastConsumer;
import de.danoeh.antennapod.core.cast.CastManager;
import de.danoeh.antennapod.core.cast.DefaultCastConsumer;
import de.danoeh.antennapod.core.cast.SwitchableMediaRouteActionProvider;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that allows for showing the MediaRouter button whenever there's a cast device in the
 * network.
 */
public abstract class CastEnabledActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = "CastEnabledActivity";

    private CastConsumer castConsumer;
    private CastManager castManager;
    private final List<CastButtonVisibilityManager> castButtons = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!CastManager.isInitialized()) {
            return;
        }

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .registerOnSharedPreferenceChangeListener(this);

        castConsumer = new DefaultCastConsumer() {
            @Override
            public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId, boolean wasLaunched) {
                onCastConnectionChanged(true);
            }

            @Override
            public void onDisconnected() {
                onCastConnectionChanged(false);
            }
        };
        castManager = CastManager.getInstance();
        castManager.addCastConsumer(castConsumer);
        CastButtonVisibilityManager castButtonVisibilityManager = new CastButtonVisibilityManager(castManager);
        castButtonVisibilityManager.setPrefEnabled(UserPreferences.isCastEnabled());
        onCastConnectionChanged(castManager.isConnected());
        castButtons.add(castButtonVisibilityManager);
    }

    @Override
    protected void onDestroy() {
        if (!CastManager.isInitialized()) {
            super.onDestroy();
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .unregisterOnSharedPreferenceChangeListener(this);
        castManager.removeCastConsumer(castConsumer);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!CastManager.isInitialized()) {
            return;
        }
        for (CastButtonVisibilityManager castButton : castButtons) {
            castButton.setResumed(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!CastManager.isInitialized()) {
            return;
        }
        for (CastButtonVisibilityManager castButton : castButtons) {
            castButton.setResumed(false);
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (UserPreferences.PREF_CAST_ENABLED.equals(key)) {
            boolean newValue = UserPreferences.isCastEnabled();
            Log.d(TAG, "onSharedPreferenceChanged(), isCastEnabled set to " + newValue);
            for (CastButtonVisibilityManager castButton : castButtons) {
                castButton.setPrefEnabled(newValue);
            }
            // PlaybackService has its own listener, so if it's active we don't have to take action here.
            if (!newValue && !PlaybackService.isRunning) {
                CastManager.getInstance().disconnect();
            }
        }
    }

    private void onCastConnectionChanged(boolean connected) {
        if (connected) {
            for (CastButtonVisibilityManager castButton : castButtons) {
                castButton.onConnected();
            }
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
        } else {
            for (CastButtonVisibilityManager castButton : castButtons) {
                castButton.onDisconnected();
            }
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }
    }

    /**
     * Should be called by any activity or fragment for which the cast button should be shown.
     */
    public final void requestCastButton(Menu menu) {
        if (!CastManager.isInitialized()) {
            return;
        }

        MenuItem mediaRouteButton = menu.findItem(R.id.media_route_menu_item);
        if (mediaRouteButton == null) {
            getMenuInflater().inflate(R.menu.cast_enabled, menu);
            mediaRouteButton = menu.findItem(R.id.media_route_menu_item);
        }

        SwitchableMediaRouteActionProvider mediaRouteActionProvider =
                CastManager.getInstance().addMediaRouterButton(mediaRouteButton);
        CastButtonVisibilityManager castButtonVisibilityManager =
                new CastButtonVisibilityManager(CastManager.getInstance());
        castButtonVisibilityManager.setMenu(menu);
        castButtonVisibilityManager.setPrefEnabled(UserPreferences.isCastEnabled());
        castButtonVisibilityManager.mediaRouteActionProvider = mediaRouteActionProvider;
        castButtonVisibilityManager.setResumed(true);
        castButtonVisibilityManager.requestCastButton(MenuItem.SHOW_AS_ACTION_ALWAYS);
        mediaRouteActionProvider.setEnabled(castButtonVisibilityManager.shouldEnable());
    }
}
