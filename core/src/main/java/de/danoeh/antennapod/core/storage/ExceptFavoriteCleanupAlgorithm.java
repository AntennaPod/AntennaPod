package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.UserPreferences;

/**
 * A cleanup algorithm that removes any item that isn't a favorite but only if space is needed.
 */
public class ExceptFavoriteCleanupAlgorithm extends EpisodeCleanupAlgorithm {

    private static final String TAG = "ExceptFavCleanupAlgo";

    /**
     * The maximum number of episodes that could be cleaned up.
     *
     * @return the number of episodes that *could* be cleaned up, if needed
     */
    public int getReclaimableItems() {
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

            if (l != null && r != null) {
                return l.compareTo(r);
            } else {
                // No date - compare by id which should be always incremented
                return Long.compare(lhs.getId(), rhs.getId());
            }
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
        List<FeedItem> downloadedItems = DBReader.getDownloadedItems();
        for (FeedItem item : downloadedItems) {
            if (item.hasMedia()
                    && item.getMedia().isDownloaded()
                    && !item.isTagged(FeedItem.TAG_FAVORITE)) {
                candidates.add(item);
            }
        }
        return candidates;
    }

    @Override
    public int getDefaultCleanupParameter() {
        int cacheSize = UserPreferences.getEpisodeCacheSize();
        if (cacheSize != UserPreferences.getEpisodeCacheSizeUnlimited()) {
            int downloadedEpisodes = DBReader.getNumberOfDownloadedEpisodes();
            if (downloadedEpisodes > cacheSize) {
                return downloadedEpisodes - cacheSize;
            }
        }
        return 0;
    }
}
