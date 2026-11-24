package de.danoeh.antennapod.storage.importexport;

import android.content.Context;
import android.util.Log;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Helpers for detecting and clearing stale download markers: episodes the database thinks are
 * downloaded but whose files are missing from disk. Typically used after a database import.
 */
public class StaleDownloadMarkers {
    private static final String TAG = "StaleDownloadMarkers";

    private StaleDownloadMarkers() {
    }

    public static void checkMissingFilesAfterImport(Consumer<List<Long>> missingMediaIdsConsumer) {
        Single.fromCallable(() -> {
            clearStaleDownloadsFromUnsubscribedFeeds();
            return findMissingMediaIds();
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(missingMediaIdsConsumer,
                        error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private static List<Long> findMissingMediaIds() {
        List<FeedItem> downloadedItems = DBReader.getEpisodes(0, Integer.MAX_VALUE,
                new FeedItemFilter(FeedItemFilter.DOWNLOADED), SortOrder.DATE_NEW_OLD);
        List<Long> missingIds = new ArrayList<>();
        for (FeedItem item : downloadedItems) {
            if (isLocalFeedItem(item)) {
                continue;
            }
            if (item.getMedia() != null && !item.getMedia().fileExists()) {
                missingIds.add(item.getMedia().getId());
            }
        }
        return missingIds;
    }

    /**
     * Items of local folder feeds always count as downloaded and their media URL is a content URI
     * rather than a file path, so the file system check does not apply to them. They can also not
     * be re-downloaded.
     */
    private static boolean isLocalFeedItem(FeedItem item) {
        return item.getFeed() != null && item.getFeed().isLocalFeed();
    }

    /**
     * Reconcile download markers for episodes whose feed is not subscribed (i.e. removed or
     * archived) and whose file is missing on disk. We don't want to re-download  these,
     * so we silently clear the marker. We leave items alone if the file still exists.
     */
    private static void clearStaleDownloadsFromUnsubscribedFeeds()
            throws ExecutionException, InterruptedException {
        List<FeedItem> downloadedItems = DBReader.getEpisodes(0, Integer.MAX_VALUE,
                new FeedItemFilter(FeedItemFilter.DOWNLOADED, FeedItemFilter.INCLUDE_NOT_SUBSCRIBED,
                        FeedItemFilter.INCLUDE_ARCHIVED),
                SortOrder.DATE_NEW_OLD);
        List<Long> missingIds = new ArrayList<>();
        for (FeedItem item : downloadedItems) {
            if (item.getFeed() == null || item.getFeed().getState() == Feed.STATE_SUBSCRIBED) {
                continue;
            }
            if (isLocalFeedItem(item)) {
                continue;
            }
            if (item.getMedia() != null && !item.getMedia().fileExists()) {
                missingIds.add(item.getMedia().getId());
            }
        }
        if (!missingIds.isEmpty()) {
            clearDownloadStateForItems(missingIds);
        }
    }

    private static void clearDownloadStateForItems(List<Long> mediaIds)
            throws ExecutionException, InterruptedException {
        DBWriter.clearDownloadStateForMediaIds(mediaIds).get();
    }

    public static void clearDownloadStateForItemsAsync(List<Long> mediaIds) {
        Completable.fromAction(() -> clearDownloadStateForItems(mediaIds))
                .subscribeOn(Schedulers.computation())
                .subscribe(() -> { }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    public static void redownloadItems(Context context, List<Long> mediaIds) {
        Completable.fromAction(() -> {
            clearDownloadStateForItems(mediaIds);
            for (Long mediaId : mediaIds) {
                FeedMedia media = DBReader.getFeedMedia(mediaId);
                if (media != null && media.getItem() != null) {
                    DownloadServiceInterface.get().download(context, media.getItem(), true);
                }
            }
        })
                .subscribeOn(Schedulers.computation())
                .subscribe(() -> { }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
