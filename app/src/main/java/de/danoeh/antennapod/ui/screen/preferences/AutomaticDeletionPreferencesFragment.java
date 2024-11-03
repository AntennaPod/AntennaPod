package de.danoeh.antennapod.ui.screen.preferences;

import android.content.res.Resources;
import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.TwoStatePreference;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.preferences.screen.AnimatedPreferenceFragment;


public class AutomaticDeletionPreferencesFragment extends AnimatedPreferenceFragment {
    private static final String PREF_AUTO_DELETE_LOCAL = "prefAutoDeleteLocal";
    private boolean blockAutoDeleteLocal = true;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_auto_deletion);
        setupScreen();
        buildEpisodeCleanupPreference();
        checkItemVisibility(UserPreferences.isAutoDelete());
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.pref_auto_delete_title);
    }

    private void checkItemVisibility(boolean autoDeleteEnabled) {
        findPreference(UserPreferences.PREF_FAVORITE_KEEPS_EPISODE).setEnabled(autoDeleteEnabled);
        findPreference(PREF_AUTO_DELETE_LOCAL).setEnabled(autoDeleteEnabled);
    }

    private void setupScreen() {
        findPreference(PREF_AUTO_DELETE_LOCAL).setOnPreferenceChangeListener((preference, newValue) -> {
            if (blockAutoDeleteLocal && newValue.equals(Boolean.TRUE)) {
                showAutoDeleteEnableDialog();
                return false;
            } else {
                return true;
            }
        });
        findPreference(UserPreferences.PREF_AUTO_DELETE).setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue instanceof Boolean) {
                checkItemVisibility((Boolean) newValue);
            }
            return true;
        });
    }

    private void showAutoDeleteEnableDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.pref_auto_local_delete_dialog_body)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    blockAutoDeleteLocal = false;
                    ((TwoStatePreference) findPreference(PREF_AUTO_DELETE_LOCAL)).setChecked(true);
                    blockAutoDeleteLocal = true;
                })
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }


    private void buildEpisodeCleanupPreference() {
        final Resources res = getActivity().getResources();

        ListPreference pref = findPreference(UserPreferences.PREF_EPISODE_CLEANUP);
        String[] values = res.getStringArray(
                de.danoeh.antennapod.ui.preferences.R.array.episode_cleanup_values);
        String[] entries = new String[values.length];
        for (int x = 0; x < values.length; x++) {
            int v = Integer.parseInt(values[x]);
            if (v == UserPreferences.EPISODE_CLEANUP_EXCEPT_FAVORITE) {
                entries[x] =  res.getString(R.string.episode_cleanup_except_favorite_removal);
            } else if (v == UserPreferences.EPISODE_CLEANUP_QUEUE) {
                entries[x] = res.getString(R.string.episode_cleanup_queue_removal);
            } else if (v == UserPreferences.EPISODE_CLEANUP_NULL) {
                entries[x] = res.getString(R.string.episode_cleanup_never);
            } else if (v == 0) {
                entries[x] = res.getString(R.string.episode_cleanup_after_listening);
            } else if (v > 0 && v < 24) {
                entries[x] = res.getQuantityString(R.plurals.episode_cleanup_hours_after_listening, v, v);
            } else {
                int numDays = v / 24; // assume underlying value will be NOT fraction of days, e.g., 36 (hours)
                entries[x] = res.getQuantityString(R.plurals.episode_cleanup_days_after_listening, numDays, numDays);
            }
        }
        pref.setEntries(entries);
    }
}
