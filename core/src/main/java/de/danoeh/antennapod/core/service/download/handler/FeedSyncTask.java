package de.danoeh.antennapod.core.service.download.handler;

import android.content.Context;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadRequest;
import de.danoeh.antennapod.parser.feed.FeedHandlerResult;

public class FeedSyncTask {
    private final Context context;
    private Feed savedFeed;
    private final FeedParserTask task;
    private FeedHandlerResult feedHandlerResult;

    public FeedSyncTask(Context context, DownloadRequest request) {
        this.context = context;
        this.task = new FeedParserTask(request);
    }

    public boolean run() {
        feedHandlerResult = task.call();
        if (!task.isSuccessful()) {
            return false;
        }

        savedFeed = DBTasks.updateFeed(context, feedHandlerResult.feed, false);
        return true;
    }

    @NonNull
    public DownloadResult getDownloadStatus() {
        return task.getDownloadStatus();
    }

    public Feed getSavedFeed() {
        return savedFeed;
    }

    public String getRedirectUrl() {
        return feedHandlerResult.redirectUrl;
    }
}
