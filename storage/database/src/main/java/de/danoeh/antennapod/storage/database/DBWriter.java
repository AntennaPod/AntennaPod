package de.danoeh.antennapod.storage.database;

import android.app.backup.BackupManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import android.view.KeyEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.google.common.util.concurrent.Futures;
import de.danoeh.antennapod.event.DownloadLogEvent;

import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.net.download.serviceinterface.AutoDownloadManager;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.ui.appstartintent.MediaButtonStarter;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.event.FavoritesEvent;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.event.playback.PlaybackHistoryEvent;
import de.danoeh.antennapod.event.QueueEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.event.FeedEvent;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.net.sync.serviceinterface.EpisodeAction;

/**
 * Provides methods for writing data to AntennaPod's database.
 * In general, DBWriter-methods will be executed on an internal ExecutorService.
 * Some methods return a Future-object which the caller can use for waiting for the method's completion. The returned Future's
 * will NOT contain any results.
 */
public class DBWriter {

    private static final String TAG = "DBWriter";

    private static final ExecutorService dbExec;

    static {
        dbExec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("DatabaseExecutor");
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }

    private DBWriter() {
    }

    /**
     * Wait until all threads are finished to avoid the "Illegal connection pointer" error of
     * Robolectric. Call this method only for unit tests.
     */
    public static void tearDownTests() {
        try {
            dbExec.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore error
        }
    }

    /**
     * Deletes a downloaded FeedMedia file from the storage device.
     *
     * @param context A context that is used for opening a database connection.
     */
    public static Future<?> deleteFeedMediaOfItem(@NonNull final Context context,
                                                  final FeedMedia media) {
        return runOnDbThread(() -> {
            if (media == null) {
                return;
            }
            boolean result = deleteFeedMediaSynchronous(context, media);
            if (result && UserPreferences.shouldDeleteRemoveFromQueue()) {
                DBWriter.removeQueueItemSynchronous(context, false, media.getItemId());
            }
        });
    }

    private static boolean deleteFeedMediaSynchronous(@NonNull Context context, @NonNull FeedMedia media) {
        Log.i(TAG, String.format(Locale.US, "Requested to delete FeedMedia [id=%d, title=%s, downloaded=%s",
                media.getId(), media.getEpisodeTitle(), media.isDownloaded()));
        boolean localDelete = false;
        if (media.getLocalFileUrl() != null && media.getLocalFileUrl().startsWith("content://")) {
            // Local feed
            DocumentFile documentFile = DocumentFile.fromSingleUri(context, Uri.parse(media.getLocalFileUrl()));
            if (documentFile == null || !documentFile.exists() || !documentFile.delete()) {
                Log.d(TAG, "Deletion of local file failed.");
            }
            media.setLocalFileUrl(null);
            localDelete = true;
        } else if (media.getLocalFileUrl() != null) {
            // delete transcript file before the media file because the fileurl is needed
            if (media.getTranscriptFileUrl() != null) {
                File transcriptFile = new File(media.getTranscriptFileUrl());
                if (transcriptFile.exists() && !transcriptFile.delete()) {
                    Log.d(TAG, "Deletion of transcript file failed.");
                }
            }

            // delete downloaded media file
            File mediaFile = new File(media.getLocalFileUrl());
            if (mediaFile.exists() && !mediaFile.delete()) {
                Log.d(TAG, "Deletion of downloaded file failed.");
            }
            media.setDownloaded(false, 0);
            media.setLocalFileUrl(null);
            media.setHasEmbeddedPicture(false);
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setMedia(media);
            adapter.close();
        }

        if (media.getId() == PlaybackPreferences.getCurrentlyPlayingFeedMediaId()) {
            PlaybackPreferences.writeNoMediaPlaying();
            context.sendBroadcast(MediaButtonStarter.createIntent(context, KeyEvent.KEYCODE_MEDIA_STOP));
        }

        if (localDelete) {
            // Do full update of this feed to get rid of the item
            FeedUpdateManager.getInstance().runOnce(context, media.getItem().getFeed());
        } else {
            if (media.getItem().getFeed().getState() == Feed.STATE_SUBSCRIBED) {
                SynchronizationQueue.getInstance().enqueueEpisodeAction(
                        new EpisodeAction.Builder(media.getItem(), EpisodeAction.DELETE)
                            .currentTimestamp()
                            .build());
            }

            EventBus.getDefault().post(FeedItemEvent.updated(media.getItem()));
        }
        return true;
    }

    /**
     * Deletes a Feed and all downloaded files of its components like images and downloaded episodes.
     *
     * @param context A context that is used for opening a database connection.
     * @param feedId  ID of the Feed that should be deleted.
     */
    public static Future<?> deleteFeed(final Context context, final long feedId) {
        return runOnDbThread(() -> {
            final Feed feed = DBReader.getFeed(feedId, false, 0, Integer.MAX_VALUE);
            if (feed == null) {
                return;
            }

            deleteFeedItemsSynchronous(context, feed.getItems());

            // delete feed
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.removeFeed(feed);
            adapter.close();

            if (!feed.isLocalFeed() && feed.getState() == Feed.STATE_SUBSCRIBED) {
                SynchronizationQueue.getInstance().enqueueFeedRemoved(feed.getDownloadUrl());
            }
            EventBus.getDefault().post(new FeedListUpdateEvent(feed));
        });
    }

    /**
     * Remove the listed items and their FeedMedia entries.
     * Deleting media also removes the download log entries.
     */
    @NonNull
    public static Future<?> deleteFeedItems(@NonNull Context context, @NonNull List<FeedItem> items) {
        return runOnDbThread(() -> deleteFeedItemsSynchronous(context, items));
    }

    /**
     * Remove the listed items and their FeedMedia entries.
     * Deleting media also removes the download log entries.
     */
    private static void deleteFeedItemsSynchronous(@NonNull Context context, @NonNull List<FeedItem> items) {
        List<FeedItem> queue = DBReader.getQueue();
        List<FeedItem> removedFromQueue = new ArrayList<>();
        for (FeedItem item : items) {
            if (queue.remove(item)) {
                removedFromQueue.add(item);
            }
            if (item.getMedia() != null) {
                if (item.getMedia().getId() == PlaybackPreferences.getCurrentlyPlayingFeedMediaId()) {
                    // Applies to both downloaded and streamed media
                    PlaybackPreferences.writeNoMediaPlaying();
                    context.sendBroadcast(MediaButtonStarter.createIntent(context, KeyEvent.KEYCODE_MEDIA_STOP));
                }
                if (!item.getFeed().isLocalFeed()) {
                    if (DownloadServiceInterface.get().isDownloadingEpisode(item.getMedia().getDownloadUrl())) {
                        DownloadServiceInterface.get().cancel(context, item.getMedia());
                    }
                    if (item.getMedia().isDownloaded()) {
                        deleteFeedMediaSynchronous(context, item.getMedia());
                    }
                }
            }
        }

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        if (!removedFromQueue.isEmpty()) {
            adapter.setQueue(queue);
        }
        adapter.removeFeedItems(items);
        adapter.close();

        for (FeedItem item : removedFromQueue) {
            EventBus.getDefault().post(QueueEvent.irreversibleRemoved(item));
        }

        // we assume we also removed download log entries for the feed or its media files.
        // especially important if download or refresh failed, as the user should not be able
        // to retry these
        EventBus.getDefault().post(DownloadLogEvent.listUpdated());

        BackupManager backupManager = new BackupManager(context);
        backupManager.dataChanged();
    }

    /**
     * Deletes the entire playback history.
     */
    public static Future<?> clearPlaybackHistory() {
        return runOnDbThread(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.clearPlaybackHistory();
            adapter.close();
            EventBus.getDefault().post(PlaybackHistoryEvent.listUpdated());
        });
    }

    /**
     * Deletes the entire download log.
     */
    public static Future<?> clearDownloadLog() {
        return runOnDbThread(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.clearDownloadLog();
            adapter.close();
            EventBus.getDefault().post(DownloadLogEvent.listUpdated());
        });
    }

    public static Future<?> deleteFromPlaybackHistory(FeedItem feedItem) {
        return addItemToPlaybackHistory(feedItem.getMedia(), new Date(0));
    }

    /**
     * Adds a FeedMedia object to the playback history. A FeedMedia object is in the playback history if
     * its playback completion date is set to a non-null value. This method will set the playback completion date to the
     * current date regardless of the current value.
     *
     * @param media FeedMedia that should be added to the playback history.
     */
    public static Future<?> addItemToPlaybackHistory(FeedMedia media) {
        return addItemToPlaybackHistory(media, new Date());
    }

    /**
     * Adds a FeedMedia object to the playback history. A FeedMedia object is in the playback history if
     * its playback completion date is set to a non-null value. This method will set the playback completion date to the
     * current date regardless of the current value.
     *
     * @param media FeedMedia that should be added to the playback history.
     * @param date LastPlayedTimeHistory for <code>media</code>
     */
    public static Future<?> addItemToPlaybackHistory(final FeedMedia media, Date date) {
        return runOnDbThread(() -> {
            Log.d(TAG, "Adding item to playback history");
            media.setLastPlayedTimeHistory(date);

            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedMediaLastPlayedTimeHistory(media);
            adapter.close();
            EventBus.getDefault().post(PlaybackHistoryEvent.listUpdated());

        });
    }

    /**
     * Adds a Download status object to the download log.
     *
     * @param status The DownloadStatus object.
     */
    public static Future<?> addDownloadStatus(final DownloadResult status) {
        return runOnDbThread(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setDownloadStatus(status);
            adapter.close();
            EventBus.getDefault().post(DownloadLogEvent.listUpdated());
        });

    }

    /**
     * Inserts a FeedItem in the queue at the specified index. The 'read'-attribute of the FeedItem will be set to
     * true. If the FeedItem is already in the queue, the queue will not be modified.
     *
     * @param context             A context that is used for opening a database connection.
     * @param itemId              ID of the FeedItem that should be added to the queue.
     * @param index               Destination index. Must be in range 0..queue.size()
     * @throws IndexOutOfBoundsException if index < 0 || index >= queue.size()
     */
    public static Future<?> addQueueItemAt(final Context context, final long itemId, final int index) {
        return runOnDbThread(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            final List<FeedItem> queue = DBReader.getQueue();

            if (!itemListContains(queue, itemId)) {
                FeedItem item = DBReader.getFeedItem(itemId);
                if (item != null) {
                    queue.add(index, item);
                    adapter.setQueue(queue);
                    item.addTag(FeedItem.TAG_QUEUE);
                    EventBus.getDefault().post(QueueEvent.added(item, index));
                    EventBus.getDefault().post(FeedItemEvent.updated(item));
                    if (item.isNew()) {
                        DBWriter.markItemPlayed(FeedItem.UNPLAYED, item.getId());
                    }
                }
            }

            adapter.close();
            AutoDownloadManager.getInstance().autodownloadUndownloadedItems(context);
        });
    }

    /**
     * Appends FeedItem objects to the end of the queue. The 'read'-attribute of all items will be set to true.
     * If a FeedItem is already in the queue, the FeedItem will not change its position in the queue.
     *
     * @param context  A context that is used for opening a database connection.
     * @param items    FeedItem objects that should be added to the queue.
     */
    public static Future<?> addQueueItem(final Context context, final FeedItem... items) {
        return runOnDbThread(() -> {
            if (items.length < 1) {
                return;
            }

            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            final List<FeedItem> queue = DBReader.getQueue();

            LongList markAsUnplayedIds = new LongList();
            List<QueueEvent> events = new ArrayList<>();
            List<FeedItem> updatedItems = new ArrayList<>();
            ItemEnqueuePositionCalculator positionCalculator =
                    new ItemEnqueuePositionCalculator(UserPreferences.getEnqueueLocation());
            Playable currentlyPlaying = DBReader.getFeedMedia(PlaybackPreferences.getCurrentlyPlayingFeedMediaId());
            int insertPosition = positionCalculator.calcPosition(queue, currentlyPlaying);
            for (FeedItem item : items) {
                if (itemListContains(queue, item.getId())) {
                    continue;
                } else if (!item.hasMedia()) {
                    continue;
                }
                queue.add(insertPosition, item);
                events.add(QueueEvent.added(item, insertPosition));

                item.addTag(FeedItem.TAG_QUEUE);
                updatedItems.add(item);
                if (item.isNew()) {
                    markAsUnplayedIds.add(item.getId());
                }
                insertPosition++;
            }
            if (!updatedItems.isEmpty()) {
                applySortOrder(queue, events);
                adapter.setQueue(queue);
                for (QueueEvent event : events) {
                    EventBus.getDefault().post(event);
                }
                EventBus.getDefault().post(FeedItemEvent.updated(updatedItems));
                if (markAsUnplayedIds.size() > 0) {
                    DBWriter.markItemPlayed(FeedItem.UNPLAYED, markAsUnplayedIds.toArray());
                }
            }
            adapter.close();
            AutoDownloadManager.getInstance().autodownloadUndownloadedItems(context);
        });
    }

    /**
     * Sorts the queue depending on the configured sort order.
     * If the queue is not in keep sorted mode, nothing happens.
     *
     * @param queue  The queue to be sorted.
     * @param events Replaces the events by a single SORT event if the list has to be sorted automatically.
     */
    private static void applySortOrder(List<FeedItem> queue, List<QueueEvent> events) {
        if (!UserPreferences.isQueueKeepSorted()) {
            // queue is not in keep sorted mode, there's nothing to do
            return;
        }

        // Sort queue by configured sort order
        SortOrder sortOrder = UserPreferences.getQueueKeepSortedOrder();
        if (sortOrder == SortOrder.RANDOM) {
            // do not shuffle the list on every change
            return;
        }
        Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(sortOrder);
        permutor.reorder(queue);

        // Replace ADDED events by a single SORTED event
        events.clear();
        events.add(QueueEvent.sorted(queue));
    }

    /**
     * Removes all FeedItem objects from the queue.
     */
    public static Future<?> clearQueue() {
        return runOnDbThread(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.clearQueue();
            adapter.close();

            EventBus.getDefault().post(QueueEvent.cleared());
        });
    }

    /**
     * Removes a FeedItem object from the queue.
     *
     * @param context             A context that is used for opening a database connection.
     * @param performAutoDownload true if an auto-download process should be started after the operation.
     * @param item                FeedItem that should be removed.
     */
    public static Future<?> removeQueueItem(final Context context,
                                            final boolean performAutoDownload, final FeedItem item) {
        return runOnDbThread(() -> removeQueueItemSynchronous(context, performAutoDownload, item.getId()));
    }

    public static Future<?> removeQueueItem(final Context context, final boolean performAutoDownload,
                                            final long... itemIds) {
        return runOnDbThread(() -> removeQueueItemSynchronous(context, performAutoDownload, itemIds));
    }

    private static void removeQueueItemSynchronous(final Context context,
                                                   final boolean performAutoDownload,
                                                   final long... itemIds) {
        if (itemIds.length < 1) {
            return;
        }
        final PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        final List<FeedItem> queue = DBReader.getQueue();

        boolean queueModified = false;
        List<QueueEvent> events = new ArrayList<>();
        List<FeedItem> updatedItems = new ArrayList<>();
        for (long itemId : itemIds) {
            int position = indexInItemList(queue, itemId);
            if (position >= 0) {
                final FeedItem item = DBReader.getFeedItem(itemId);
                if (item == null) {
                    Log.e(TAG, "removeQueueItem - item in queue but somehow cannot be loaded."
                            + " Item ignored. It should never happen. id:" + itemId);
                    continue;
                }
                queue.remove(position);
                item.removeTag(FeedItem.TAG_QUEUE);
                events.add(QueueEvent.removed(item));
                updatedItems.add(item);
                queueModified = true;
            } else {
                Log.v(TAG, "removeQueueItem - item  not in queue:" + itemId);
            }
        }
        if (queueModified) {
            adapter.setQueue(queue);
            for (QueueEvent event : events) {
                EventBus.getDefault().post(event);
            }
            EventBus.getDefault().post(FeedItemEvent.updated(updatedItems));
        } else {
            Log.w(TAG, "Queue was not modified by call to removeQueueItem");
        }
        adapter.close();
        if (performAutoDownload) {
            AutoDownloadManager.getInstance().autodownloadUndownloadedItems(context);
        }
    }

    public static Future<?> toggleFavoriteItem(final FeedItem item) {
        if (item.isTagged(FeedItem.TAG_FAVORITE)) {
            return removeFavoriteItem(item);
        } else {
            return addFavoriteItem(item);
        }
    }

    public static Future<?> addFavoriteItem(final FeedItem item) {
        return runOnDbThread(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            adapter.addFavoriteItem(item);
            adapter.close();
            item.addTag(FeedItem.TAG_FAVORITE);
            EventBus.getDefault().post(new FavoritesEvent());
            EventBus.getDefault().post(FeedItemEvent.updated(item));
        });
    }

    public static Future<?> removeFavoriteItem(final FeedItem item) {
        return runOnDbThread(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            adapter.removeFavoriteItem(item);
            adapter.close();
            item.removeTag(FeedItem.TAG_FAVORITE);
            EventBus.getDefault().post(new FavoritesEvent());
            EventBus.getDefault().post(FeedItemEvent.updated(item));
        });
    }

    /**
     * Changes the position of a FeedItem in the queue.
     *
     * @param from            Source index. Must be in range 0..queue.size()-1.
     * @param to              Destination index. Must be in range 0..queue.size()-1.
     * @param broadcastUpdate true if this operation should trigger a QueueUpdateBroadcast. This option should be set to
     *                        false if the caller wants to avoid unexpected updates of the GUI.
     * @throws IndexOutOfBoundsException if (to < 0 || to >= queue.size()) || (from < 0 || from >= queue.size())
     */
    public static Future<?> moveQueueItem(final int from, final int to, final boolean broadcastUpdate) {
        return runOnDbThread(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            final List<FeedItem> queue = DBReader.getQueue();

            if (from >= 0 && from < queue.size() && to >= 0 && to < queue.size()) {
                final FeedItem item = queue.remove(from);
                queue.add(to, item);

                adapter.setQueue(queue);
                if (broadcastUpdate) {
                    EventBus.getDefault().post(QueueEvent.moved(item, to));
                }
            }
            adapter.close();
        });
    }

    public static Future<?> moveQueueItemsToTop(final List<FeedItem> items) {
        return runOnDbThread(() -> moveQueueItemsSynchronous(true, items));
    }

    public static Future<?> moveQueueItemsToBottom(final List<FeedItem> items) {
        return runOnDbThread(() -> moveQueueItemsSynchronous(false, items));
    }

    private static void moveQueueItemsSynchronous(final boolean moveToTop, final List<FeedItem> items) {
        if (items.isEmpty()) {
            return;
        }

        final PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        final List<FeedItem> queue = DBReader.getQueue();

        List<FeedItem> selectedItems = moveToTop ? new ArrayList<>(items) : items;
        if (moveToTop) {
            Collections.reverse(selectedItems);
        }

        boolean queueModified = false;
        List<QueueEvent> events = new ArrayList<>();

        queue.removeAll(selectedItems);
        events.add(QueueEvent.setQueue(queue));

        for (FeedItem item : selectedItems) {
            int newIndex = moveToTop ? 0 : queue.size();
            queue.add(newIndex, item);
            events.add(QueueEvent.moved(item, newIndex));
            queueModified = true;
        }

        if (queueModified) {
            adapter.setQueue(queue);
            for (QueueEvent event : events) {
                EventBus.getDefault().post(event);
            }
        } else {
            Log.w(TAG, "moveToTop: " + moveToTop +  " - Queue was not modified.");
        }
        adapter.close();
    }

    public static Future<?> resetPagedFeedPage(Feed feed) {
        return runOnDbThread(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.resetPagedFeedPage(feed);
            adapter.close();
        });
    }

    /**
     * Sets the 'read'-attribute of all specified FeedItems
     *
     * @param played  New value of the 'read'-attribute, one of FeedItem.PLAYED, FeedItem.NEW,
     *                FeedItem.UNPLAYED
     * @param itemIds IDs of the FeedItems.
     */
    public static Future<?> markItemPlayed(final int played, final long... itemIds) {
        return markItemPlayed(played, true, itemIds);
    }

    /**
     * Sets the 'read'-attribute of all specified FeedItems
     *
     * @param played  New value of the 'read'-attribute, one of FeedItem.PLAYED, FeedItem.NEW,
     *                FeedItem.UNPLAYED
     * @param broadcastUpdate true if this operation should trigger a UnreadItemsUpdate broadcast.
     *        This option is usually set to true
     * @param itemIds IDs of the FeedItems.
     */
    public static Future<?> markItemPlayed(final int played, final boolean broadcastUpdate,
                                           final long... itemIds) {
        return runOnDbThread(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedItemRead(played, itemIds);
            adapter.close();
            if (broadcastUpdate) {
                EventBus.getDefault().post(new UnreadItemsUpdateEvent());
            }
        });
    }

    /**
     * Sets the 'read'-attribute of a FeedItem to the specified value.
     *
     * @param item               The FeedItem object
     * @param played             New value of the 'read'-attribute one of FeedItem.PLAYED,
     *                           FeedItem.NEW, FeedItem.UNPLAYED
     * @param resetMediaPosition true if this method should also reset the position of the FeedItem's FeedMedia object.
     */
    @NonNull
    public static Future<?> markItemPlayed(FeedItem item, int played, boolean resetMediaPosition) {
        return runOnDbThread(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedItemRead(item, played, resetMediaPosition);
            adapter.close();

            EventBus.getDefault().post(new UnreadItemsUpdateEvent());
        });
    }

    /**
     * Sets the 'read'-attribute of all NEW FeedItems of a specific Feed to UNPLAYED.
     *
     * @param feedId ID of the Feed.
     */
    public static Future<?> removeFeedNewFlag(final long feedId) {
        return runOnDbThread(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedItems(FeedItem.NEW, FeedItem.UNPLAYED, feedId);
            adapter.close();

            EventBus.getDefault().post(new UnreadItemsUpdateEvent());
        });
    }

    /**
     * Sets the 'read'-attribute of all NEW FeedItems to UNPLAYED.
     */
    public static Future<?> removeAllNewFlags() {
        return runOnDbThread(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedItems(FeedItem.NEW, FeedItem.UNPLAYED);
            adapter.close();

            EventBus.getDefault().post(new UnreadItemsUpdateEvent());
        });
    }

    static Future<?> addNewFeed(final Context context, final Feed... feeds) {
        return runOnDbThread(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setCompleteFeed(feeds);
            adapter.close();

            for (Feed feed : feeds) {
                if (!feed.isLocalFeed() && feed.getState() == Feed.STATE_SUBSCRIBED) {
                    SynchronizationQueue.getInstance().enqueueFeedAdded(feed.getDownloadUrl());
                }
            }

            BackupManager backupManager = new BackupManager(context);
            backupManager.dataChanged();
        });
    }

    static Future<?> setCompleteFeed(final Feed... feeds) {
        return runOnDbThread(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setCompleteFeed(feeds);
            adapter.close();
        });
    }

    public static Future<?> setItemList(final List<FeedItem> items) {
        return runOnDbThread(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.storeFeedItemlist(items);
            adapter.close();
            EventBus.getDefault().post(FeedItemEvent.updated(items));
        });
    }

    /**
     * Saves a FeedMedia object in the database. This method will save all attributes of the FeedMedia object. The
     * contents of FeedComponent-attributes (e.g. the FeedMedia's 'item'-attribute) will not be saved.
     *
     * @param media The FeedMedia object.
     */
    public static Future<?> setFeedMedia(final FeedMedia media) {
        return runOnDbThread(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setMedia(media);
            adapter.close();
        });
    }

    /**
     * Saves the 'position', 'duration' and 'last played time' attributes of a FeedMedia object
     *
     * @param media The FeedMedia object.
     */
    public static Future<?> setFeedMediaPlaybackInformation(final FeedMedia media) {
        return runOnDbThread(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedMediaPlaybackInformation(media);
            adapter.close();
        });
    }

    /**
     * Saves a FeedItem object in the database. This method will save all attributes of the FeedItem object including
     * the content of FeedComponent-attributes.
     *
     * @param item The FeedItem object.
     */
    public static Future<?> setFeedItem(final FeedItem item) {
        return runOnDbThread(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setSingleFeedItem(item);
            adapter.close();
            EventBus.getDefault().post(FeedItemEvent.updated(item));
        });
    }

    /**
     * Updates download URL of a feed
     */
    public static Future<?> updateFeedDownloadURL(final String original, final String updated) {
        Log.d(TAG, "updateFeedDownloadURL(original: " + original + ", updated: " + updated + ")");
        return runOnDbThread(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedDownloadUrl(original, updated);
            adapter.close();
        });
    }

    /**
     * Saves a FeedPreferences object in the database. The Feed ID of the FeedPreferences-object MUST NOT be 0.
     *
     * @param preferences The FeedPreferences object.
     */
    public static Future<?> setFeedPreferences(final FeedPreferences preferences) {
        return runOnDbThread(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedPreferences(preferences);
            adapter.close();
            EventBus.getDefault().post(new FeedListUpdateEvent(preferences.getFeedID()));
        });
    }

    private static boolean itemListContains(List<FeedItem> items, long itemId) {
        return indexInItemList(items, itemId) >= 0;
    }

    private static int indexInItemList(List<FeedItem> items, long itemId) {
        for (int i = 0; i < items.size(); i++) {
            FeedItem item = items.get(i);
            if (item.getId() == itemId) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Saves if a feed's last update failed
     *
     * @param lastUpdateFailed true if last update failed
     */
    public static Future<?> setFeedLastUpdateFailed(final long feedId,
                                                    final boolean lastUpdateFailed) {
        return runOnDbThread(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedLastUpdateFailed(feedId, lastUpdateFailed);
            adapter.close();
            EventBus.getDefault().post(new FeedListUpdateEvent(feedId));
        });
    }

    public static Future<?> setFeedCustomTitle(Feed feed) {
        return runOnDbThread(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedCustomTitle(feed.getId(), feed.getCustomTitle());
            adapter.close();
            EventBus.getDefault().post(new FeedListUpdateEvent(feed));
        });
    }

    public static Future<?> setFeedState(Context context, Feed feed, int newState) {
        int oldState = feed.getState();
        return runOnDbThread(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedState(feed.getId(), newState);
            feed.setState(newState);
            if (oldState == Feed.STATE_NOT_SUBSCRIBED && newState == Feed.STATE_SUBSCRIBED) {
                feed.getPreferences().setKeepUpdated(true);
                DBWriter.setFeedPreferences(feed.getPreferences());
                FeedUpdateManager.getInstance().runOnceOrAsk(context, feed);
                SynchronizationQueue.getInstance().enqueueFeedAdded(feed.getDownloadUrl());
                DBReader.getFeedItemList(feed, FeedItemFilter.unfiltered(),
                        SortOrder.DATE_NEW_OLD, 0, Integer.MAX_VALUE);
                for (FeedItem item : feed.getItems()) {
                    if (item.isPlayed()) {
                        SynchronizationQueue.getInstance().enqueueEpisodePlayed(item.getMedia(), true);
                    }
                }
            }
            adapter.close();
            EventBus.getDefault().post(new FeedListUpdateEvent(feed));
        });
    }

    /**
     * Sort the FeedItems in the queue with the given the named sort order.
     *
     * @param broadcastUpdate <code>true</code> if this operation should trigger a
     *                        QueueUpdateBroadcast. This option should be set to <code>false</code>
     *                        if the caller wants to avoid unexpected updates of the GUI.
     */
    public static Future<?> reorderQueue(@Nullable SortOrder sortOrder, final boolean broadcastUpdate) {
        if (sortOrder == null) {
            Log.w(TAG, "reorderQueue() - sortOrder is null. Do nothing.");
            return runOnDbThread(() -> { });
        }
        final Permutor<FeedItem> permutor = FeedItemPermutors.getPermutor(sortOrder);
        return runOnDbThread(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            final List<FeedItem> queue = DBReader.getQueue();

            permutor.reorder(queue);
            adapter.setQueue(queue);
            if (broadcastUpdate) {
                EventBus.getDefault().post(QueueEvent.sorted(queue));
            }
            adapter.close();
        });
    }

    /**
     * Set filter of the feed
     *
     * @param feedId       The feed's ID
     * @param filterValues Values that represent properties to filter by
     */
    public static Future<?> setFeedItemsFilter(final long feedId,
                                               final Set<String> filterValues) {
        Log.d(TAG, "setFeedItemsFilter() called with: " + "feedId = [" + feedId + "], filterValues = [" + filterValues + "]");
        return runOnDbThread(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedItemFilter(feedId, filterValues);
            adapter.close();
            EventBus.getDefault().post(new FeedEvent(FeedEvent.Action.FILTER_CHANGED, feedId));
        });
    }

    /**
     * Set item sort order of the feed
     *
     */
    public static Future<?> setFeedItemSortOrder(long feedId, @Nullable SortOrder sortOrder) {
        return runOnDbThread(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedItemSortOrder(feedId, sortOrder);
            adapter.close();
            EventBus.getDefault().post(new FeedEvent(FeedEvent.Action.SORT_ORDER_CHANGED, feedId));
        });
    }

    /**
     * Reset the statistics in DB
     */
    @NonNull
    public static Future<?> resetStatistics() {
        return runOnDbThread(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.resetAllMediaPlayedDuration();
            adapter.close();
        });
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
        long feedId = 0;
        if (cursor.moveToFirst()) {
            do {
                if (cursor.getString(1).equals(downloadUrl)) {
                    feedId = cursor.getLong(0);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        adapter.close();

        if (feedId != 0) {
            try {
                deleteFeed(context, feedId).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "removeFeedWithDownloadUrl: Could not find feed with url: " + downloadUrl);
        }
    }

    /**
     * Submit to the DB thread only if caller is not already on the DB thread. Otherwise,
     * just execute synchronously
     */
    private static Future<?> runOnDbThread(Runnable runnable) {
        if ("DatabaseExecutor".equals(Thread.currentThread().getName())) {
            runnable.run();
            return Futures.immediateFuture(null);
        } else {
            return dbExec.submit(runnable);
        }
    }
}
