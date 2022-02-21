package de.danoeh.antennapod.fragment.preferences;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.dialog.ChooseDataFolderDialog;

import java.io.File;

public class StoragePreferencesFragment extends PreferenceFragmentCompat {
    private static final String PREF_CHOOSE_DATA_DIR = "prefChooseDataDir";
    private static final String PREF_IMPORT_EXPORT = "prefImportExport";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_storage);
        setupStorageScreen();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.storage_pref);
    }

    @Override
    public void onResume() {
        super.onResume();
        setDataFolderText();
    }

    private void setupStorageScreen() {
        findPreference(PREF_CHOOSE_DATA_DIR).setOnPreferenceClickListener(
                preference -> {
                    ChooseDataFolderDialog.showDialog(getContext(), path -> {
                        UserPreferences.setDataFolder(path);
                        setDataFolderText();
                    });
                    return true;
                }
        );
        findPreference(PREF_IMPORT_EXPORT).setOnPreferenceClickListener(
                preference -> {
                    ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_import_export);
                    return true;
                }
        );
    }

    private void setDataFolderText() {
        File f = UserPreferences.getDataFolder(null);
        if (f != null) {
            findPreference(PREF_CHOOSE_DATA_DIR).setSummary(f.getAbsolutePath());
        }
    }
}
