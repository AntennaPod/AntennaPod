package de.danoeh.antennapod.core.storage;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.List;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.APDownloadAlgorithm.ItemProvider;

@VisibleForTesting
public interface DownloadItemSelector {
    @NonNull
    List<? extends FeedItem> getAutoDownloadableEpisodes(@NonNull ItemProvider itemProvider);
}

