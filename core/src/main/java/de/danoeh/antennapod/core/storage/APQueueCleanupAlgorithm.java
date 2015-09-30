package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.util.LongList;

/**
 * A cleanup algorithm that removes any item that isn't in the queue and isn't a favorite
 * but only if space is needed.
 */
public class APQueueCleanupAlgorithm extends EpisodeCleanupAlgorithm {

    private static final String TAG = "APQueueCleanupAlgorithm";

    @Override
    public int performCleanup(Context context, int numberOfEpisodesToDelete) {
        List<FeedItem> candidates = new ArrayList<>();
        List<FeedItem> downloadedItems = DBReader.getDownloadedItems();
        LongList queue = DBReader.getQueueIDList();
        List<FeedItem> delete;
        for (FeedItem item : downloadedItems) {
            if (!queue.contains(item.getId())) {
                candidates.add(item);
            }
        }

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


        Log.i(TAG, String.format(
                "Auto-delete deleted %d episodes (%d requested)", counter,
                numberOfEpisodesToDelete));

        return counter;
    }

    @Override
    public int getDefaultCleanupParameter() {
        return 0;
    }
}
