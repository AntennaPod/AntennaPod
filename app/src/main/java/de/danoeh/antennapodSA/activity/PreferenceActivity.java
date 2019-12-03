package de.danoeh.antennapodSA.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import com.bytehamster.lib.preferencesearch.SearchPreferenceResult;
import com.bytehamster.lib.preferencesearch.SearchPreferenceResultListener;

import de.danoeh.antennapodSA.R;
import de.danoeh.antennapodSA.core.preferences.UserPreferences;
import de.danoeh.antennapodSA.fragment.preferences.AutoDownloadPreferencesFragment;
import de.danoeh.antennapodSA.fragment.preferences.GpodderPreferencesFragment;
import de.danoeh.antennapodSA.fragment.preferences.IntegrationsPreferencesFragment;
import de.danoeh.antennapodSA.fragment.preferences.MainPreferencesFragment;
import de.danoeh.antennapodSA.fragment.preferences.NetworkPreferencesFragment;
import de.danoeh.antennapodSA.fragment.preferences.PlaybackPreferencesFragment;
import de.danoeh.antennapodSA.fragment.preferences.StoragePreferencesFragment;
import de.danoeh.antennapodSA.fragment.preferences.UserInterfacePreferencesFragment;

/**
 * PreferenceActivity for API 11+. In order to change the behavior of the preference UI, see
 * PreferenceController.
 */
public class PreferenceActivity extends AppCompatActivity implements SearchPreferenceResultListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        FrameLayout root = new FrameLayout(this);
        root.setId(R.id.content);
        root.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        getSupportFragmentManager().beginTransaction().replace(R.id.content, new MainPreferencesFragment()).commit();

    }

    private PreferenceFragmentCompat getPreferenceScreen(int screen) {
        PreferenceFragmentCompat prefFragment = null;

        if (screen == R.xml.preferences_user_interface) {
            prefFragment = new UserInterfacePreferencesFragment();
        } else if (screen == R.xml.preferences_integrations) {
            prefFragment = new IntegrationsPreferencesFragment();
        } else if (screen == R.xml.preferences_network) {
            prefFragment = new NetworkPreferencesFragment();
        } else if (screen == R.xml.preferences_storage) {
            prefFragment = new StoragePreferencesFragment();
        } else if (screen == R.xml.preferences_autodownload) {
            prefFragment = new AutoDownloadPreferencesFragment();
        } else if (screen == R.xml.preferences_gpodder) {
            prefFragment = new GpodderPreferencesFragment();
        } else if (screen == R.xml.preferences_playback) {
            prefFragment = new PlaybackPreferencesFragment();
        }
        return prefFragment;
    }

    public static int getTitleOfPage(int preferences) {
        switch (preferences) {
            case R.xml.preferences_network:
                return R.string.network_pref;
            case R.xml.preferences_autodownload:
                return R.string.pref_automatic_download_title;
            case R.xml.preferences_playback:
                return R.string.playback_pref;
            case R.xml.preferences_storage:
                return R.string.storage_pref;
            case R.xml.preferences_user_interface:
                return R.string.user_interface_label;
            case R.xml.preferences_integrations:
                return R.string.integrations_label;
            case R.xml.preferences_gpodder:
                return R.string.gpodnet_main_label;
            default:
                return R.string.settings_label;
        }
    }

    public PreferenceFragmentCompat openScreen(int screen) {
        PreferenceFragmentCompat fragment = getPreferenceScreen(screen);
        getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment)
                .addToBackStack(getString(getTitleOfPage(screen))).commit();
        return fragment;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                finish();
            } else {
                getSupportFragmentManager().popBackStack();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onSearchResultClicked(SearchPreferenceResult result) {
        PreferenceFragmentCompat fragment = openScreen(result.getResourceFile());
        result.highlight(fragment);
    }
}
