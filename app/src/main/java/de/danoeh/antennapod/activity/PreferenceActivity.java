package de.danoeh.antennapod.activity;

import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.bytehamster.lib.preferencesearch.SearchPreferenceResult;
import com.bytehamster.lib.preferencesearch.SearchPreferenceResultListener;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.fragment.preferences.AutoDownloadPreferencesFragment;
import de.danoeh.antennapod.fragment.preferences.GpodderPreferencesFragment;
import de.danoeh.antennapod.fragment.preferences.ImportExportPreferencesFragment;
import de.danoeh.antennapod.fragment.preferences.MainPreferencesFragment;
import de.danoeh.antennapod.fragment.preferences.NetworkPreferencesFragment;
import de.danoeh.antennapod.fragment.preferences.NotificationPreferencesFragment;
import de.danoeh.antennapod.fragment.preferences.PlaybackPreferencesFragment;
import de.danoeh.antennapod.fragment.preferences.StoragePreferencesFragment;
import de.danoeh.antennapod.fragment.preferences.UserInterfacePreferencesFragment;

/**
 * PreferenceActivity for API 11+. In order to change the behavior of the preference UI, see
 * PreferenceController.
 */
public class PreferenceActivity extends AppCompatActivity implements SearchPreferenceResultListener {
    private static final String FRAGMENT_TAG = "tag_preferences";

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

        if (getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content, new MainPreferencesFragment(), FRAGMENT_TAG)
                    .commit();
        }
    }

    private PreferenceFragmentCompat getPreferenceScreen(int screen) {
        PreferenceFragmentCompat prefFragment = null;

        if (screen == R.xml.preferences_user_interface) {
            prefFragment = new UserInterfacePreferencesFragment();
        } else if (screen == R.xml.preferences_network) {
            prefFragment = new NetworkPreferencesFragment();
        } else if (screen == R.xml.preferences_storage) {
            prefFragment = new StoragePreferencesFragment();
        } else if (screen == R.xml.preferences_import_export) {
            prefFragment = new ImportExportPreferencesFragment();
        } else if (screen == R.xml.preferences_autodownload) {
            prefFragment = new AutoDownloadPreferencesFragment();
        } else if (screen == R.xml.preferences_gpodder) {
            prefFragment = new GpodderPreferencesFragment();
        } else if (screen == R.xml.preferences_playback) {
            prefFragment = new PlaybackPreferencesFragment();
        } else if (screen == R.xml.preferences_notifications) {
            prefFragment = new NotificationPreferencesFragment();
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
            case R.xml.preferences_import_export:
                return R.string.import_export_pref;
            case R.xml.preferences_user_interface:
                return R.string.user_interface_label;
            case R.xml.preferences_gpodder:
                return R.string.gpodnet_main_label;
            case R.xml.preferences_notifications:
                return R.string.notification_pref_fragment;
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
