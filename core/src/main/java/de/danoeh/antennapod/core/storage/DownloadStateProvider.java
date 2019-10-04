package de.danoeh.antennapod.core.storage;

import androidx.annotation.NonNull;

import de.danoeh.antennapod.core.feed.FeedFile;

/**
 * Allow callers to query the states of downloads, but not affect them.
 */
public interface DownloadStateProvider {
    /**
     * @return {@code true} if the named feedfile is in the downloads list
     */
    boolean isDownloadingFile(@NonNull FeedFile item);

}
