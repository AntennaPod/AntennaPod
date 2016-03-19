package de.danoeh.antennapod.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;

/**
 * Activity that allows for showing the MediaRouter button whenever there's a cast device in the
 * network.
 */
public abstract class CastEnabledActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    protected VideoCastManager mCastManager;
    private int castUICounter;
    protected MenuItem mMediaRouteMenuItem;
    protected boolean isCastEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.getDefaultSharedPreferences(this).
                registerOnSharedPreferenceChangeListener(this);

        castUICounter = 0;
        mCastManager = VideoCastManager.getInstance();
        isCastEnabled = UserPreferences.isCastEnabled();
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
        mMediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        mMediaRouteMenuItem.setEnabled(isCastEnabled);
        mMediaRouteMenuItem = mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
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

    //This whole method might just be useless because it's assumed that the cast button
    //won't show where the user actually has the power to change the preference.
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(UserPreferences.PREF_CAST_ENABLED)) {
            isCastEnabled = UserPreferences.isCastEnabled();
            mMediaRouteMenuItem.setEnabled(isCastEnabled);
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
