package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.asynctask.FlattrClickWorker;
import de.danoeh.antennapod.core.asynctask.FlattrStatusFetcher;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.GpodnetSyncService;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.DownloadError;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.core.util.comparator.FeedItemPubdateComparator;
import de.danoeh.antennapod.core.util.exception.MediaFileNotFoundException;
import de.danoeh.antennapod.core.util.flattr.FlattrUtils;

import static android.content.Context.MODE_PRIVATE;

/**
 * Provides methods for doing common tasks that use DBReader and DBWriter.
 */
public final class DBTasks {
    private static final String TAG = "DBTasks";

    private static final String PREF_NAME = "dbtasks";
    private static final String PREF_LAST_REFRESH = "last_refresh";

    /**
     * Executor service used by the autodownloadUndownloadedEpisodes method.
     */
    private static final ExecutorService autodownloadExec;

    static {
        autodownloadExec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }

    private DBTasks() {
    }

    /**
     * Removes the feed with the given download url. This method should NOT be executed on the GUI thread.
     *
     * @param context     Used for accessing the db
     * @param downloadUrl URL of the feed.
     */
    public static void removeFeedWithDownloadUrl(Context context, String downloadUrl) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        Cursor cursor = adapter.getFeedCursorDownloadUrls();
        long feedID = 0;
        if (cursor.moveToFirst()) {
            do {
                if (cursor.getString(1).equals(downloadUrl)) {
                    feedID = cursor.getLong(0);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        adapter.close();

        if (feedID != 0) {
            try {
                DBWriter.deleteFeed(context, feedID).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "removeFeedWithDownloadUrl: Could not find feed with url: " + downloadUrl);
        }
    }

    /**
     * Starts playback of a FeedMedia object's file. This method will build an Intent based on the given parameters to
     * start the {@link PlaybackService}.
     *
     * @param context           Used for sending starting Services and Activities.
     * @param media             The FeedMedia object.
     * @param showPlayer        If true, starts the appropriate player activity ({@link de.danoeh.antennapod.activity.AudioplayerActivity}
     *                          or {@link de.danoeh.antennapod.activity.VideoplayerActivity}
     * @param startWhenPrepared Parameter for the {@link PlaybackService} start intent. If true, playback will start as
     *                          soon as the PlaybackService has finished loading the FeedMedia object's file.
     * @param shouldStream      Parameter for the {@link PlaybackService} start intent. If true, the FeedMedia object's file
     *                          will be streamed, otherwise the downloaded file will be used. If the downloaded file cannot be
     *                          found, the PlaybackService will shutdown and the database entry of the FeedMedia object will be
     *                          corrected.
     */
    public static void playMedia(final Context context, final FeedMedia media,
                                 boolean showPlayer, boolean startWhenPrepared, boolean shouldStream) {
        try {
            if (!shouldStream) {
                if (!media.fileExists()) {
                    throw new MediaFileNotFoundException(
                            "No episode was found at " + media.getFile_url(),
                            media);
                }
            }
            // Start playback Service
            Intent launchIntent = new Intent(context, PlaybackService.class);
            launchIntent.putExtra(PlaybackService.EXTRA_PLAYABLE, media);
            launchIntent.putExtra(PlaybackService.EXTRA_START_WHEN_PREPARED,
                    startWhenPrepared);
            launchIntent.putExtra(PlaybackService.EXTRA_SHOULD_STREAM,
                    shouldStream);
            launchIntent.putExtra(PlaybackService.EXTRA_PREPARE_IMMEDIATELY,
                    true);
            context.startService(launchIntent);
            if (showPlayer) {
                // Launch media player
                context.startActivity(PlaybackService.getPlayerActivityIntent(
                        context, media));
            }
            DBWriter.addQueueItemAt(context, media.getItem().getId(), 0, false);
        } catch (MediaFileNotFoundException e) {
            e.printStackTrace();
            if (media.isPlaying()) {
                context.sendBroadcast(new Intent(
                        PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE));
            }
            notifyMissingFeedMediaFile(context, media);
        }
    }

    private static final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    /**
     * Refreshes a given list of Feeds in a separate Thread. This method might ignore subsequent calls if it is still
     * enqueuing Feeds for download from a previous call
     *
     * @param context Might be used for accessing the database
     * @param feeds   List of Feeds that should be refreshed.
     */
    public static void refreshAllFeeds(final Context context,
                                       final List<Feed> feeds) {
        if (isRefreshing.compareAndSet(false, true)) {
            new Thread() {
                public void run() {
                    if (feeds != null) {
                        refreshFeeds(context, feeds);
                    } else {
                        refreshFeeds(context, DBReader.getFeedList());
                    }
                    isRefreshing.set(false);

                    SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                    prefs.edit().putLong(PREF_LAST_REFRESH, System.currentTimeMillis()).apply();

                    if (FlattrUtils.hasToken()) {
                        Log.d(TAG, "Flattring all pending things.");
                        new FlattrClickWorker(context).executeAsync(); // flattr pending things

                        Log.d(TAG, "Fetching flattr status.");
                        new FlattrStatusFetcher(context).start();

                    }
                    if (ClientConfig.gpodnetCallbacks.gpodnetEnabled()) {
                        GpodnetSyncService.sendSyncIntent(context);
                    }
                    Log.d(TAG, "refreshAllFeeds autodownload");
                    autodownloadUndownloadedItems(context);
                }
            }.start();
        } else {
            Log.d(TAG, "Ignoring request to refresh all feeds: Refresh lock is locked");
        }
    }

    /**
     * @param context
     * @param feedList the list of feeds to refresh
     */
    private static void refreshFeeds(final Context context,
                                     final List<Feed> feedList) {

        for (Feed feed : feedList) {
            FeedPreferences prefs = feed.getPreferences();
            // feeds with !getKeepUpdated can only be refreshed
            // directly from the FeedActivity
            if (prefs.getKeepUpdated()) {
                try {
                    refreshFeed(context, feed);
                } catch (DownloadRequestException e) {
                    e.printStackTrace();
                    DBWriter.addDownloadStatus(
                            new DownloadStatus(feed, feed
                                    .getHumanReadableIdentifier(),
                                    DownloadError.ERROR_REQUEST_ERROR, false, e
                                    .getMessage()
                            )
                    );
                }
            }
        }

    }

    /**
     * Downloads all pages of the given feed.
     *
     * @param context Used for requesting the download.
     * @param feed    The Feed object.
     */
    public static void refreshCompleteFeed(final Context context, final Feed feed) {
        try {
            refreshFeed(context, feed, true, false);
        } catch (DownloadRequestException e) {
            e.printStackTrace();
            DBWriter.addDownloadStatus(
                    new DownloadStatus(feed, feed
                            .getHumanReadableIdentifier(),
                            DownloadError.ERROR_REQUEST_ERROR, false, e
                            .getMessage()
                    )
            );
        }
    }

    /**
     * Downloads all pages of the given feed even if feed has not been modified since last refresh
     *
     * @param context Used for requesting the download.
     * @param feed    The Feed object.
     */
    public static void forceRefreshCompleteFeed(final Context context, final Feed feed) {
        try {
            refreshFeed(context, feed, true, true);
        } catch (DownloadRequestException e) {
            e.printStackTrace();
            DBWriter.addDownloadStatus(
                    new DownloadStatus(feed, feed
                            .getHumanReadableIdentifier(),
                            DownloadError.ERROR_REQUEST_ERROR, false, e
                            .getMessage()
                    )
            );
        }
    }

    /**
     * Queues the next page of this Feed for download. The given Feed has to be a paged
     * Feed (isPaged()=true) and must contain a nextPageLink.
     *
     * @param context      Used for requesting the download.
     * @param feed         The feed whose next page should be loaded.
     * @param loadAllPages True if any subsequent pages should also be loaded, false otherwise.
     */
    public static void loadNextPageOfFeed(final Context context, Feed feed, boolean loadAllPages) throws DownloadRequestException {
        if (feed.isPaged() && feed.getNextPageLink() != null) {
            int pageNr = feed.getPageNr() + 1;
            Feed nextFeed = new Feed(feed.getNextPageLink(), null, feed.getTitle() + "(" + pageNr + ")");
            nextFeed.setPageNr(pageNr);
            nextFeed.setPaged(true);
            nextFeed.setId(feed.getId());
            DownloadRequester.getInstance().downloadFeed(context, nextFeed, loadAllPages, false);
        } else {
            Log.e(TAG, "loadNextPageOfFeed: Feed was either not paged or contained no nextPageLink");
        }
    }

    /**
     * Refresh a specific Feed. The refresh may get canceled if the feed does not seem to be modified
     * and the last update was only few days ago.
     *
     * @param context Used for requesting the download.
     * @param feed    The Feed object.
     */
    private static void refreshFeed(Context context, Feed feed)
            throws DownloadRequestException {
        Log.d(TAG, "refreshFeed(feed.id: " + feed.getId() +")");
        refreshFeed(context, feed, false, false);
    }

    /**
     * Refresh a specific feed even if feed has not been modified since last refresh
     *
     * @param context Used for requesting the download.
     * @param feed    The Feed object.
     */
    public static void forceRefreshFeed(Context context, Feed feed)
            throws DownloadRequestException {
        Log.d(TAG, "refreshFeed(feed.id: " + feed.getId() +")");
        refreshFeed(context, feed, false, true);
    }

    private static void refreshFeed(Context context, Feed feed, boolean loadAllPages, boolean force)
            throws DownloadRequestException {
        Feed f;
        String lastUpdate = feed.hasLastUpdateFailed() ? null : feed.getLastUpdate();
        if (feed.getPreferences() == null) {
            f = new Feed(feed.getDownload_url(), lastUpdate, feed.getTitle());
        } else {
            f = new Feed(feed.getDownload_url(), lastUpdate, feed.getTitle(),
                    feed.getPreferences().getUsername(), feed.getPreferences().getPassword());
        }
        f.setId(feed.getId());
        DownloadRequester.getInstance().downloadFeed(context, f, loadAllPages, force);
    }

    /*
     *  Checks if the app should refresh all feeds, i.e. if the last auto refresh failed.
     *
     *  The feeds are only refreshed if an update interval or time of day is set and the last
     *  (successful) refresh was before the last interval or more than a day ago, respectively.
     */
    public static void checkShouldRefreshFeeds(Context context) {
        long interval = 0;
        if(UserPreferences.getUpdateInterval() > 0) {
            interval = UserPreferences.getUpdateInterval();
        } else if(UserPreferences.getUpdateTimeOfDay().length > 0){
            interval = TimeUnit.DAYS.toMillis(1);
        }
        if(interval == 0) { // auto refresh is disabled
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        long lastRefresh = prefs.getLong(PREF_LAST_REFRESH, 0);
        Log.d(TAG, "last refresh: " + Converter.getDurationStringLocalized(context,
                System.currentTimeMillis() - lastRefresh) + " ago");
        if(lastRefresh <= System.currentTimeMillis() - interval) {
            DBTasks.refreshAllFeeds(context, null);
        }
    }

    /**
     * Notifies the database about a missing FeedMedia file. This method will correct the FeedMedia object's values in the
     * DB and send a FeedUpdateBroadcast.
     */
    public static void notifyMissingFeedMediaFile(final Context context,
                                                  final FeedMedia media) {
        Log.i(TAG,
                "The feedmanager was notified about a missing episode. It will update its database now.");
        media.setDownloaded(false);
        media.setFile_url(null);
        DBWriter.setFeedMedia(media);
        EventDistributor.getInstance().sendFeedUpdateBroadcast();
    }

    /**
     * Request the download of all objects in the queue. from a separate Thread.
     *
     * @param context Used for requesting the download an accessing the database.
     */
    public static void downloadAllItemsInQueue(final Context context) {
        new Thread() {
            public void run() {
                List<FeedItem> queue = DBReader.getQueue();
                if (!queue.isEmpty()) {
                    try {
                        downloadFeedItems(context,
                                queue.toArray(new FeedItem[queue.size()]));
                    } catch (DownloadRequestException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    /**
     * Requests the download of a list of FeedItem objects.
     *
     * @param context Used for requesting the download and accessing the DB.
     * @param items   The FeedItem objects.
     */
    public static void downloadFeedItems(final Context context,
                                         FeedItem... items) throws DownloadRequestException {
        downloadFeedItems(true, context, items);
    }

    static void downloadFeedItems(boolean performAutoCleanup,
                                  final Context context, final FeedItem... items)
            throws DownloadRequestException {
        final DownloadRequester requester = DownloadRequester.getInstance();

        if (performAutoCleanup) {
            new Thread() {

                @Override
                public void run() {
                    ClientConfig.dbTasksCallbacks.getEpisodeCacheCleanupAlgorithm()
                            .makeRoomForEpisodes(context, items.length);
                }

            }.start();
        }
        for (FeedItem item : items) {
            if (item.getMedia() != null
                    && !requester.isDownloadingFile(item.getMedia())
                    && !item.getMedia().isDownloaded()) {
                if (items.length > 1) {
                    try {
                        requester.downloadMedia(context, item.getMedia());
                    } catch (DownloadRequestException e) {
                        e.printStackTrace();
                        DBWriter.addDownloadStatus(
                                new DownloadStatus(item.getMedia(), item
                                        .getMedia()
                                        .getHumanReadableIdentifier(),
                                        DownloadError.ERROR_REQUEST_ERROR,
                                        false, e.getMessage()
                                )
                        );
                    }
                } else {
                    requester.downloadMedia(context, item.getMedia());
                }
            }
        }
    }

    /**
     * Looks for undownloaded episodes in the queue or list of unread items and request a download if
     * 1. Network is available
     * 2. The device is charging or the user allows auto download on battery
     * 3. There is free space in the episode cache
     * This method is executed on an internal single thread executor.
     *
     * @param context  Used for accessing the DB.
     * @return A Future that can be used for waiting for the methods completion.
     */
    public static Future<?> autodownloadUndownloadedItems(final Context context) {
        Log.d(TAG, "autodownloadUndownloadedItems");
        return autodownloadExec.submit(ClientConfig.dbTasksCallbacks.getAutomaticDownloadAlgorithm()
                .autoDownloadUndownloadedItems(context));

    }

    /**
     * Removed downloaded episodes outside of the queue if the episode cache is full. Episodes with a smaller
     * 'playbackCompletionDate'-value will be deleted first.
     * <p/>
     * This method should NOT be executed on the GUI thread.
     *
     * @param context Used for accessing the DB.
     */
    public static void performAutoCleanup(final Context context) {
        ClientConfig.dbTasksCallbacks.getEpisodeCacheCleanupAlgorithm().performCleanup(context);
    }

    /**
     * Returns the successor of a FeedItem in the queue.
     *
     * @param itemId  ID of the FeedItem
     * @param queue   Used for determining the successor of the item. If this parameter is null, the method will load
     *                the queue from the database in the same thread.
     * @return Successor of the FeedItem or null if the FeedItem is not in the queue or has no successor.
     */
    public static FeedItem getQueueSuccessorOfItem(final long itemId, List<FeedItem> queue) {
        FeedItem result = null;
        if (queue == null) {
            queue = DBReader.getQueue();
        }
        if (queue != null) {
            Iterator<FeedItem> iterator = queue.iterator();
            while (iterator.hasNext()) {
                FeedItem item = iterator.next();
                if (item.getId() == itemId) {
                    if (iterator.hasNext()) {
                        result = iterator.next();
                    }
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Loads the queue from the database and checks if the specified FeedItem is in the queue.
     * This method should NOT be executed in the GUI thread.
     *
     * @param context    Used for accessing the DB.
     * @param feedItemId ID of the FeedItem
     */
    public static boolean isInQueue(Context context, final long feedItemId) {
        LongList queue = DBReader.getQueueIDList();
        return queue.contains(feedItemId);
    }

    private static Feed searchFeedByIdentifyingValueOrID(PodDBAdapter adapter,
                                                         Feed feed) {
        if (feed.getId() != 0) {
            return DBReader.getFeed(feed.getId(), adapter);
        } else {
            List<Feed> feeds = DBReader.getFeedList();
            for (Feed f : feeds) {
                if (f.getIdentifyingValue().equals(feed.getIdentifyingValue())) {
                    f.setItems(DBReader.getFeedItemList(f));
                    return f;
                }
            }
        }
        return null;
    }

    /**
     * Get a FeedItem by its identifying value.
     */
    private static FeedItem searchFeedItemByIdentifyingValue(Feed feed,
                                                             String identifier) {
        for (FeedItem item : feed.getItems()) {
            if (item.getIdentifyingValue().equals(identifier)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Adds new Feeds to the database or updates the old versions if they already exists. If another Feed with the same
     * identifying value already exists, this method will add new FeedItems from the new Feed to the existing Feed.
     * These FeedItems will be marked as unread with the exception of the most recent FeedItem.
     * <p/>
     * This method can update multiple feeds at once. Submitting a feed twice in the same method call can result in undefined behavior.
     * <p/>
     * This method should NOT be executed on the GUI thread.
     *
     * @param context  Used for accessing the DB.
     * @param newFeeds The new Feed objects.
     * @return The updated Feeds from the database if it already existed, or the new Feed from the parameters otherwise.
     */
    public static synchronized Feed[] updateFeed(final Context context,
                                                 final Feed... newFeeds) {
        List<Feed> newFeedsList = new ArrayList<>();
        List<Feed> updatedFeedsList = new ArrayList<>();
        Feed[] resultFeeds = new Feed[newFeeds.length];
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();

        for (int feedIdx = 0; feedIdx < newFeeds.length; feedIdx++) {

            final Feed newFeed = newFeeds[feedIdx];

            // Look up feed in the feedslist
            final Feed savedFeed = searchFeedByIdentifyingValueOrID(adapter,
                    newFeed);
            if (savedFeed == null) {
                Log.d(TAG, "Found no existing Feed with title "
                                + newFeed.getTitle() + ". Adding as new one.");

                // Add a new Feed
                // all new feeds will have the most recent item marked as unplayed
                FeedItem mostRecent = newFeed.getMostRecentItem();
                if (mostRecent != null) {
                    mostRecent.setNew();
                }

                newFeedsList.add(newFeed);
                resultFeeds[feedIdx] = newFeed;
            } else {
                Log.d(TAG, "Feed with title " + newFeed.getTitle()
                            + " already exists. Syncing new with existing one.");

                Collections.sort(newFeed.getItems(), new FeedItemPubdateComparator());

                if (newFeed.getPageNr() == savedFeed.getPageNr()) {
                    if (savedFeed.compareWithOther(newFeed)) {
                        Log.d(TAG, "Feed has updated attribute values. Updating old feed's attributes");
                        savedFeed.updateFromOther(newFeed);
                    }
                } else {
                    Log.d(TAG, "New feed has a higher page number.");
                    savedFeed.setNextPageLink(newFeed.getNextPageLink());
                }
                if (savedFeed.getPreferences().compareWithOther(newFeed.getPreferences())) {
                    Log.d(TAG, "Feed has updated preferences. Updating old feed's preferences");
                    savedFeed.getPreferences().updateFromOther(newFeed.getPreferences());
                }

                // get the most recent date now, before we start changing the list
                FeedItem priorMostRecent = savedFeed.getMostRecentItem();
                Date priorMostRecentDate = null;
                if (priorMostRecent != null) {
                    priorMostRecentDate = priorMostRecent.getPubDate();
                }

                // Look for new or updated Items
                for (int idx = 0; idx < newFeed.getItems().size(); idx++) {
                    final FeedItem item = newFeed.getItems().get(idx);
                    FeedItem oldItem = searchFeedItemByIdentifyingValue(savedFeed,
                            item.getIdentifyingValue());
                    if (oldItem == null) {
                        // item is new
                        item.setFeed(savedFeed);
                        item.setAutoDownload(savedFeed.getPreferences().getAutoDownload());
                        savedFeed.getItems().add(idx, item);

                        // only mark the item new if it was published after or at the same time
                        // as the most recent item
                        // (if the most recent date is null then we can assume there are no items
                        // and this is the first, hence 'new')
                        if (priorMostRecentDate == null ||
                                priorMostRecentDate.before(item.getPubDate()) ||
                                priorMostRecentDate.equals(item.getPubDate())) {
                            Log.d(TAG, "Marking item published on " + item.getPubDate() +
                                    " new, prior most recent date = " + priorMostRecentDate);
                            item.setNew();
                        }
                    } else {
                        oldItem.updateFromOther(item);
                    }
                }
                // update attributes
                savedFeed.setLastUpdate(newFeed.getLastUpdate());
                savedFeed.setType(newFeed.getType());
                savedFeed.setLastUpdateFailed(false);

                updatedFeedsList.add(savedFeed);
                resultFeeds[feedIdx] = savedFeed;
            }
        }

        adapter.close();

        try {
            DBWriter.addNewFeed(context, newFeedsList.toArray(new Feed[newFeedsList.size()])).get();
            DBWriter.setCompleteFeed(updatedFeedsList.toArray(new Feed[updatedFeedsList.size()])).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        EventDistributor.getInstance().sendFeedUpdateBroadcast();

        return resultFeeds;
    }

    /**
     * Searches the titles of FeedItems of a specific Feed for a given
     * string.
     *
     * @param context Used for accessing the DB.
     * @param feedID  The id of the feed whose items should be searched.
     * @param query   The search string.
     * @return A FutureTask object that executes the search request and returns the search result as a List of FeedItems.
     */
    public static FutureTask<List<FeedItem>> searchFeedItemTitle(final Context context,
                                                                 final long feedID, final String query) {
        return new FutureTask<>(new QueryTask<List<FeedItem>>(context) {
            @Override
            public void execute(PodDBAdapter adapter) {
                Cursor searchResult = adapter.searchItemTitles(feedID,
                        query);
                List<FeedItem> items = DBReader.extractItemlistFromCursor(searchResult);
                DBReader.loadAdditionalFeedItemListData(items);
                setResult(items);
                searchResult.close();
            }
        });
    }

    /**
     * Searches the authors of FeedItems of a specific Feed for a given
     * string.
     *
     * @param context Used for accessing the DB.
     * @param feedID  The id of the feed whose items should be searched.
     * @param query   The search string.
     * @return A FutureTask object that executes the search request and returns the search result as a List of FeedItems.
     */
    public static FutureTask<List<FeedItem>> searchFeedItemAuthor(final Context context,
                                                                 final long feedID, final String query) {
        return new FutureTask<>(new QueryTask<List<FeedItem>>(context) {
            @Override
            public void execute(PodDBAdapter adapter) {
                Cursor searchResult = adapter.searchItemAuthors(feedID,
                        query);
                List<FeedItem> items = DBReader.extractItemlistFromCursor(searchResult);
                DBReader.loadAdditionalFeedItemListData(items);
                setResult(items);
                searchResult.close();
            }
        });
    }

    /**
     * Searches the feed identifiers of FeedItems of a specific Feed for a given
     * string.
     *
     * @param context Used for accessing the DB.
     * @param feedID  The id of the feed whose items should be searched.
     * @param query   The search string.
     * @return A FutureTask object that executes the search request and returns the search result as a List of FeedItems.
     */
    public static FutureTask<List<FeedItem>> searchFeedItemFeedIdentifier(final Context context,
                                                                 final long feedID, final String query) {
        return new FutureTask<>(new QueryTask<List<FeedItem>>(context) {
            @Override
            public void execute(PodDBAdapter adapter) {
                Cursor searchResult = adapter.searchItemFeedIdentifiers(feedID,
                        query);
                List<FeedItem> items = DBReader.extractItemlistFromCursor(searchResult);
                DBReader.loadAdditionalFeedItemListData(items);
                setResult(items);
                searchResult.close();
            }
        });
    }

    /**
     * Searches the descriptions of FeedItems of a specific Feed for a given
     * string.
     *
     * @param context Used for accessing the DB.
     * @param feedID  The id of the feed whose items should be searched.
     * @param query   The search string
     * @return A FutureTask object that executes the search request and returns the search result as a List of FeedItems.
     */
    public static FutureTask<List<FeedItem>> searchFeedItemDescription(final Context context,
                                                                       final long feedID, final String query) {
        return new FutureTask<>(new QueryTask<List<FeedItem>>(context) {
            @Override
            public void execute(PodDBAdapter adapter) {
                Cursor searchResult = adapter.searchItemDescriptions(feedID,
                        query);
                List<FeedItem> items = DBReader.extractItemlistFromCursor(searchResult);
                DBReader.loadAdditionalFeedItemListData(items);
                setResult(items);
                searchResult.close();
            }
        });
    }

    /**
     * Searches the contentEncoded-value of FeedItems of a specific Feed for a given
     * string.
     *
     * @param context Used for accessing the DB.
     * @param feedID  The id of the feed whose items should be searched.
     * @param query   The search string
     * @return A FutureTask object that executes the search request and returns the search result as a List of FeedItems.
     */
    public static FutureTask<List<FeedItem>> searchFeedItemContentEncoded(final Context context,
                                                                          final long feedID, final String query) {
        return new FutureTask<>(new QueryTask<List<FeedItem>>(context) {
            @Override
            public void execute(PodDBAdapter adapter) {
                Cursor searchResult = adapter.searchItemContentEncoded(feedID,
                        query);
                List<FeedItem> items = DBReader.extractItemlistFromCursor(searchResult);
                DBReader.loadAdditionalFeedItemListData(items);
                setResult(items);
                searchResult.close();
            }
        });
    }

    /**
     * Searches chapters of the FeedItems of a specific Feed for a given string.
     *
     * @param context Used for accessing the DB.
     * @param feedID  The id of the feed whose items should be searched.
     * @param query   The search string
     * @return A FutureTask object that executes the search request and returns the search result as a List of FeedItems.
     */
    public static FutureTask<List<FeedItem>> searchFeedItemChapters(final Context context,
                                                                    final long feedID, final String query) {
        return new FutureTask<>(new QueryTask<List<FeedItem>>(context) {
            @Override
            public void execute(PodDBAdapter adapter) {
                Cursor searchResult = adapter.searchItemChapters(feedID,
                        query);
                List<FeedItem> items = DBReader.extractItemlistFromCursor(searchResult);
                DBReader.loadAdditionalFeedItemListData(items);
                setResult(items);
                searchResult.close();
            }
        });
    }

    /**
     * A runnable which should be used for database queries. The onCompletion
     * method is executed on the database executor to handle Cursors correctly.
     * This class automatically creates a PodDBAdapter object and closes it when
     * it is no longer in use.
     */
    abstract static class QueryTask<T> implements Callable<T> {
        private T result;
        private final Context context;

        public QueryTask(Context context) {
            this.context = context;
        }

        @Override
        public T call() throws Exception {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            execute(adapter);
            adapter.close();
            return result;
        }

        public abstract void execute(PodDBAdapter adapter);

        void setResult(T result) {
            this.result = result;
        }
    }

    /**
     * Adds the given FeedItem to the flattr queue if the user is logged in. Otherwise, a dialog
     * will be opened that lets the user go either to the login screen or the website of the flattr thing.
     *
     * @param context
     * @param item
     */
    public static void flattrItemIfLoggedIn(Context context, FeedItem item) {
        if (FlattrUtils.hasToken()) {
            item.getFlattrStatus().setFlattrQueue();
            DBWriter.setFlattredStatus(context, item, true);
        } else {
            FlattrUtils.showNoTokenDialogOrRedirect(context, item.getPaymentLink());
        }
    }

    /**
     * Adds the given Feed to the flattr queue if the user is logged in. Otherwise, a dialog
     * will be opened that lets the user go either to the login screen or the website of the flattr thing.
     *
     * @param context
     * @param feed
     */
    public static void flattrFeedIfLoggedIn(Context context, Feed feed) {
        if (FlattrUtils.hasToken()) {
            feed.getFlattrStatus().setFlattrQueue();
            DBWriter.setFlattredStatus(context, feed, true);
        } else {
            FlattrUtils.showNoTokenDialogOrRedirect(context, feed.getPaymentLink());
        }
    }

}
