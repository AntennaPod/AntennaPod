package de.danoeh.antennapodSA.core.service.download.handler;

import android.content.Context;
import android.util.Log;

import de.danoeh.antennapodSA.core.ClientConfig;
import de.danoeh.antennapodSA.core.feed.Feed;
import de.danoeh.antennapodSA.core.service.download.DownloadRequest;
import de.danoeh.antennapodSA.core.service.download.DownloadStatus;
import de.danoeh.antennapodSA.core.storage.DBTasks;
import de.danoeh.antennapodSA.core.storage.DownloadRequestException;
import de.danoeh.antennapodSA.core.storage.DownloadRequester;
import de.danoeh.antennapodSA.core.syndication.handler.FeedHandlerResult;

public class FeedSyncTask {
    private static final String TAG = "FeedParserTask";
    private final DownloadRequest request;
    private final Context context;
    private DownloadStatus downloadStatus;

    public FeedSyncTask(Context context, DownloadRequest request) {
        this.request = request;
        this.context = context;
    }

    public boolean run() {
        FeedParserTask task = new FeedParserTask(request);
        FeedHandlerResult result = task.call();
        downloadStatus = task.getDownloadStatus();

        if (!task.isSuccessful()) {
            return false;
        }

        Feed[] savedFeeds = DBTasks.updateFeed(context, result.feed);
        Feed savedFeed = savedFeeds[0];
        // If loadAllPages=true, check if another page is available and queue it for download
        final boolean loadAllPages = request.getArguments().getBoolean(DownloadRequester.REQUEST_ARG_LOAD_ALL_PAGES);
        final Feed feed = result.feed;
        if (loadAllPages && feed.getNextPageLink() != null) {
            try {
                feed.setId(savedFeed.getId());
                DBTasks.loadNextPageOfFeed(context, savedFeed, true);
            } catch (DownloadRequestException e) {
                Log.e(TAG, "Error trying to load next page", e);
            }
        }

        ClientConfig.downloadServiceCallbacks.onFeedParsed(context, savedFeed);
        return true;
    }

    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }
}
