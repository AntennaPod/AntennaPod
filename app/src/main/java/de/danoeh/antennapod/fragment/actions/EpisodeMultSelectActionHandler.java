package de.danoeh.antennapod.fragment.actions;

import android.util.Log;

import androidx.annotation.PluralsRes;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.fragment.FeedItemlistFragment;

public class EpisodeMultSelectActionHandler {
    private FeedItemlistFragment feedItemlistFragment;

    public EpisodeMultSelectActionHandler(FeedItemlistFragment feedItemlistFragment) {
        this.feedItemlistFragment = feedItemlistFragment;
    }

    public void handleAction(int id) {
        switch (id) {
            case R.id.add_to_queue_batch:
                queueChecked();
                break;
            case R.id.remove_from_queue_batch:
                removeFromQueueChecked();
                break;
            case R.id.mark_read_batch:
                markedCheckedPlayed();
                break;
            case R.id.mark_unread_batch:
                markedCheckedUnplayed();
                break;
            case R.id.download_batch:
                downloadChecked();
                break;
            case R.id.delete_batch:
                deleteChecked();
                break;
            default:
                Log.e(feedItemlistFragment.getTag(), "Unrecognized speed dial action item. Do nothing. id=" + id);
        }
    }

    private void queueChecked() {
        // Check if an episode actually contains any media files before adding it to queue
        LongList toQueue = new LongList(feedItemlistFragment.getSelectedItems().size());
        for (FeedItem episode : feedItemlistFragment.getSelectedItems()) {
            if (episode.hasMedia()) {
                toQueue.add(episode.getId());
            }
        }
        DBWriter.addQueueItem(feedItemlistFragment.getActivity(), true, toQueue.toArray());
        showMessage(R.plurals.added_to_queue_batch_label, toQueue.size());
    }

    private void removeFromQueueChecked() {
        long[] checkedIds = getSelectedIds();
        DBWriter.removeQueueItem(feedItemlistFragment.getActivity(), true, checkedIds);
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
        List<FeedItem> toDownload = new ArrayList<>(feedItemlistFragment.getSelectedItems().size());
        List<FeedItem> episodes = feedItemlistFragment.getSelectedItems();
        for (FeedItem episode : episodes) {
            if (episode.hasMedia() && !episode.getFeed().isLocalFeed()) {
                toDownload.add(episode);
            }
        }
        try {
            DownloadRequester.getInstance().downloadMedia(feedItemlistFragment.getActivity(),
                    true, toDownload.toArray(new FeedItem[0]));
        } catch (DownloadRequestException e) {
            e.printStackTrace();
            DownloadRequestErrorDialogCreator.newRequestErrorDialog(feedItemlistFragment.getActivity(), e.getMessage());
        }
        showMessage(R.plurals.downloading_batch_label, toDownload.size());
    }

    private void deleteChecked() {
        int countHasMedia = 0;
        int countNoMedia = 0;
        List<FeedItem> episodes = feedItemlistFragment.getSelectedItems();
        for (FeedItem feedItem : episodes) {
            if (feedItem.hasMedia() && feedItem.getMedia().isDownloaded()) {
                countHasMedia++;
                DBWriter.deleteFeedMediaOfItem(feedItemlistFragment.getActivity(), feedItem.getMedia().getId());
            } else {
                countNoMedia++;
            }
        }
        showMessageMore(R.plurals.deleted_multi_episode_batch_label, countNoMedia, countHasMedia);
    }

    private void showMessage(@PluralsRes int msgId, int numItems) {
        ((MainActivity) feedItemlistFragment.getActivity()).showSnackbarAbovePlayer(
                feedItemlistFragment.getResources().getQuantityString(msgId, numItems, numItems), Snackbar.LENGTH_LONG);
    }

    private void showMessageMore(@PluralsRes int msgId, int countNoMedia, int countHasMedia) {
        ((MainActivity) feedItemlistFragment.getActivity()).showSnackbarAbovePlayer(
                feedItemlistFragment.getResources().getQuantityString(msgId,
                        (countHasMedia + countNoMedia),
                        (countHasMedia + countNoMedia), countHasMedia),
                Snackbar.LENGTH_LONG);
    }

    private long[] getSelectedIds() {
        List<FeedItem> items = feedItemlistFragment.getSelectedItems();
        long[] checkedIds = new long[items.size()];
        for (int i = 0; i < items.size(); ++i) {
            checkedIds[i] = items.get(i).getId();
        }
        return checkedIds;
    }
}
