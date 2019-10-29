package de.danoeh.antennapod.core.service.download.handler;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.syndication.handler.FeedHandlerResult;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Takes a single Feed, parses the corresponding file and refreshes
 * information in the manager.
 */
public class FeedSyncThread extends Thread {
    private static final String TAG = "FeedSyncThread";
    private static final long WAIT_TIMEOUT = 3000;

    private final BlockingQueue<DownloadRequest> completedRequests = new LinkedBlockingDeque<>();
    private final CompletionService<Pair<DownloadRequest, FeedHandlerResult>> parserService =
            new ExecutorCompletionService<>(Executors.newSingleThreadExecutor());
    private final ExecutorService dbService = Executors.newSingleThreadExecutor();
    private final Context context;
    private Future<?> dbUpdateFuture;
    private final FeedSyncCallback feedSyncCallback;
    private volatile boolean isActive = true;
    private volatile boolean isCollectingRequests = false;

    public FeedSyncThread(Context context, FeedSyncCallback feedSyncCallback) {
        super("FeedSyncThread");
        this.context = context;
        this.feedSyncCallback = feedSyncCallback;
    }

    /**
     * Waits for completed requests. Once the first request has been taken, the method will wait WAIT_TIMEOUT ms longer to
     * collect more completed requests.
     *
     * @return Collected feeds or null if the method has been interrupted during the first waiting period.
     */
    private List<Pair<DownloadRequest, FeedHandlerResult>> collectCompletedRequests() {
        List<Pair<DownloadRequest, FeedHandlerResult>> results = new LinkedList<>();
        DownloadRequester requester = DownloadRequester.getInstance();
        int tasks = 0;

        try {
            DownloadRequest request = completedRequests.take();
            submitParseRequest(request);
            tasks++;
        } catch (InterruptedException e) {
            Log.e(TAG, "FeedSyncThread was interrupted");
            return null;
        }

        tasks += pollCompletedDownloads();

        isCollectingRequests = true;

        if (requester.isDownloadingFeeds()) {
            // wait for completion of more downloads
            long startTime = System.currentTimeMillis();
            long currentTime = startTime;
            while (requester.isDownloadingFeeds() && (currentTime - startTime) < WAIT_TIMEOUT) {
                try {
                    Log.d(TAG, "Waiting for " + (startTime + WAIT_TIMEOUT - currentTime) + " ms");
                    sleep(startTime + WAIT_TIMEOUT - currentTime);
                } catch (InterruptedException e) {
                    Log.d(TAG, "interrupted while waiting for more downloads");
                    tasks += pollCompletedDownloads();
                } finally {
                    currentTime = System.currentTimeMillis();
                }
            }

            tasks += pollCompletedDownloads();

        }

        isCollectingRequests = false;

        for (int i = 0; i < tasks; i++) {
            try {
                Pair<DownloadRequest, FeedHandlerResult> result = parserService.take().get();
                if (result != null) {
                    results.add(result);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "FeedSyncThread was interrupted");
            } catch (ExecutionException e) {
                Log.e(TAG, "ExecutionException in FeedSyncThread: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return results;
    }

    private void submitParseRequest(DownloadRequest request) {
        parserService.submit(() -> {
            FeedParserTask task = new FeedParserTask(request);
            FeedHandlerResult result = task.call();

            if (task.isSuccessful()) {
                // we create a 'successful' download log if the feed's last refresh failed
                List<DownloadStatus> log = DBReader.getFeedDownloadLog(request.getFeedfileId());
                if (log.size() > 0 && !log.get(0).isSuccessful()) {
                    feedSyncCallback.downloadStatusGenerated(task.getDownloadStatus());
                }
                return Pair.create(request, result);
            } else {
                feedSyncCallback.failedSyncingFeed();
                feedSyncCallback.downloadStatusGenerated(task.getDownloadStatus());
                return null;
            }
        });
    }

    private int pollCompletedDownloads() {
        int tasks = 0;
        while (!completedRequests.isEmpty()) {
            DownloadRequest request = completedRequests.poll();
            submitParseRequest(request);
            tasks++;
        }
        return tasks;
    }

    @Override
    public void run() {
        while (isActive) {
            final List<Pair<DownloadRequest, FeedHandlerResult>> results = collectCompletedRequests();

            if (results == null) {
                continue;
            }

            Log.d(TAG, "Bundling " + results.size() + " feeds");

            // Save information of feed in DB
            if (dbUpdateFuture != null) {
                try {
                    dbUpdateFuture.get();
                } catch (InterruptedException e) {
                    Log.e(TAG, "FeedSyncThread was interrupted");
                } catch (ExecutionException e) {
                    Log.e(TAG, "ExecutionException in FeedSyncThread: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            dbUpdateFuture = dbService.submit(() -> {
                Feed[] savedFeeds = DBTasks.updateFeed(context, getFeeds(results));

                for (int i = 0; i < savedFeeds.length; i++) {
                    Feed savedFeed = savedFeeds[i];

                    // If loadAllPages=true, check if another page is available and queue it for download
                    final boolean loadAllPages = results.get(i).first.getArguments()
                            .getBoolean(DownloadRequester.REQUEST_ARG_LOAD_ALL_PAGES);
                    final Feed feed = results.get(i).second.feed;
                    if (loadAllPages && feed.getNextPageLink() != null) {
                        try {
                            feed.setId(savedFeed.getId());
                            DBTasks.loadNextPageOfFeed(context, savedFeed, true);
                        } catch (DownloadRequestException e) {
                            Log.e(TAG, "Error trying to load next page", e);
                        }
                    }

                    ClientConfig.downloadServiceCallbacks.onFeedParsed(context, savedFeed);
                }
                feedSyncCallback.finishedSyncingFeeds(savedFeeds.length);
            });

        }

        if (dbUpdateFuture != null) {
            try {
                dbUpdateFuture.get();
            } catch (InterruptedException e) {
                Log.e(TAG, "interrupted while updating the db");
            } catch (ExecutionException e) {
                Log.e(TAG, "ExecutionException while updating the db: " + e.getMessage());
            }
        }

        Log.d(TAG, "Shutting down");
    }

    private Feed[] getFeeds(List<Pair<DownloadRequest, FeedHandlerResult>> results) {
        Feed[] feeds = new Feed[results.size()];
        for (int i = 0; i < results.size(); i++) {
            feeds[i] = results.get(i).second.feed;
        }
        return feeds;
    }

    public void shutdown() {
        isActive = false;
        if (isCollectingRequests) {
            interrupt();
        }
    }

    public void submitCompletedDownload(DownloadRequest request) {
        completedRequests.offer(request);
        if (isCollectingRequests) {
            interrupt();
        }
    }

    public interface FeedSyncCallback {
        void finishedSyncingFeeds(int numberOfCompletedFeeds);
        void failedSyncingFeed();
        void downloadStatusGenerated(DownloadStatus downloadStatus);
    }

}