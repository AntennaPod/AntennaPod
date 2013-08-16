package de.danoeh.antennapod.storage;

import java.io.File;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.feed.*;
import de.danoeh.antennapod.preferences.PlaybackPreferences;
import de.danoeh.antennapod.service.PlaybackService;
import de.danoeh.antennapod.service.download.DownloadStatus;
import de.danoeh.antennapod.util.QueueAccess;

/**
 * Provides methods for writing data to AntennaPod's database.
 * In general, DBWriter-methods will be executed on an internal ExecutorService.
 * Some methods return a Future-object which the caller can use for waiting for the method's completion. The returned Future's
 * will NOT contain any results.
 * The caller can also use the {@link EventDistributor} in order to be notified about the method's completion asynchronously.
 * This class will use the {@link EventDistributor} to notify listeners about changes in the database.
 */
public class DBWriter {
    private static final String TAG = "DBWriter";

    private static final ExecutorService dbExec;

    static {
        dbExec = Executors.newSingleThreadExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            }
        });
    }

    private DBWriter() {
    }

    /**
     * Deletes a downloaded FeedMedia file from the storage device.
     *
     * @param context A context that is used for opening a database connection.
     * @param mediaId ID of the FeedMedia object whose downloaded file should be deleted.
     */
    public static Future<?> deleteFeedMediaOfItem(final Context context,
                                                  final long mediaId) {
        return dbExec.submit(new Runnable() {
            @Override
            public void run() {

                final FeedMedia media = DBReader.getFeedMedia(context, mediaId);
                if (media != null) {
                    boolean result = false;
                    if (media.isDownloaded()) {
                        // delete downloaded media file
                        File mediaFile = new File(media.getFile_url());
                        if (mediaFile.exists()) {
                            result = mediaFile.delete();
                        }
                        media.setDownloaded(false);
                        media.setFile_url(null);
                        PodDBAdapter adapter = new PodDBAdapter(context);
                        adapter.open();
                        adapter.setMedia(media);
                        adapter.close();

                        // If media is currently being played, change playback
                        // type to 'stream' and shutdown playback service
                        SharedPreferences prefs = PreferenceManager
                                .getDefaultSharedPreferences(context);
                        if (PlaybackPreferences.getCurrentlyPlayingMedia() == FeedMedia.PLAYABLE_TYPE_FEEDMEDIA) {
                            if (media.getId() == PlaybackPreferences
                                    .getCurrentlyPlayingFeedMediaId()) {
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean(
                                        PlaybackPreferences.PREF_CURRENT_EPISODE_IS_STREAM,
                                        true);
                                editor.commit();
                            }
                            if (PlaybackPreferences
                                    .getCurrentlyPlayingFeedMediaId() == media
                                    .getId()) {
                                context.sendBroadcast(new Intent(
                                        PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE));
                            }
                        }
                    }
                    if (AppConfig.DEBUG)
                        Log.d(TAG, "Deleting File. Result: " + result);
                }
            }
        });
    }

    /**
     * Deletes a Feed and all downloaded files of its components like images and downloaded episodes.
     *
     * @param context A context that is used for opening a database connection.
     * @param feedId  ID of the Feed that should be deleted.
     */
    public static Future<?> deleteFeed(final Context context, final long feedId) {
        return dbExec.submit(new Runnable() {
            @Override
            public void run() {
                DownloadRequester requester = DownloadRequester.getInstance();
                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(context
                                .getApplicationContext());
                final Feed feed = DBReader.getFeed(context, feedId);
                if (feed != null) {
                    if (PlaybackPreferences.getCurrentlyPlayingMedia() == FeedMedia.PLAYABLE_TYPE_FEEDMEDIA
                            && PlaybackPreferences.getLastPlayedFeedId() == feed
                            .getId()) {
                        context.sendBroadcast(new Intent(
                                PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE));
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putLong(
                                PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEED_ID,
                                -1);
                        editor.commit();
                    }

                    // delete image file
                    if (feed.getImage() != null) {
                        if (feed.getImage().isDownloaded()
                                && feed.getImage().getFile_url() != null) {
                            File imageFile = new File(feed.getImage()
                                    .getFile_url());
                            imageFile.delete();
                        } else if (requester.isDownloadingFile(feed.getImage())) {
                            requester.cancelDownload(context, feed.getImage());
                        }
                    }
                    // delete stored media files and mark them as read
                    List<FeedItem> queue = DBReader.getQueue(context);
                    boolean queueWasModified = false;
                    if (feed.getItems() == null) {
                        DBReader.getFeedItemList(context, feed);
                    }

                    for (FeedItem item : feed.getItems()) {
                        queueWasModified |= queue.remove(item);
                        if (item.getMedia() != null
                                && item.getMedia().isDownloaded()) {
                            File mediaFile = new File(item.getMedia()
                                    .getFile_url());
                            mediaFile.delete();
                        } else if (item.getMedia() != null
                                && requester.isDownloadingFile(item.getMedia())) {
                            requester.cancelDownload(context, item.getMedia());
                        }
                    }
                    PodDBAdapter adapter = new PodDBAdapter(context);
                    adapter.open();
                    if (queueWasModified) {
                        adapter.setQueue(queue);
                    }
                    adapter.removeFeed(feed);
                    adapter.close();
                    EventDistributor.getInstance().sendFeedUpdateBroadcast();
                }
            }
        });
    }

    /**
     * Deletes the entire playback history.
     *
     * @param context A context that is used for opening a database connection.
     */
    public static Future<?> clearPlaybackHistory(final Context context) {
        return dbExec.submit(new Runnable() {

            @Override
            public void run() {
                PodDBAdapter adapter = new PodDBAdapter(context);
                adapter.open();
                adapter.clearPlaybackHistory();
                adapter.close();
                EventDistributor.getInstance()
                        .sendPlaybackHistoryUpdateBroadcast();
            }
        });
    }

    /**
     * Adds a FeedMedia object to the playback history. A FeedMedia object is in the playback history if
     * its playback completion date is set to a non-null value. This method will set the playback completion date to the
     * current date regardless of the current value.
     *
     * @param context A context that is used for opening a database connection.
     * @param media   FeedMedia that should be added to the playback history.
     */
    public static Future<?> addItemToPlaybackHistory(final Context context,
                                                     final FeedMedia media) {
        return dbExec.submit(new Runnable() {
            @Override
            public void run() {
                if (AppConfig.DEBUG)
                    Log.d(TAG, "Adding new item to playback history");
                media.setPlaybackCompletionDate(new Date());
                PodDBAdapter adapter = new PodDBAdapter(context);
                adapter.open();
                adapter.setMedia(media);
                adapter.close();
                EventDistributor.getInstance().sendPlaybackHistoryUpdateBroadcast();

            }
        });
    }

    private static void cleanupDownloadLog(final PodDBAdapter adapter) {
        final long logSize = adapter.getDownloadLogSize();
        if (logSize > DBReader.DOWNLOAD_LOG_SIZE) {
            if (AppConfig.DEBUG)
                Log.d(TAG, "Cleaning up download log");
            adapter.removeDownloadLogItems(logSize - DBReader.DOWNLOAD_LOG_SIZE);
        }
    }

    /**
     * Adds a Download status object to the download log.
     *
     * @param context A context that is used for opening a database connection.
     * @param status  The DownloadStatus object.
     */
    public static Future<?> addDownloadStatus(final Context context,
                                              final DownloadStatus status) {
        return dbExec.submit(new Runnable() {

            @Override
            public void run() {

                PodDBAdapter adapter = new PodDBAdapter(context);
                adapter.open();

                adapter.setDownloadStatus(status);
                cleanupDownloadLog(adapter);
                adapter.close();
                EventDistributor.getInstance().sendDownloadLogUpdateBroadcast();
            }
        });

    }

    /**
     * Inserts a FeedItem in the queue at the specified index. The 'read'-attribute of the FeedItem will be set to
     * true. If the FeedItem is already in the queue, the queue will not be modified.
     *
     * @param context             A context that is used for opening a database connection.
     * @param itemId              ID of the FeedItem that should be added to the queue.
     * @param index               Destination index. Must be in range 0..queue.size()
     * @param performAutoDownload True if an auto-download process should be started after the operation
     * @throws IndexOutOfBoundsException if index < 0 || index >= queue.size()
     */
    public static Future<?> addQueueItemAt(final Context context, final long itemId,
                                           final int index, final boolean performAutoDownload) {
        return dbExec.submit(new Runnable() {

            @Override
            public void run() {
                final PodDBAdapter adapter = new PodDBAdapter(context);
                adapter.open();
                final List<FeedItem> queue = DBReader
                        .getQueue(context, adapter);
                FeedItem item = null;

                if (queue != null) {
                    boolean queueModified = false;
                    boolean unreadItemsModified = false;

                    if (!itemListContains(queue, itemId)) {
                        item = DBReader.getFeedItem(context, itemId);
                        if (item != null) {
                            queue.add(index, item);
                            queueModified = true;
                            if (!item.isRead()) {
                                item.setRead(true);
                                unreadItemsModified = true;
                            }
                        }
                    }
                    if (queueModified) {
                        adapter.setQueue(queue);
                        EventDistributor.getInstance()
                                .sendQueueUpdateBroadcast();
                    }
                    if (unreadItemsModified && item != null) {
                        adapter.setSingleFeedItem(item);
                        EventDistributor.getInstance()
                                .sendUnreadItemsUpdateBroadcast();
                    }
                }
                adapter.close();
                if (performAutoDownload) {

                    new Thread() {
                        @Override
                        public void run() {
                            DBTasks.autodownloadUndownloadedItems(context);

                        }
                    }.start();
                }

            }
        });

    }

    /**
     * Appends FeedItem objects to the end of the queue. The 'read'-attribute of all items will be set to true.
     * If a FeedItem is already in the queue, the FeedItem will not change its position in the queue.
     *
     * @param context A context that is used for opening a database connection.
     * @param itemIds IDs of the FeedItem objects that should be added to the queue.
     */
    public static Future<?> addQueueItem(final Context context,
                                         final long... itemIds) {
        return dbExec.submit(new Runnable() {

            @Override
            public void run() {
                if (itemIds.length > 0) {
                    final PodDBAdapter adapter = new PodDBAdapter(context);
                    adapter.open();
                    final List<FeedItem> queue = DBReader.getQueue(context,
                            adapter);

                    if (queue != null) {
                        boolean queueModified = false;
                        boolean unreadItemsModified = false;
                        List<FeedItem> itemsToSave = new LinkedList<FeedItem>();
                        for (int i = 0; i < itemIds.length; i++) {
                            if (!itemListContains(queue, itemIds[i])) {
                                final FeedItem item = DBReader.getFeedItem(
                                        context, itemIds[i]);

                                if (item != null) {
                                    queue.add(item);
                                    queueModified = true;
                                    if (!item.isRead()) {
                                        item.setRead(true);
                                        itemsToSave.add(item);
                                        unreadItemsModified = true;
                                    }
                                }
                            }
                        }
                        if (queueModified) {
                            adapter.setQueue(queue);
                            EventDistributor.getInstance()
                                    .sendQueueUpdateBroadcast();
                        }
                        if (unreadItemsModified) {
                            adapter.setFeedItemlist(itemsToSave);
                            EventDistributor.getInstance()
                                    .sendUnreadItemsUpdateBroadcast();
                        }
                    }
                    adapter.close();
                    new Thread() {
                        @Override
                        public void run() {
                            DBTasks.autodownloadUndownloadedItems(context);

                        }
                    }.start();
                }
            }
        });

    }

    /**
     * Removes all FeedItem objects from the queue.
     *
     * @param context A context that is used for opening a database connection.
     */
    public static Future<?> clearQueue(final Context context) {
        return dbExec.submit(new Runnable() {

            @Override
            public void run() {
                PodDBAdapter adapter = new PodDBAdapter(context);
                adapter.open();
                adapter.clearQueue();
                adapter.close();

                EventDistributor.getInstance().sendQueueUpdateBroadcast();
            }
        });
    }

    /**
     * Removes a FeedItem object from the queue.
     *
     * @param context             A context that is used for opening a database connection.
     * @param itemId              ID of the FeedItem that should be removed.
     * @param performAutoDownload true if an auto-download process should be started after the operation.
     */
    public static Future<?> removeQueueItem(final Context context,
                                            final long itemId, final boolean performAutoDownload) {
        return dbExec.submit(new Runnable() {

            @Override
            public void run() {
                final PodDBAdapter adapter = new PodDBAdapter(context);
                adapter.open();
                final List<FeedItem> queue = DBReader
                        .getQueue(context, adapter);
                FeedItem item = null;

                if (queue != null) {
                    boolean queueModified = false;
                    QueueAccess queueAccess = QueueAccess.ItemListAccess(queue);
                    if (queueAccess.contains(itemId)) {
                        item = DBReader.getFeedItem(context, itemId);
                        if (item != null) {
                            queueModified = queueAccess.remove(itemId);
                        }
                    }
                    if (queueModified) {
                        adapter.setQueue(queue);
                        EventDistributor.getInstance()
                                .sendQueueUpdateBroadcast();
                    } else {
                        Log.w(TAG, "Queue was not modified by call to removeQueueItem");
                    }
                } else {
                    Log.e(TAG, "removeQueueItem: Could not load queue");
                }
                adapter.close();
                if (performAutoDownload) {

                    new Thread() {
                        @Override
                        public void run() {
                            DBTasks.autodownloadUndownloadedItems(context);

                        }
                    }.start();
                }
            }
        });

    }
    
    /**
     * Moves the specified item to the top of the queue.
     *
     * @param context         A context that is used for opening a database connection.
     * @param selectedItem    The item to move to the top of the queue
     * @param broadcastUpdate true if this operation should trigger a QueueUpdateBroadcast. This option should be set to
     *                        false if the caller wants to avoid unexpected updates of the GUI.
     * @throws IndexOutOfBoundsException if (to < 0 || to >= queue.size()) || (from < 0 || from >= queue.size())
     */
    public static Future<?> moveQueueItemToTop(final Context context, final FeedItem selectedItem, final boolean broadcastUpdate) {
        return dbExec.submit(new Runnable() {

            @Override
            public void run() {
                final PodDBAdapter adapter = new PodDBAdapter(context);
                adapter.open();
                final List<FeedItem> queue = DBReader
                        .getQueue(context, adapter);

                if (queue != null) {
                    if (queue.remove(selectedItem)) {
                    	// it was removed, put it on the front
                    	queue.add(0, selectedItem);
                    } else {
                    	Log.e(TAG, "moveQueueItemToTop: Could not move to top, no such item");
                    }
                } else {
                    Log.e(TAG, "moveQueueItemToTop: Could not move to top, no queue");
                }
                adapter.close();
            }
        });
    }
    
    /**
     * Changes the position of a FeedItem in the queue.
     *
     * @param context         A context that is used for opening a database connection.
     * @param from            Source index. Must be in range 0..queue.size()-1.
     * @param to              Destination index. Must be in range 0..queue.size()-1.
     * @param broadcastUpdate true if this operation should trigger a QueueUpdateBroadcast. This option should be set to
     *                        false if the caller wants to avoid unexpected updates of the GUI.
     * @throws IndexOutOfBoundsException if (to < 0 || to >= queue.size()) || (from < 0 || from >= queue.size())
     */
    public static Future<?> moveQueueItem(final Context context, final int from,
                                          final int to, final boolean broadcastUpdate) {
        return dbExec.submit(new Runnable() {

            @Override
            public void run() {
                final PodDBAdapter adapter = new PodDBAdapter(context);
                adapter.open();
                final List<FeedItem> queue = DBReader
                        .getQueue(context, adapter);

                if (queue != null) {
                    if (from >= 0 && from < queue.size() && to >= 0
                            && to < queue.size()) {

                        final FeedItem item = queue.remove(from);
                        queue.add(to, item);

                        adapter.setQueue(queue);
                        if (broadcastUpdate) {
                            EventDistributor.getInstance()
                                    .sendQueueUpdateBroadcast();
                        }

                    }
                } else {
                    Log.e(TAG, "moveQueueItem: Could not load queue");
                }
                adapter.close();
            }
        });
    }

    /**
     * Sets the 'read'-attribute of a FeedItem to the specified value.
     *
     * @param context            A context that is used for opening a database connection.
     * @param item               The FeedItem object
     * @param read               New value of the 'read'-attribute
     * @param resetMediaPosition true if this method should also reset the position of the FeedItem's FeedMedia object.
     *                           If the FeedItem has no FeedMedia object, this parameter will be ignored.
     */
    public static Future<?> markItemRead(Context context, FeedItem item, boolean read, boolean resetMediaPosition) {
        long mediaId = (item.hasMedia()) ? item.getMedia().getId() : 0;
        return markItemRead(context, item.getId(), read, mediaId, resetMediaPosition);
    }

    /**
     * Sets the 'read'-attribute of a FeedItem to the specified value.
     *
     * @param context A context that is used for opening a database connection.
     * @param itemId  ID of the FeedItem
     * @param read    New value of the 'read'-attribute
     */
    public static Future<?> markItemRead(final Context context, final long itemId,
                                         final boolean read) {
        return markItemRead(context, itemId, read, 0, false);
    }

    private static Future<?> markItemRead(final Context context, final long itemId,
                                          final boolean read, final long mediaId,
                                          final boolean resetMediaPosition) {
        return dbExec.submit(new Runnable() {

            @Override
            public void run() {
                final PodDBAdapter adapter = new PodDBAdapter(context);
                adapter.open();
                adapter.setFeedItemRead(read, itemId, mediaId,
                        resetMediaPosition);
                adapter.close();

                EventDistributor.getInstance().sendUnreadItemsUpdateBroadcast();
            }
        });
    }

    /**
     * Sets the 'read'-attribute of all FeedItems of a specific Feed to true.
     *
     * @param context A context that is used for opening a database connection.
     * @param feedId  ID of the Feed.
     */
    public static Future<?> markFeedRead(final Context context, final long feedId) {
        return dbExec.submit(new Runnable() {

            @Override
            public void run() {
                final PodDBAdapter adapter = new PodDBAdapter(context);
                adapter.open();
                Cursor itemCursor = adapter.getAllItemsOfFeedCursor(feedId);
                long[] itemIds = new long[itemCursor.getCount()];
                itemCursor.moveToFirst();
                for (int i = 0; i < itemIds.length; i++) {
                    itemIds[i] = itemCursor.getLong(PodDBAdapter.KEY_ID_INDEX);
                }
                itemCursor.close();
                adapter.setFeedItemRead(true, itemIds);
                adapter.close();

                EventDistributor.getInstance().sendUnreadItemsUpdateBroadcast();
            }
        });

    }

    /**
     * Sets the 'read'-attribute of all FeedItems to true.
     *
     * @param context A context that is used for opening a database connection.
     */
    public static Future<?> markAllItemsRead(final Context context) {
        return dbExec.submit(new Runnable() {

            @Override
            public void run() {
                final PodDBAdapter adapter = new PodDBAdapter(context);
                adapter.open();
                Cursor itemCursor = adapter.getUnreadItemsCursor();
                long[] itemIds = new long[itemCursor.getCount()];
                itemCursor.moveToFirst();
                for (int i = 0; i < itemIds.length; i++) {
                    itemIds[i] = itemCursor.getLong(PodDBAdapter.KEY_ID_INDEX);
                }
                itemCursor.close();
                adapter.setFeedItemRead(true, itemIds);
                adapter.close();

                EventDistributor.getInstance().sendUnreadItemsUpdateBroadcast();
            }
        });

    }

    static Future<?> addNewFeed(final Context context, final Feed feed) {
        return dbExec.submit(new Runnable() {

            @Override
            public void run() {
                final PodDBAdapter adapter = new PodDBAdapter(context);
                adapter.open();
                adapter.setCompleteFeed(feed);
                adapter.close();

                EventDistributor.getInstance().sendFeedUpdateBroadcast();
            }
        });
    }

    static Future<?> setCompleteFeed(final Context context, final Feed feed) {
        return dbExec.submit(new Runnable() {

            @Override
            public void run() {
                PodDBAdapter adapter = new PodDBAdapter(context);
                adapter.open();
                adapter.setCompleteFeed(feed);
                adapter.close();

                EventDistributor.getInstance().sendFeedUpdateBroadcast();
            }
        });

    }

    /**
     * Saves a FeedMedia object in the database. This method will save all attributes of the FeedMedia object. The
     * contents of FeedComponent-attributes (e.g. the FeedMedia's 'item'-attribute) will not be saved.
     *
     * @param context A context that is used for opening a database connection.
     * @param media   The FeedMedia object.
     */
    public static Future<?> setFeedMedia(final Context context,
                                         final FeedMedia media) {
        return dbExec.submit(new Runnable() {

            @Override
            public void run() {
                PodDBAdapter adapter = new PodDBAdapter(context);
                adapter.open();
                adapter.setMedia(media);
                adapter.close();
            }
        });
    }

    /**
     * Saves the 'position' and 'duration' attributes of a FeedMedia object
     *
     * @param context A context that is used for opening a database connection.
     * @param media   The FeedMedia object.
     */
    public static Future<?> setFeedMediaPlaybackInformation(final Context context, final FeedMedia media) {
        return dbExec.submit(new Runnable() {
            @Override
            public void run() {
                PodDBAdapter adapter = new PodDBAdapter(context);
                adapter.open();
                adapter.setFeedMediaPlaybackInformation(media);
                adapter.close();
            }
        });
    }

    /**
     * Saves a FeedItem object in the database. This method will save all attributes of the FeedItem object including
     * the content of FeedComponent-attributes.
     *
     * @param context A context that is used for opening a database connection.
     * @param item    The FeedItem object.
     */
    public static Future<?> setFeedItem(final Context context,
                                        final FeedItem item) {
        return dbExec.submit(new Runnable() {

            @Override
            public void run() {
                PodDBAdapter adapter = new PodDBAdapter(context);
                adapter.open();
                adapter.setSingleFeedItem(item);
                adapter.close();
            }
        });
    }

    /**
     * Saves a FeedImage object in the database. This method will save all attributes of the FeedImage object. The
     * contents of FeedComponent-attributes (e.g. the FeedImages's 'feed'-attribute) will not be saved.
     *
     * @param context A context that is used for opening a database connection.
     * @param image   The FeedImage object.
     */
    public static Future<?> setFeedImage(final Context context,
                                         final FeedImage image) {
        return dbExec.submit(new Runnable() {

            @Override
            public void run() {
                PodDBAdapter adapter = new PodDBAdapter(context);
                adapter.open();
                adapter.setImage(image);
                adapter.close();
            }
        });
    }

    private static boolean itemListContains(List<FeedItem> items, long itemId) {
        for (FeedItem item : items) {
            if (item.getId() == itemId) {
                return true;
            }
        }
        return false;
    }
}
