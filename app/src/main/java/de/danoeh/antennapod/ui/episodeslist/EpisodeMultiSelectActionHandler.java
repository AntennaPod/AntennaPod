package de.danoeh.antennapod.ui.episodeslist;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.PluralsRes;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.LongList;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.ui.view.LocalDeleteModal;
import org.greenrobot.eventbus.EventBus;

public class EpisodeMultiSelectActionHandler {
    private static final String TAG = "EpisodeSelectHandler";
    private final Activity activity;
    private final int actionId;
    private int totalNumItems = 0;

    public EpisodeMultiSelectActionHandler(Activity activity, int actionId) {
        this.activity = activity;
        this.actionId = actionId;
    }

    public void handleAction(List<FeedItem> items) {
        if (actionId == R.id.add_to_queue_item) {
            queueChecked(items);
        } else if (actionId == R.id.remove_from_queue_item) {
            removeFromQueueChecked(items);
        }  else if (actionId == R.id.remove_inbox_item) {
            removeFromInboxChecked(items);
        } else if (actionId == R.id.mark_read_item) {
            markedCheckedPlayed(items);
        } else if (actionId == R.id.mark_unread_item) {
            markedCheckedUnplayed(items);
        } else if (actionId == R.id.download_item) {
            downloadChecked(items);
        } else if (actionId == R.id.remove_item) {
            LocalDeleteModal.showLocalFeedDeleteWarningIfNecessary(activity, items, () -> deleteChecked(items));
        } else {
            Log.e(TAG, "Unrecognized speed dial action item. Do nothing. id=" + actionId);
        }
    }

    private void queueChecked(List<FeedItem> items) {
        // Count here to give accurate number in snackbar
        List<FeedItem> toQueue = new ArrayList<>();
        for (FeedItem episode : items) {
            if (episode.hasMedia() && !episode.isTagged(FeedItem.TAG_QUEUE)) {
                toQueue.add(episode);
            }
        }
        DBWriter.addQueueItem(activity, toQueue.toArray(new FeedItem[0]));
        showMessage(R.plurals.added_to_queue_batch_label, toQueue.size());
    }

    private void removeFromQueueChecked(List<FeedItem> items) {
        long[] checkedIds = getSelectedIds(items);
        DBWriter.removeQueueItem(activity, true, checkedIds);
        showMessage(R.plurals.removed_from_queue_batch_label, checkedIds.length);
    }

    private void removeFromInboxChecked(List<FeedItem> items) {
        LongList markUnplayed = new LongList();
        for (FeedItem episode : items) {
            if (episode.isNew()) {
                markUnplayed.add(episode.getId());
            }
        }
        DBWriter.markItemPlayed(FeedItem.UNPLAYED, markUnplayed.toArray());
        showMessage(R.plurals.removed_from_inbox_batch_label, markUnplayed.size());
    }

    private void markedCheckedPlayed(List<FeedItem> items) {
        long[] checkedIds = getSelectedIds(items);
        DBWriter.markItemPlayed(FeedItem.PLAYED, checkedIds);
        showMessage(R.plurals.marked_read_batch_label, checkedIds.length);
    }

    private void markedCheckedUnplayed(List<FeedItem> items) {
        long[] checkedIds = getSelectedIds(items);
        DBWriter.markItemPlayed(FeedItem.UNPLAYED, checkedIds);
        showMessage(R.plurals.marked_unread_batch_label, checkedIds.length);
    }

    private void downloadChecked(List<FeedItem> items) {
        // download the check episodes in the same order as they are currently displayed
        int downloaded = 0;
        for (FeedItem episode : items) {
            if (episode.hasMedia() && !episode.isDownloaded() && !episode.getFeed().isLocalFeed()) {
                DownloadServiceInterface.get().download(activity, episode);
                downloaded++;
            }
        }
        showMessage(R.plurals.downloading_batch_label, downloaded);
    }

    private void deleteChecked(List<FeedItem> items) {
        int countHasMedia = 0;
        for (FeedItem feedItem : items) {
            if ((feedItem.hasMedia() && feedItem.getMedia().isDownloaded())
                    || feedItem.getFeed().isLocalFeed()) {
                countHasMedia++;
                DBWriter.deleteFeedMediaOfItem(activity, feedItem.getMedia());
            }
        }
        showMessage(R.plurals.deleted_multi_episode_batch_label, countHasMedia);
    }

    private void showMessage(@PluralsRes int msgId, int numItems) {
        if (numItems == 1) {
            return;
        }
        totalNumItems += numItems;
        activity.runOnUiThread(() -> {
            String text = activity.getResources().getQuantityString(msgId, totalNumItems, totalNumItems);
            EventBus.getDefault().post(new MessageEvent(text));
        });
    }

    private long[] getSelectedIds(List<FeedItem> items) {
        long[] checkedIds = new long[items.size()];
        for (int i = 0; i < items.size(); ++i) {
            checkedIds[i] = items.get(i).getId();
        }
        return checkedIds;
    }
}
