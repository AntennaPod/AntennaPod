package de.danoeh.antennapod.core.storage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.List;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences.EnqueueLocation;
import de.danoeh.antennapod.core.util.playback.Playable;

/**
 * @see DBWriter#addQueueItem(Context, boolean, long...) it uses the class to determine
 * the positions of the {@link FeedItem} in the queue.
 */
class ItemEnqueuePositionCalculator {

    @NonNull
    private final EnqueueLocation enqueueLocation;

    @VisibleForTesting
    DownloadStateProvider downloadStateProvider = DownloadRequester.getInstance();

    public ItemEnqueuePositionCalculator(@NonNull EnqueueLocation enqueueLocation) {
        this.enqueueLocation = enqueueLocation;
    }

    /**
     * @param positionAmongToAdd Typically, the callers has a list of items to be inserted to
     *                           the queue. This parameter indicates the position (0-based) of
     *                           the item among the one to inserted. E.g., it is needed for
     *                           enqueue at front option.
     * @param item               the item to be inserted
     * @param curQueue           the queue to which the item is to be inserted
     * @param currentPlaying     the currently playing media
     * @return the position (0-based) the item should be inserted to the named queue
     */
    public int calcPosition(int positionAmongToAdd, @NonNull FeedItem item,
                            @NonNull List<FeedItem> curQueue, @Nullable Playable currentPlaying) {
        switch (enqueueLocation) {
            case BACK:
                return curQueue.size();
            case FRONT:
                // return NOT 0, so that when a list of items are inserted, the items inserted
                // keep the same order. Returning 0 will reverse the order
                return getPositionOfFirstNonDownloadingItem(positionAmongToAdd, curQueue);
            case AFTER_CURRENTLY_PLAYING:
                int currentlyPlayingPosition = getCurrentlyPlayingPosition(curQueue, currentPlaying);
                return getPositionOfFirstNonDownloadingItem(
                        currentlyPlayingPosition + (1 + positionAmongToAdd), curQueue);
            default:
                throw new AssertionError("calcPosition() : unrecognized enqueueLocation option: " + enqueueLocation);
        }
    }

    private int getPositionOfFirstNonDownloadingItem(int startPosition, List<FeedItem> curQueue) {
        final int curQueueSize = curQueue.size();
        for (int i = startPosition; i < curQueueSize; i++) {
            if (!isItemAtPositionDownloading(i, curQueue)) {
                return i;
            } // else continue to search;
        }
        return curQueueSize;
    }

    private boolean isItemAtPositionDownloading(int position, List<FeedItem> curQueue) {
        FeedItem curItem;
        try {
            curItem = curQueue.get(position);
        } catch (IndexOutOfBoundsException e) {
            curItem = null;
        }

        if (curItem != null
                && curItem.getMedia() != null
                && downloadStateProvider.isDownloadingFile(curItem.getMedia())) {
            return true;
        } else {
            return false;
        }
    }

    private static int getCurrentlyPlayingPosition(@NonNull List<FeedItem> curQueue,
                                                   @Nullable Playable currentPlaying) {
        if (!(currentPlaying instanceof FeedMedia)) {
            return -1;
        }
        final long curPlayingItemId = ((FeedMedia) currentPlaying).getItem().getId();
        for (int i = 0; i < curQueue.size(); i++) {
            if (curPlayingItemId == curQueue.get(i).getId()) {
                return i;
            }
        }
        return -1;
    }
}
