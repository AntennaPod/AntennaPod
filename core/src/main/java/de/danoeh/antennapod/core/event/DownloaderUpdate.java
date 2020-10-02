package de.danoeh.antennapod.core.event;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.util.LongList;

public class DownloaderUpdate {

    /* Downloaders that are currently running */
    @NonNull
    public final List<Downloader> downloaders;

    /**
     * IDs of feeds that are currently being downloaded
     * Often used to show some progress wheel in the action bar
     */
    public final long[] feedIds;

    /**
     * IDs of feed media that are currently being downloaded
     * Can be used to show and update download progress bars
     */
    public final long[] mediaIds;

    DownloaderUpdate(@NonNull List<Downloader> downloaders) {
        this.downloaders = downloaders;
        LongList feedIds1 = new LongList();
        LongList mediaIds1 = new LongList();
        for(Downloader d1 : downloaders) {
            int type = d1.getDownloadRequest().getFeedfileType();
            long id = d1.getDownloadRequest().getFeedfileId();
            if(type == Feed.FEEDFILETYPE_FEED) {
                feedIds1.add(id);
            } else if(type == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                mediaIds1.add(id);
            }
        }

        this.feedIds = feedIds1.toArray();
        this.mediaIds = mediaIds1.toArray();
    }

    @NonNull
    @Override
    public String toString() {
        return "DownloaderUpdate{" +
                "downloaders=" + downloaders +
                ", feedIds=" + Arrays.toString(feedIds) +
                ", mediaIds=" + Arrays.toString(mediaIds) +
                '}';
    }
}
