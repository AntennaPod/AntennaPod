package de.danoeh.antennapod.core.service.download.handler;

import android.content.Context;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.model.download.DownloadStatus;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.parser.feed.FeedHandlerResult;

public class FeedSyncTask {
    private final DownloadRequest request;
    private final Context context;
    private DownloadStatus downloadStatus;
    private Feed savedFeed;

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

        savedFeed = DBTasks.updateFeed(context, result.feed, false);
        // If loadAllPages=true, check if another page is available and queue it for download
        final boolean loadAllPages = request.getArguments().getBoolean(DownloadRequest.REQUEST_ARG_LOAD_ALL_PAGES);
        final Feed feed = result.feed;
        if (loadAllPages && feed.getNextPageLink() != null) {
            feed.setId(savedFeed.getId());
            DBTasks.loadNextPageOfFeed(context, feed, true);
        }
        return true;
    }

    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }

    public Feed getSavedFeed() {
        return savedFeed;
    }
}
