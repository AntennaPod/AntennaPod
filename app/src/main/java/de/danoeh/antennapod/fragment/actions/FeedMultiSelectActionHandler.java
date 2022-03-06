package de.danoeh.antennapod.fragment.actions;

import android.util.Log;

import androidx.annotation.PluralsRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.databinding.PlaybackSpeedFeedSettingDialogBinding;
import de.danoeh.antennapod.dialog.RemoveFeedDialog;
import de.danoeh.antennapod.dialog.TagSettingsDialog;
import de.danoeh.antennapod.fragment.preferences.dialog.PreferenceListDialog;
import de.danoeh.antennapod.fragment.preferences.dialog.PreferenceSwitchDialog;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedPreferences;

public class FeedMultiSelectActionHandler {
    private static final String TAG = "FeedSelectHandler";
    private final MainActivity activity;
    private final List<Feed> selectedItems;

    public FeedMultiSelectActionHandler(MainActivity activity, List<Feed> selectedItems) {
        this.activity = activity;
        this.selectedItems = selectedItems;
    }

    public void handleAction(int id) {
        if (id == R.id.remove_item) {
            RemoveFeedDialog.show(activity, selectedItems);
        } else if (id == R.id.keep_updated) {
            keepUpdatedPrefHandler();
        } else if (id == R.id.autodownload) {
            autoDownloadPrefHandler();
        } else if (id == R.id.autoDeleteDownload) {
            autoDeleteEpisodesPrefHandler();
        } else if (id == R.id.playback_speed) {
            playbackSpeedPrefHandler();
        } else if (id == R.id.edit_tags) {
            editFeedPrefTags();
        } else {
            Log.e(TAG, "Unrecognized speed dial action item. Do nothing. id=" + id);
        }
    }

    private void autoDownloadPrefHandler() {
        PreferenceSwitchDialog preferenceSwitchDialog = new PreferenceSwitchDialog(activity,
                activity.getString(R.string.auto_download_settings_label),
                activity.getString(R.string.auto_download_label));
        preferenceSwitchDialog.setOnPreferenceChangedListener(new PreferenceSwitchDialog.OnPreferenceChangedListener() {
            @Override
            public void preferenceChanged(boolean enabled) {
                saveFeedPreferences(feedPreferences -> feedPreferences.setAutoDownload(enabled));
            }
        });
        preferenceSwitchDialog.openDialog();
    }

    private void playbackSpeedPrefHandler() {
        PlaybackSpeedFeedSettingDialogBinding viewBinding =
                PlaybackSpeedFeedSettingDialogBinding.inflate(activity.getLayoutInflater());
        viewBinding.seekBar.setProgressChangedListener(speed ->
                viewBinding.currentSpeedLabel.setText(String.format(Locale.getDefault(), "%.2fx", speed)));
        viewBinding.useGlobalCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewBinding.seekBar.setEnabled(!isChecked);
            viewBinding.seekBar.setAlpha(isChecked ? 0.4f : 1f);
            viewBinding.currentSpeedLabel.setAlpha(isChecked ? 0.4f : 1f);
        });
        viewBinding.seekBar.updateSpeed(1.0f);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.playback_speed)
                .setView(viewBinding.getRoot())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    float newSpeed = viewBinding.useGlobalCheckbox.isChecked()
                            ? FeedPreferences.SPEED_USE_GLOBAL : viewBinding.seekBar.getCurrentSpeed();
                    saveFeedPreferences(feedPreferences -> feedPreferences.setFeedPlaybackSpeed(newSpeed));
                })
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }

    private void autoDeleteEpisodesPrefHandler() {
        PreferenceListDialog preferenceListDialog = new PreferenceListDialog(activity,
                "Auto delete episodes");
        String[] items = activity.getResources().getStringArray(R.array.spnAutoDeleteItems);
        String[] values = activity.getResources().getStringArray(R.array.spnAutoDeleteValues);
        preferenceListDialog.openDialog(items);
        preferenceListDialog.setOnPreferenceChangedListener(which -> {
            FeedPreferences.AutoDeleteAction autoDeleteAction = null;
            switch (values[which]) {
                case "global":
                    autoDeleteAction = FeedPreferences.AutoDeleteAction.GLOBAL;
                    break;
                case "always":
                    autoDeleteAction = FeedPreferences.AutoDeleteAction.YES;
                    break;
                case "never":
                    autoDeleteAction = FeedPreferences.AutoDeleteAction.NO;
                    break;
                default:
            }
            FeedPreferences.AutoDeleteAction finalAutoDeleteAction = autoDeleteAction;
            saveFeedPreferences(feedPreferences -> {
                feedPreferences.setAutoDeleteAction(finalAutoDeleteAction);
            });
        });
    }

    private void keepUpdatedPrefHandler() {
        PreferenceSwitchDialog preferenceSwitchDialog = new PreferenceSwitchDialog(activity,
                activity.getString(R.string.kept_updated),
                activity.getString(R.string.keep_updated_summary));
        preferenceSwitchDialog.setOnPreferenceChangedListener(keepUpdated -> {
            saveFeedPreferences(feedPreferences -> {
                feedPreferences.setKeepUpdated(keepUpdated);
            });
        });
        preferenceSwitchDialog.openDialog();
    }

    private void showMessage(@PluralsRes int msgId, int numItems) {
        activity.showSnackbarAbovePlayer(activity.getResources()
                .getQuantityString(msgId, numItems, numItems), Snackbar.LENGTH_LONG);
    }

    private void saveFeedPreferences(Consumer<FeedPreferences> preferencesConsumer) {
        for (Feed feed : selectedItems) {
            preferencesConsumer.accept(feed.getPreferences());
            DBWriter.setFeedPreferences(feed.getPreferences());
        }
        showMessage(R.plurals.updated_feeds_batch_label, selectedItems.size());
    }

    private void editFeedPrefTags() {
        ArrayList<FeedPreferences> preferencesList = new ArrayList<>();
        for (Feed feed : selectedItems) {
            preferencesList.add(feed.getPreferences());
        }
        TagSettingsDialog.newInstance(preferencesList).show(activity.getSupportFragmentManager(),
                TagSettingsDialog.TAG);
    }
}
