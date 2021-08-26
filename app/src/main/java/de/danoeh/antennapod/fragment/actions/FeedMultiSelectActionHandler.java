package de.danoeh.antennapod.fragment.actions;

import android.util.Log;

import androidx.annotation.PluralsRes;
import androidx.core.util.Consumer;

import com.google.android.material.snackbar.Snackbar;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.dialog.RemoveFeedDialog;
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
            RemoveFeedDialog.show(activity, selectedItems, null);
        } else if (id == R.id.keep_updated) {
            keepUpdatedPrefHandler();
        } else if (id == R.id.autodownload) {
            autoDownloadPrefHandler();
        } else if (id == R.id.autoDeleteDownload) {
            autoDeleteEpisodesPrefHandler();
        } else if (id == R.id.playback_speed) {
            playbackSpeedPrefHandler();
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

    private static final DecimalFormat SPEED_FORMAT =
            new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));

    private void playbackSpeedPrefHandler() {
        final String[] speeds = activity.getResources().getStringArray(R.array.playback_speed_values);
        String[] values = new String[speeds.length + 1];
        values[0] = SPEED_FORMAT.format(FeedPreferences.SPEED_USE_GLOBAL);

        String[] entries = new String[speeds.length + 1];
        entries[0] = activity.getString(R.string.feed_auto_download_global);

        System.arraycopy(speeds, 0, values, 1, speeds.length);
        System.arraycopy(speeds, 0, entries, 1, speeds.length);

        PreferenceListDialog preferenceListDialog = new PreferenceListDialog(activity,
                activity.getString(R.string.playback_speed));
        preferenceListDialog.openDialog(entries);
        preferenceListDialog.setOnPreferenceChangedListener(pos -> {
            saveFeedPreferences(feedPreferences -> {
                feedPreferences.setFeedPlaybackSpeed(Float.parseFloat((String) values[pos]));
            });

        });
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
}
