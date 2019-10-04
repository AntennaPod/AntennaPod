package de.danoeh.antennapod.core.storage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.List;

import de.danoeh.antennapod.core.feed.FeedItem;

/**
 * @see DBWriter#addQueueItem(Context, boolean, long...) it uses the class to determine
 * the positions of the {@link FeedItem} in the queue.
 */
class ItemEnqueuePositionCalculator {

    public static class Options {
        private boolean enqueueAtFront = false;
        private boolean keepInProgressAtFront = false;

        public boolean isEnqueueAtFront() {
            return enqueueAtFront;
        }

        public Options setEnqueueAtFront(boolean enqueueAtFront) {
            this.enqueueAtFront = enqueueAtFront;
            return this;
        }

        public boolean isKeepInProgressAtFront() {
            return keepInProgressAtFront;
        }

        public Options setKeepInProgressAtFront(boolean keepInProgressAtFront) {
            this.keepInProgressAtFront = keepInProgressAtFront;
            return this;
        }
    }

    @NonNull
    private final Options options;

    @VisibleForTesting
    DownloadStateProvider downloadStateProvider = DownloadRequester.getInstance();

    public ItemEnqueuePositionCalculator(@NonNull Options options) {
        this.options = options;
    }

    /**
     *
     * @param positionAmongToAdd Typically, the callers has a list of items to be inserted to
     *                           the queue. This parameter indicates the position (0-based) of
     *                           the item among the one to inserted. E.g., it is needed for
     *                           enqueue at front option.
     *
     * @param item the item to be inserted
     * @param curQueue the queue to which the item is to be inserted
     * @return the position (0-based) the item should be inserted to the named queu
     */
    public int calcPosition(int positionAmongToAdd, FeedItem item, List<FeedItem> curQueue) {
        if (options.isEnqueueAtFront()) {
            if (options.isKeepInProgressAtFront() &&
                    curQueue.size() > 0 &&
                    curQueue.get(0).getMedia() != null &&
                    curQueue.get(0).getMedia().isInProgress()) {
                // leave the front in progress item at the front
                return getPositionOfFirstNonDownloadingItem(positionAmongToAdd + 1, curQueue);
            } else { // typical case
                // return NOT 0, so that when a list of items are inserted, the items inserted
                // keep the same order. Returning 0 will reverse the order
                return getPositionOfFirstNonDownloadingItem(positionAmongToAdd, curQueue);
            }
        } else {
            return curQueue.size();
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
}
