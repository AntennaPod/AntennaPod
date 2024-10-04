package de.danoeh.antennapod.ui.preferences.screen;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.preferences.R;

public class AutoDownloadPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_autodownload);

        findPreference(UserPreferences.PREF_ENABLE_AUTODL).setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    if (newValue instanceof Boolean) {
                        checkAutodownloadItemVisibility((Boolean) newValue);
                    }
                    return true;
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.pref_automatic_download_title);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAutodownloadItemVisibility(UserPreferences.isEnableAutodownload());
    }

    private void checkAutodownloadItemVisibility(boolean autoDownload) {
        findPreference(UserPreferences.PREF_EPISODE_CACHE_SIZE).setEnabled(autoDownload);
        findPreference(UserPreferences.PREF_ENABLE_AUTODL_ON_BATTERY).setEnabled(autoDownload);
    }
}
