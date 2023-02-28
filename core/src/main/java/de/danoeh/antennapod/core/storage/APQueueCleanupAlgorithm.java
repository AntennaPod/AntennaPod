package de.danoeh.antennapod.core.storage;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;

/**
 * A cleanup algorithm that removes any item that isn't in the queue and isn't a favorite
 * but only if space is needed.
 */
public class APQueueCleanupAlgorithm extends EpisodeCleanupAlgorithm {

    private static final String TAG = "APQueueCleanupAlgorithm";

    /**
     * @return the number of episodes that *could* be cleaned up, if needed
     */
    public int getReclaimableItems()
    {
        return getCandidates().size();
    }

    @Override
    public int performCleanup(Context context, int numberOfEpisodesToDelete) {
        List<FeedItem> candidates = getCandidates();
        List<FeedItem> delete;

        // in the absence of better data, we'll sort by item publication date
        Collections.sort(candidates, (lhs, rhs) -> {
            Date l = lhs.getPubDate();
            Date r = rhs.getPubDate();

            if (l == null) {
                l = new Date();
            }
            if (r == null) {
                r = new Date();
            }
            return l.compareTo(r);
        });

        if (candidates.size() > numberOfEpisodesToDelete) {
            delete = candidates.subList(0, numberOfEpisodesToDelete);
        } else {
            delete = candidates;
        }

        for (FeedItem item : delete) {
            try {
                DBWriter.deleteFeedMediaOfItem(context, item.getMedia().getId()).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        int counter = delete.size();


        Log.i(TAG, String.format(Locale.US,
                "Auto-delete deleted %d episodes (%d requested)", counter,
                numberOfEpisodesToDelete));

        return counter;
    }

    @NonNull
    private List<FeedItem> getCandidates() {
        List<FeedItem> candidates = new ArrayList<>();
        List<FeedItem> downloadedItems = DBReader.getEpisodes(0, Integer.MAX_VALUE,
                new FeedItemFilter(FeedItemFilter.DOWNLOADED), SortOrder.DATE_NEW_OLD);
        for (FeedItem item : downloadedItems) {
            if (item.hasMedia()
                    && item.getMedia().isDownloaded()
                    && !item.isTagged(FeedItem.TAG_QUEUE)
                    && !item.isTagged(FeedItem.TAG_FAVORITE)) {
                candidates.add(item);
            }
        }
        return candidates;
    }

    @Override
    public int getDefaultCleanupParameter() {
        return getNumEpisodesToCleanup(0);
    }
}
