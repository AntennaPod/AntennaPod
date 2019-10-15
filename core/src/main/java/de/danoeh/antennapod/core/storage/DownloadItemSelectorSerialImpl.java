package de.danoeh.antennapod.core.storage;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.core.feed.FeedItem;

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class DownloadItemSelectorSerialImpl implements DownloadItemSelector {
    private static final String TAG = "DlItemSelectorSerial";

    public DownloadItemSelectorSerialImpl() { }

    @NonNull
    @Override
    public List<? extends FeedItem> getAutoDownloadableEpisodes() {
        return new ArrayList<>(); // TODO-1077: 10/13/2019 - to be implemented
    }
}
