package de.danoeh.antennapod.ui.screen.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.storage.mediamanagement.MediaFileMigrationWorker;

import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.preferences.screen.AnimatedPreferenceFragment;
import de.danoeh.antennapod.ui.preferences.screen.downloads.ChooseDataFolderDialog;

import java.io.File;


public class DownloadsPreferencesFragment extends AnimatedPreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String PREF_SCREEN_AUTODL = "prefAutoDownloadSettings";
    private static final String PREF_SCREEN_AUTO_DELETE = "prefAutoDeleteScreen";
    private static final String PREF_PROXY = "prefProxy";
    private static final String PREF_CHOOSE_DATA_DIR = "prefChooseDataDir";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_downloads);
        setupNetworkScreen();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.downloads_pref);
        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        setDataFolderText();
    }

    private void setupNetworkScreen() {
        findPreference(PREF_SCREEN_AUTODL).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_autodownload);
            return true;
        });
        findPreference(PREF_SCREEN_AUTO_DELETE).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_auto_deletion);
            return true;
        });
        // validate and set correct value: number of downloads between 1 and 50 (inclusive)
        findPreference(PREF_PROXY).setOnPreferenceClickListener(preference -> {
            ProxyDialog dialog = new ProxyDialog(getActivity());
            dialog.show();
            return true;
        });
        findPreference(PREF_CHOOSE_DATA_DIR).setOnPreferenceClickListener(preference -> {
            ChooseDataFolderDialog.showDialog(getContext(), moveRequest -> {
                if (moveRequest.moveFiles) {
                    showMoveFilesDialog(moveRequest.newPath, moveRequest.forceMoveInsufficientSpace);
                } else {
                    UserPreferences.setDataFolder(moveRequest.newPath);
                    setDataFolderText();
                }
            });
            return true;
        });
        setDataFolderText();
    }

    private void setDataFolderText() {
        File f = UserPreferences.getDataFolder(null);
        if (f != null) {
            findPreference(PREF_CHOOSE_DATA_DIR).setSummary(f.getAbsolutePath());
        }
    }

    private void showMoveFilesDialog(String newPath, boolean forceMove) {
        new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.move_files_dialog_title)
                .setMessage(R.string.move_files_dialog_message)
                .setPositiveButton(R.string.move_files_button, (dialog, which)
                        -> startFileMigration(newPath, forceMove))
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }

    private void startFileMigration(String newPath, boolean forceMove) {
        // Check space before starting migration unless force move is enabled
        if (!forceMove && !hasEnoughSpaceForMigration(newPath)) {
            return; // Error dialog already shown
        }

        MediaFileMigrationWorker.enqueue(getContext(), newPath, forceMove, forceMove);
        setDataFolderText();
    }

    private boolean hasEnoughSpaceForMigration(String newPath) {
        File targetDir = new File(newPath);

        // Use same validation as storage detection
        if (!isWritable(targetDir)) {
            new MaterialAlertDialogBuilder(getContext())
                    .setTitle("Storage Not Accessible")
                    .setMessage("The selected storage location is not accessible for writing.")
                    .setPositiveButton(R.string.confirm_label, null)
                    .show();
            return false;
        }

        long availableSpace;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                android.os.storage.StorageManager storageManager = (android.os.storage.StorageManager) 
                        getContext().getSystemService(Context.STORAGE_SERVICE);
                java.util.UUID uuid = storageManager.getUuidForPath(targetDir);
                availableSpace = storageManager.getAllocatableBytes(uuid);
            } catch (Exception e) {
                availableSpace = targetDir.getUsableSpace();
            }
        } else {
            availableSpace = targetDir.getUsableSpace();
        }

        // Quick space check before starting worker
        // Rough estimate: assume average 50MB per episode
        // The worker will do precise calculation
        long roughEstimate = 50 * 1024 * 1024; // 50MB in bytes

        if (availableSpace < roughEstimate) {
            new MaterialAlertDialogBuilder(getContext())
                    .setTitle("Insufficient Storage")
                    .setMessage(getString(R.string.migration_failed))
                    .setPositiveButton(R.string.confirm_label, null)
                    .show();
            return false;
        }

        return true;
    }

    private boolean isWritable(File dir) {
        return dir != null && dir.exists() && dir.canRead() && dir.canWrite();
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (UserPreferences.PREF_UPDATE_INTERVAL_MINUTES.equals(key)
                || UserPreferences.PREF_MOBILE_UPDATE.equals(key)) {
            FeedUpdateManager.getInstance().restartUpdateAlarm(getContext(), true);
        }
    }
}
