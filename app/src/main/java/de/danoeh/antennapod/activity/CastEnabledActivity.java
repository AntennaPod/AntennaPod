package de.danoeh.antennapod.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.cast.CastManager;
import de.danoeh.antennapod.core.cast.SwitchableMediaRouteActionProvider;
import de.danoeh.antennapod.core.preferences.UserPreferences;

/**
 * Activity that allows for showing the MediaRouter button whenever there's a cast device in the
 * network.
 */
public abstract class CastEnabledActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = "CastEnabledActivity";

    protected CastManager mCastManager;
    private volatile int castUICounter;
    protected SwitchableMediaRouteActionProvider mMediaRouteActionProvider;
    protected volatile boolean isCastEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
                registerOnSharedPreferenceChangeListener(this);

        castUICounter = 0;
        mCastManager = CastManager.getInstance();
        isCastEnabled = UserPreferences.isCastEnabled();
    }

    @Override
    protected void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .unregisterOnSharedPreferenceChangeListener(this);
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
        mMediaRouteActionProvider = mCastManager
                .addMediaRouterButton(menu.findItem(R.id.media_route_menu_item));
        mMediaRouteActionProvider.setEnabled(isCastEnabled);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        castUICounter++;
        if (isCastEnabled) {
            mCastManager.incrementUiCounter();
            castUICounter++;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        castUICounter--;
        if (isCastEnabled) {
            mCastManager.decrementUiCounter();
            castUICounter--;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(UserPreferences.PREF_CAST_ENABLED)) {
            isCastEnabled = UserPreferences.isCastEnabled();
            Log.d(TAG, "onSharedPreferenceChanged(), isCastEnabled set to " + isCastEnabled);
            mMediaRouteActionProvider.setEnabled(isCastEnabled);
            if (isCastEnabled) {
                //Test if activity is resumed but without UI counter incremented
                if (castUICounter==1) {
                    mCastManager.incrementUiCounter();
                    castUICounter++;
                }
            } else {
                if (castUICounter > 1) {
                    mCastManager.decrementUiCounter();
                    castUICounter--;
                }
                //TODO disable any current casting (or possibly do it within the PlaybackService)
            }
        }
    }
}
