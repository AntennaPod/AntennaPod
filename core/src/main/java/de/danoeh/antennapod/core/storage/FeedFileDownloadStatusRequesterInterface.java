package de.danoeh.antennapod.core.storage;

import android.support.annotation.NonNull;

import de.danoeh.antennapod.core.feed.FeedFile;

public interface FeedFileDownloadStatusRequesterInterface {
    /**
     * @return {@code true} if the named feedfile is in the downloads list
     */
    boolean isDownloadingFile(@NonNull FeedFile item);

}
