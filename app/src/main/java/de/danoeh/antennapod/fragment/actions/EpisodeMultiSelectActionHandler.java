package de.danoeh.antennapod.fragment.actions;

import android.util.Log;

import androidx.annotation.PluralsRes;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.DownloadRequestCreator;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.model.feed.FeedItem;

public class EpisodeMultiSelectActionHandler {
    private static final String TAG = "EpisodeSelectHandler";
    private final MainActivity activity;
    private final List<FeedItem> selectedItems;

    public EpisodeMultiSelectActionHandler(MainActivity activity, List<FeedItem> selectedItems) {
        this.activity = activity;
        this.selectedItems = selectedItems;
    }

    public void handleAction(int id) {
        if (id == R.id.add_to_queue_batch) {
            queueChecked();
        } else if (id == R.id.remove_from_queue_batch) {
            removeFromQueueChecked();
        } else if (id == R.id.mark_read_batch) {
            markedCheckedPlayed();
        } else if (id == R.id.mark_unread_batch) {
            markedCheckedUnplayed();
        } else if (id == R.id.download_batch) {
            downloadChecked();
        } else if (id == R.id.delete_batch) {
            deleteChecked();
        } else {
            Log.e(TAG, "Unrecognized speed dial action item. Do nothing. id=" + id);
        }
    }

    private void queueChecked() {
        // Check if an episode actually contains any media files before adding it to queue
        LongList toQueue = new LongList(selectedItems.size());
        for (FeedItem episode : selectedItems) {
            if (episode.hasMedia()) {
                toQueue.add(episode.getId());
            }
        }
        DBWriter.addQueueItem(activity, true, toQueue.toArray());
        showMessage(R.plurals.added_to_queue_batch_label, toQueue.size());
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

    private void downloadChecked() {
        // download the check episodes in the same order as they are currently displayed
        List<DownloadRequest> requests = new ArrayList<>();
        for (FeedItem episode : selectedItems) {
            if (episode.hasMedia() && !episode.getFeed().isLocalFeed()) {
                requests.add(DownloadRequestCreator.create(episode.getMedia()).build());
            }
        }
        DownloadService.download(activity, true, requests.toArray(new DownloadRequest[0]));
        showMessage(R.plurals.downloading_batch_label, requests.size());
    }

    private void deleteChecked() {
        int countHasMedia = 0;
        for (FeedItem feedItem : selectedItems) {
            if (feedItem.hasMedia() && feedItem.getMedia().isDownloaded()) {
                countHasMedia++;
                DBWriter.deleteFeedMediaOfItem(activity, feedItem.getMedia().getId());
            }
        }
        showMessage(R.plurals.deleted_multi_episode_batch_label, countHasMedia);
    }

    private void showMessage(@PluralsRes int msgId, int numItems) {
        activity.showSnackbarAbovePlayer(activity.getResources()
                .getQuantityString(msgId, numItems, numItems), Snackbar.LENGTH_LONG);
    }

    private long[] getSelectedIds() {
        long[] checkedIds = new long[selectedItems.size()];
        for (int i = 0; i < selectedItems.size(); ++i) {
            checkedIds[i] = selectedItems.get(i).getId();
        }
        return checkedIds;
    }
}
