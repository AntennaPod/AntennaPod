package de.danoeh.antennapod.net.download.serviceinterface;

import android.content.Context;
import android.content.Intent;
import de.danoeh.antennapod.model.feed.FeedItem;

import java.util.HashMap;
import java.util.Map;

public abstract class DownloadServiceInterface {
    public static final String WORK_TAG = "episodeDownload";
    public static final String WORK_TAG_EPISODE_URL = "episodeUrl:";
    public static final String WORK_DATA_PROGRESS = "progress";
    public static final String WORK_DATA_MEDIA_ID = "media_id";
    public static final String WORK_DATA_WAS_QUEUED = "was_queued";
    private static DownloadServiceInterface impl;
    private Map<String, Integer> currentDownloads = new HashMap<>();

    public static DownloadServiceInterface get() {
        return impl;
    }

    public static void setImpl(DownloadServiceInterface impl) {
        DownloadServiceInterface.impl = impl;
    }

    public void setCurrentDownloads(Map<String, Integer> currentDownloads) {
        this.currentDownloads = currentDownloads;
    }

    public abstract void download(Context context, FeedItem item);

    public abstract void download(Context context, boolean cleanupMedia, DownloadRequest... requests);

    public abstract Intent makeDownloadIntent(Context context, boolean cleanupMedia, DownloadRequest... requests);

    public abstract void cancel(Context context, String url);

    public abstract void cancelAll(Context context);

    public boolean isDownloadingEpisode(String url) {
        return currentDownloads.containsKey(url);
    }

    public int getProgress(String url) {
        return isDownloadingEpisode(url) ? currentDownloads.get(url) : -1;
    }
}
