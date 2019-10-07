package de.danoeh.antennapod.core.storage;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.danoeh.antennapod.core.feed.FeedFilter;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.storage.APDownloadAlgorithm.ItemProvider;

@VisibleForTesting
public class DownloadItemSelectorEpisodicImpl implements DownloadItemSelector {
    private static final String TAG = "DlItemSelectorEpisodic";

    @Override
    @NonNull
    public List<? extends FeedItem> getAutoDownloadableEpisodes(@NonNull ItemProvider itemProvider) {
        List<FeedItem> candidates;
        final List<? extends FeedItem> queue = itemProvider.getQueue();
        final List<? extends FeedItem> newItems = itemProvider.getNewItemsList();
        candidates = new ArrayList<>(queue.size() + newItems.size());
        candidates.addAll(queue);
        for(FeedItem newItem : newItems) {
            FeedPreferences feedPrefs = newItem.getFeed().getPreferences();
            FeedFilter feedFilter = feedPrefs.getFilter();
            if(!candidates.contains(newItem) && feedFilter.shouldAutoDownload(newItem)) {
                candidates.add(newItem);
            }
        }

        // filter items that are not auto downloadable
        Iterator<FeedItem> it = candidates.iterator();
        while(it.hasNext()) {
            FeedItem item = it.next();
            if(!item.isAutoDownloadable()) {
                it.remove();
            }
        }
        return candidates;
    }
}
