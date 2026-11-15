package de.danoeh.antennapod.ui.preferences.screen;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;

import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.preferences.R;

public class AutoDownloadPreferencesFragment extends AnimatedPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_autodownload);
        setupPreferences();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.pref_automatic_download_title);
        updateSummaries();
    }

    private void setupPreferences() {
        Preference prefOrder = findPreference(UserPreferences.PREF_AUTODOWNLOAD_ORDER);
        if (prefOrder != null) {
            prefOrder.setOnPreferenceClickListener(preference -> {
                String[] entries = getResources().getStringArray(R.array.autodownload_order_entries);
                String[] values = getResources().getStringArray(R.array.autodownload_order_values);
                int currentVal = UserPreferences.getAutoDownloadOrder();
                int selectedIndex = 0;
                for (int i = 0; i < values.length; i++) {
                    if (Integer.parseInt(values[i]) == currentVal) {
                        selectedIndex = i;
                        break;
                    }
                }
                PreferenceListDialog dialog = new PreferenceListDialog(requireContext(),
                        getString(R.string.pref_autodownload_order_title));
                dialog.setSelectedPos(selectedIndex);
                dialog.setOnPreferenceChangedListener(pos -> {
                    if (pos >= 0 && pos < values.length) {
                        UserPreferences.setPrefAutoDownloadOrder(Integer.parseInt(values[pos]));
                        updateSummaries();
                    }
                });
                dialog.openDialog(entries);
                return true;
            });
        }

        Preference prefCache = findPreference(UserPreferences.PREF_EPISODE_CACHE_SIZE);
        if (prefCache != null) {
            prefCache.setOnPreferenceClickListener(preference -> {
                String[] entries = getResources().getStringArray(R.array.episode_cache_size_entries);
                String[] values = getResources().getStringArray(R.array.episode_cache_size_values);
                int currentVal = UserPreferences.getEpisodeCacheSize();
                int selectedIndex = 0;
                for (int i = 0; i < values.length; i++) {
                    if (Integer.parseInt(values[i]) == currentVal) {
                        selectedIndex = i;
                        break;
                    }
                }
                PreferenceListDialog dialog = new PreferenceListDialog(requireContext(),
                        getString(R.string.pref_episode_cache_title));
                dialog.setSelectedPos(selectedIndex);
                dialog.setOnPreferenceChangedListener(pos -> {
                    if (pos >= 0 && pos < values.length) {
                        UserPreferences.setEpisodeCacheSize(Integer.parseInt(values[pos]));
                        updateSummaries();
                    }
                });
                dialog.openDialog(entries);
                return true;
            });
        }
    }

    private void updateSummaries() {
        Preference prefOrder = findPreference(UserPreferences.PREF_AUTODOWNLOAD_ORDER);
        if (prefOrder != null) {
            String[] entries = getResources().getStringArray(R.array.autodownload_order_entries);
            String[] values = getResources().getStringArray(R.array.autodownload_order_values);
            int currentVal = UserPreferences.getAutoDownloadOrder();
            for (int i = 0; i < values.length; i++) {
                if (Integer.parseInt(values[i]) == currentVal) {
                    prefOrder.setSummary(entries[i]);
                    break;
                }
            }
        }

        Preference prefCache = findPreference(UserPreferences.PREF_EPISODE_CACHE_SIZE);
        if (prefCache != null) {
            String[] entries = getResources().getStringArray(R.array.episode_cache_size_entries);
            String[] values = getResources().getStringArray(R.array.episode_cache_size_values);
            int currentVal = UserPreferences.getEpisodeCacheSize();
            for (int i = 0; i < values.length; i++) {
                if (Integer.parseInt(values[i]) == currentVal) {
                    prefCache.setSummary(entries[i]);
                    break;
                }
            }
        }
    }
}
