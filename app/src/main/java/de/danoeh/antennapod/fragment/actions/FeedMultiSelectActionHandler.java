package de.danoeh.antennapod.fragment.actions;

import android.util.Log;

import androidx.annotation.PluralsRes;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.dialog.RemoveFeedDialog;
import de.danoeh.antennapod.fragment.preferences.dialog.PreferenceSwitchDialog;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
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
            deleteChecked();
        } else if (id == R.id.keep_updated) {
            keepUpdatedChecked();
        } else if (id == R.id.autodownload) {
            deleteChecked();
        } else {
            Log.e(TAG, "Unrecognized speed dial action item. Do nothing. id=" + id);
        }
    }

    private void keepUpdatedChecked() {
        PreferenceSwitchDialog preferenceSwitchDialog = new PreferenceSwitchDialog(activity,activity.getString(R.string.kept_updated), activity.getString(R.string.keep_updated_summary));
        preferenceSwitchDialog.setOnPreferenceChangedListener(new PreferenceSwitchDialog.OnPreferenceChangedListener() {
            @Override
            public void preferenceChanged(boolean enabled) {
                if (enabled) {
                    saveFeedPreferences(feedPreferences -> feedPreferences.setKeepUpdated(enabled));
                }
            }
        });
        preferenceSwitchDialog.openDialog();
    }

    private void markedCheckedPlayed() {
        long[] checkedIds = getSelectedIds();
        DBWriter.markItemPlayed(FeedItem.PLAYED, checkedIds);
        showMessage(R.plurals.marked_read_batch_label, checkedIds.length);
    }

    private void markedCheckedUnplayed() {
        long[] checkedIds = getSelectedIds();
        DBWriter.markItemPlayed(FeedItem.UNPLAYED, checkedIds);
        showMessage(R.plurals.marked_unread_batch_label, checkedIds.length);
    }


    private void deleteChecked() {
        RemoveFeedDialog.show(activity, selectedItems, null);
    }

    private void showMessage(@PluralsRes int msgId, int numItems) {
        activity.showSnackbarAbovePlayer(activity.getResources()
                .getQuantityString(msgId, numItems, numItems), Snackbar.LENGTH_LONG);
    }

    private void showMessageMore(@PluralsRes int msgId, int countNoMedia, int countHasMedia) {
        activity.showSnackbarAbovePlayer(activity.getResources()
                .getQuantityString(msgId,
                        (countHasMedia + countNoMedia),
                        (countHasMedia + countNoMedia), countHasMedia),
                Snackbar.LENGTH_LONG);
    }

    private long[] getSelectedIds() {
        long[] checkedIds = new long[selectedItems.size()];
        for (int i = 0; i < selectedItems.size(); ++i) {
            checkedIds[i] = selectedItems.get(i).getId();
        }
        return checkedIds;
    }
    interface OnSetFeedPreferenceListener {
        void onSetFeedPreferenceListener(FeedPreferences feedPreferences);
    }
    private void saveFeedPreferences(OnSetFeedPreferenceListener onSetFeedPreferenceListener) {
        for (Feed feed : selectedItems) {
            onSetFeedPreferenceListener.onSetFeedPreferenceListener(feed.getPreferences());
            DBWriter.setFeedPreferences(feed.getPreferences());
        }
    }
}
