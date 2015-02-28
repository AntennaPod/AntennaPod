package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.QueueAccess;

/**
 * Implementation of the EpisodeCleanupAlgorithm interface used by AntennaPod.
 */
public class APCleanupAlgorithm implements EpisodeCleanupAlgorithm<Integer> {
    private static final String TAG = "APCleanupAlgorithm";

    @Override
    public int performCleanup(Context context, Integer episodeNumber) {
        List<FeedItem> candidates = new ArrayList<FeedItem>();
        List<FeedItem> downloadedItems = DBReader.getDownloadedItems(context);
        QueueAccess queue = QueueAccess.IDListAccess(DBReader.getQueueIDList(context));
        List<FeedItem> delete;
        for (FeedItem item : downloadedItems) {
            if (item.hasMedia() && item.getMedia().isDownloaded()
                    && !queue.contains(item.getId()) && item.isRead()) {
                candidates.add(item);
            }

        }

        Collections.sort(candidates, new Comparator<FeedItem>() {
            @Override
            public int compare(FeedItem lhs, FeedItem rhs) {
                Date l = lhs.getMedia().getPlaybackCompletionDate();
                Date r = rhs.getMedia().getPlaybackCompletionDate();

                if (l == null) {
                    l = new Date(0);
                }
                if (r == null) {
                    r = new Date(0);
                }
                return l.compareTo(r);
            }
        });

        if (candidates.size() > episodeNumber) {
            delete = candidates.subList(0, episodeNumber);
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
                episodeNumber));

        return counter;
    }

    @Override
    public Integer getDefaultCleanupParameter(Context context) {
        return getPerformAutoCleanupArgs(context, 0);
    }

    @Override
    public Integer getPerformCleanupParameter(Context context, List<FeedItem> items) {
        return getPerformAutoCleanupArgs(context, items.size());
    }

    static int getPerformAutoCleanupArgs(Context context,
                                         final int episodeNumber) {
        if (episodeNumber >= 0
                && UserPreferences.getEpisodeCacheSize() != UserPreferences
                .getEpisodeCacheSizeUnlimited()) {
            int downloadedEpisodes = DBReader
                    .getNumberOfDownloadedEpisodes(context);
            if (downloadedEpisodes + episodeNumber >= UserPreferences
                    .getEpisodeCacheSize()) {

                return downloadedEpisodes + episodeNumber
                        - UserPreferences.getEpisodeCacheSize();
            }
        }
        return 0;
    }
}
