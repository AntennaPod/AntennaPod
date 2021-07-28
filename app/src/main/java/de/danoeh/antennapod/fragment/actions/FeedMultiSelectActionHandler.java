package de.danoeh.antennapod.fragment.actions;

import android.util.Log;

import androidx.annotation.PluralsRes;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.fragment.BatchFeedSettingsFragment;
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
            removeFromQueueChecked();
        } else if (id == R.id.autodownload) {
            deleteChecked();
        } else {
            Log.e(TAG, "Unrecognized speed dial action item. Do nothing. id=" + id);
        }
    }

    private void removeFromQueueChecked() {
        long[] checkedIds = getSelectedIds();
        DBWriter.removeQueueItem(activity, true, checkedIds);
        showMessage(R.plurals.removed_from_queue_batch_label, checkedIds.length);
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
        int countHasMedia = 0;
        int countNoMedia = 0;
        // TODO: 7/28/2021
        showMessageMore(R.plurals.deleted_multi_episode_batch_label, countNoMedia, countHasMedia);
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
