package de.danoeh.antennapod.net.download.serviceinterface;

import android.content.Context;
import de.danoeh.antennapod.model.download.DownloadStatus;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;

import java.util.HashMap;
import java.util.Map;

public abstract class DownloadServiceInterface {
    public static final String WORK_TAG = "episodeDownload";
    public static final String WORK_TAG_EPISODE_URL = "episodeUrl:";
    public static final String WORK_DATA_PROGRESS = "progress";
    public static final String WORK_DATA_MEDIA_ID = "media_id";
    public static final String WORK_DATA_WAS_QUEUED = "was_queued";
    private static DownloadServiceInterface impl;
    private Map<String, DownloadStatus> currentDownloads = new HashMap<>();

    public static DownloadServiceInterface get() {
        return impl;
    }

    public static void setImpl(DownloadServiceInterface impl) {
        DownloadServiceInterface.impl = impl;
    }

    public void setCurrentDownloads(Map<String, DownloadStatus> currentDownloads) {
        this.currentDownloads = currentDownloads;
    }

    /**
     * Download immediately after user action.
     */
    public abstract void downloadNow(Context context, FeedItem item, boolean ignoreConstraints);

    /**
     * Download when device seems fit.
     */
    public abstract void download(Context context, FeedItem item);

    public abstract void cancel(Context context, FeedMedia media);

    public abstract void cancelAll(Context context);

    public boolean isDownloadingEpisode(String url) {
        return currentDownloads.containsKey(url)
                && currentDownloads.get(url).getState() != DownloadStatus.STATE_COMPLETED;
    }

    public boolean isEpisodeQueued(String url) {
        return currentDownloads.containsKey(url)
                && currentDownloads.get(url).getState() == DownloadStatus.STATE_QUEUED;
    }

    public int getProgress(String url) {
        return isDownloadingEpisode(url) ? currentDownloads.get(url).getProgress() : -1;
    }
}
