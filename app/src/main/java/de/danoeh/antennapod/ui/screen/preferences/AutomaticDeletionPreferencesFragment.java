package de.danoeh.antennapod.ui.screen.preferences;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;


public class AutomaticDeletionPreferencesFragment extends PreferenceFragmentCompat {
    private static final String PREF_AUTO_DELETE_LOCAL = "prefAutoDeleteLocal";
    private boolean blockAutoDeleteLocal = true;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_auto_deletion);
        setupScreen();
        checkItemVisibility(UserPreferences.isAutoDeletePlayed());
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
        findPreference(UserPreferences.PREF_AUTO_DELETE_PLAYED).setOnPreferenceChangeListener((preference, newValue) -> {
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
}
