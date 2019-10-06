package de.danoeh.antennapod.core.storage;

import androidx.annotation.NonNull;

import java.util.List;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.APDownloadAlgorithm.ItemProvider;

interface DownloadItemSelector {
    @NonNull
    List<? extends FeedItem> getAutoDownloadableEpisodes(@NonNull ItemProvider itemProvider);
}

