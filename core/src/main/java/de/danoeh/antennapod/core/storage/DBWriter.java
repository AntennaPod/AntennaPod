package de.danoeh.antennapod.core.storage;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.shredzone.flattr4j.model.Flattr;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.asynctask.FlattrClickWorker;
import de.danoeh.antennapod.core.event.FavoritesEvent;
import de.danoeh.antennapod.core.event.FeedItemEvent;
import de.danoeh.antennapod.core.event.MessageEvent;
import de.danoeh.antennapod.core.event.QueueEvent;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedEvent;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetEpisodeAction;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.core.util.flattr.FlattrStatus;
import de.danoeh.antennapod.core.util.flattr.FlattrThing;
import de.danoeh.antennapod.core.util.flattr.SimpleFlattrThing;
import de.greenrobot.event.EventBus;

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
        dbExec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
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
        return dbExec.submit(() -> {
            final FeedMedia media = DBReader.getFeedMedia(mediaId);
            if (media != null) {
                Log.i(TAG, String.format("Requested to delete FeedMedia [id=%d, title=%s, downloaded=%s",
                        media.getId(), media.getEpisodeTitle(), String.valueOf(media.isDownloaded())));
                if (media.isDownloaded()) {
                    // delete downloaded media file
                    File mediaFile = new File(media.getFile_url());
                    if (mediaFile.exists() && !mediaFile.delete()) {
                        MessageEvent evt = new MessageEvent(context.getString(R.string.delete_failed));
                        EventBus.getDefault().post(evt);
                        return;
                    }
                    media.setDownloaded(false);
                    media.setFile_url(null);
                    media.setHasEmbeddedPicture(false);
                    PodDBAdapter adapter = PodDBAdapter.getInstance();
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
                    // Gpodder: queue delete action for synchronization
                    if(GpodnetPreferences.loggedIn()) {
                        FeedItem item = media.getItem();
                        GpodnetEpisodeAction action = new GpodnetEpisodeAction.Builder(item, GpodnetEpisodeAction.Action.DELETE)
                                .currentDeviceId()
                                .currentTimestamp()
                                .build();
                        GpodnetPreferences.enqueueEpisodeAction(action);
                    }
                }
                EventBus.getDefault().post(FeedItemEvent.deletedMedia(Collections.singletonList(media.getItem())));
                EventDistributor.getInstance().sendUnreadItemsUpdateBroadcast();
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
        return dbExec.submit(() -> {
            DownloadRequester requester = DownloadRequester.getInstance();
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(context
                            .getApplicationContext());
            final Feed feed = DBReader.getFeed(feedId);

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
                List<FeedItem> queue = DBReader.getQueue();
                List<FeedItem> removed = new ArrayList<>();
                if (feed.getItems() == null) {
                    DBReader.getFeedItemList(feed);
                }

                for (FeedItem item : feed.getItems()) {
                    if(queue.remove(item)) {
                        removed.add(item);
                    }
                    if (item.getMedia() != null
                            && item.getMedia().isDownloaded()) {
                        File mediaFile = new File(item.getMedia()
                                .getFile_url());
                        mediaFile.delete();
                    } else if (item.getMedia() != null
                            && requester.isDownloadingFile(item.getMedia())) {
                        requester.cancelDownload(context, item.getMedia());
                    }

                    if (item.hasItemImage()) {
                        FeedImage image = item.getImage();
                        if (image.isDownloaded() && image.getFile_url() != null) {
                            File imgFile = new File(image.getFile_url());
                            imgFile.delete();
                        } else if (requester.isDownloadingFile(image)) {
                            requester.cancelDownload(context, item.getImage());
                        }
                    }
                }
                PodDBAdapter adapter = PodDBAdapter.getInstance();
                adapter.open();
                if (removed.size() > 0) {
                    adapter.setQueue(queue);
                    for(FeedItem item : removed) {
                        EventBus.getDefault().post(QueueEvent.irreversibleRemoved(item));
                    }
                }
                adapter.removeFeed(feed);
                adapter.close();

                if (ClientConfig.gpodnetCallbacks.gpodnetEnabled()) {
                    GpodnetPreferences.addRemovedFeed(feed.getDownload_url());
                }
                EventDistributor.getInstance().sendFeedUpdateBroadcast();

                // we assume we also removed download log entries for the feed or its media files.
                // especially important if download or refresh failed, as the user should not be able
                // to retry these
                EventDistributor.getInstance().sendDownloadLogUpdateBroadcast();

                BackupManager backupManager = new BackupManager(context);
                backupManager.dataChanged();
            }
        });
    }

    /**
     * Deletes the entire playback history.
     *
     */
    public static Future<?> clearPlaybackHistory() {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.clearPlaybackHistory();
            adapter.close();
            EventDistributor.getInstance().sendPlaybackHistoryUpdateBroadcast();
        });
    }

    /**
     * Deletes the entire download log.
     */
    public static Future<?> clearDownloadLog() {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.clearDownloadLog();
            adapter.close();
            EventDistributor.getInstance().sendDownloadLogUpdateBroadcast();
        });
    }


    /**
     * Adds a FeedMedia object to the playback history. A FeedMedia object is in the playback history if
     * its playback completion date is set to a non-null value. This method will set the playback completion date to the
     * current date regardless of the current value.
     *
     * @param media   FeedMedia that should be added to the playback history.
     */
    public static Future<?> addItemToPlaybackHistory(final FeedMedia media) {
        return dbExec.submit(() -> {
            Log.d(TAG, "Adding new item to playback history");
            media.setPlaybackCompletionDate(new Date());

            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedMediaPlaybackCompletionDate(media);
            adapter.close();
            EventDistributor.getInstance().sendPlaybackHistoryUpdateBroadcast();

        });
    }

    /**
     * Adds a Download status object to the download log.
     *
     * @param status  The DownloadStatus object.
     */
    public static Future<?> addDownloadStatus(final DownloadStatus status) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setDownloadStatus(status);
            adapter.close();
            EventDistributor.getInstance().sendDownloadLogUpdateBroadcast();
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
        return dbExec.submit(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            final List<FeedItem> queue = DBReader.getQueue(adapter);
            FeedItem item;

            if (queue != null) {
                if (!itemListContains(queue, itemId)) {
                    item = DBReader.getFeedItem(itemId);
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
            }

            adapter.close();
            if (performAutoDownload) {
                DBTasks.autodownloadUndownloadedItems(context);
            }

        });

    }

    public static Future<?> addQueueItem(final Context context,
                                         final FeedItem... items) {
        LongList itemIds = new LongList(items.length);
        for (FeedItem item : items) {
            itemIds.add(item.getId());
            item.addTag(FeedItem.TAG_QUEUE);
        }
        return addQueueItem(context, false, itemIds.toArray());
    }

    /**
     * Appends FeedItem objects to the end of the queue. The 'read'-attribute of all items will be set to true.
     * If a FeedItem is already in the queue, the FeedItem will not change its position in the queue.
     *
     * @param context A context that is used for opening a database connection.
     * @param performAutoDownload true if an auto-download process should be started after the operation.
     * @param itemIds IDs of the FeedItem objects that should be added to the queue.
     */
    public static Future<?> addQueueItem(final Context context, final boolean performAutoDownload,
                                         final long... itemIds) {
        return dbExec.submit(() -> {
            if (itemIds.length > 0) {
                final PodDBAdapter adapter = PodDBAdapter.getInstance();
                adapter.open();
                final List<FeedItem> queue = DBReader.getQueue(adapter);

                if (queue != null) {
                    boolean queueModified = false;
                    LongList markAsUnplayedIds = new LongList();
                    List<QueueEvent> events = new ArrayList<>();
                    List<FeedItem> updatedItems = new ArrayList<>();
                    for (int i = 0; i < itemIds.length; i++) {
                        if (!itemListContains(queue, itemIds[i])) {
                            final FeedItem item = DBReader.getFeedItem(itemIds[i]);


                            if (item != null) {
                                // add item to either front ot back of queue
                                boolean addToFront = UserPreferences.enqueueAtFront();
                                if (addToFront) {
                                    queue.add(i, item);
                                    events.add(QueueEvent.added(item, i));
                                } else {
                                    queue.add(item);
                                    events.add(QueueEvent.added(item, queue.size() - 1));
                                }
                                item.addTag(FeedItem.TAG_QUEUE);
                                updatedItems.add(item);
                                queueModified = true;
                                if (item.isNew()) {
                                    markAsUnplayedIds.add(item.getId());
                                }
                            }
                        }
                    }
                    if (queueModified) {
                        adapter.setQueue(queue);
                        for (QueueEvent event : events) {
                            EventBus.getDefault().post(event);
                        }
                        EventBus.getDefault().post(FeedItemEvent.updated(updatedItems));
                        if (markAsUnplayedIds.size() > 0) {
                            DBWriter.markItemPlayed(FeedItem.UNPLAYED, markAsUnplayedIds.toArray());
                        }
                    }
                }
                adapter.close();
                if (performAutoDownload) {
                    DBTasks.autodownloadUndownloadedItems(context);
                }
            }
        });
    }

    /**
     * Removes all FeedItem objects from the queue.
     *
     */
    public static Future<?> clearQueue() {
        return dbExec.submit(() -> {
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
     * @param item                FeedItem that should be removed.
     * @param performAutoDownload true if an auto-download process should be started after the operation.
     */
    public static Future<?> removeQueueItem(final Context context,
                                            final FeedItem item, final boolean performAutoDownload) {
        return dbExec.submit(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            final List<FeedItem> queue = DBReader.getQueue(adapter);

            if (queue != null) {
                int position = queue.indexOf(item);
                if (position >= 0) {
                    queue.remove(position);
                    adapter.setQueue(queue);
                    item.removeTag(FeedItem.TAG_QUEUE);
                    EventBus.getDefault().post(QueueEvent.removed(item));
                    EventBus.getDefault().post(FeedItemEvent.updated(item));
                } else {
                    Log.w(TAG, "Queue was not modified by call to removeQueueItem");
                }
            } else {
                Log.e(TAG, "removeQueueItem: Could not load queue");
            }
            adapter.close();
            if (performAutoDownload) {
                DBTasks.autodownloadUndownloadedItems(context);
            }
        });

    }

    public static Future<?> addFavoriteItem(final FeedItem item) {
        return dbExec.submit(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            adapter.addFavoriteItem(item);
            adapter.close();
            item.addTag(FeedItem.TAG_FAVORITE);
            EventBus.getDefault().post(FavoritesEvent.added(item));
            EventBus.getDefault().post(FeedItemEvent.updated(item));
        });
    }

    public static Future<?> addFavoriteItemById(final long itemId) {
        return dbExec.submit(() -> {
            final FeedItem item = DBReader.getFeedItem(itemId);
            if (item == null) {
                Log.d(TAG, "Can't find item for itemId " + itemId);
                return;
            }
            final PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            adapter.addFavoriteItem(item);
            adapter.close();
            item.addTag(FeedItem.TAG_FAVORITE);
            EventBus.getDefault().post(FavoritesEvent.added(item));
            EventBus.getDefault().post(FeedItemEvent.updated(item));
        });
    }

    public static Future<?> removeFavoriteItem(final FeedItem item) {
        return dbExec.submit(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            adapter.removeFavoriteItem(item);
            adapter.close();
            item.removeTag(FeedItem.TAG_FAVORITE);
            EventBus.getDefault().post(FavoritesEvent.removed(item));
            EventBus.getDefault().post(FeedItemEvent.updated(item));
        });
    }

    /**
     * Moves the specified item to the top of the queue.
     *  @param itemId          The item to move to the top of the queue
     * @param broadcastUpdate true if this operation should trigger a QueueUpdateBroadcast. This option should be set to
     */
    public static Future<?> moveQueueItemToTop(final long itemId, final boolean broadcastUpdate) {
        return dbExec.submit(() -> {
            LongList queueIdList = DBReader.getQueueIDList();
            int index = queueIdList.indexOf(itemId);
            if (index >=0) {
                moveQueueItemHelper(index, 0, broadcastUpdate);
            } else {
                Log.e(TAG, "moveQueueItemToTop: item not found");
            }
        });
    }

    /**
     * Moves the specified item to the bottom of the queue.
     *  @param itemId          The item to move to the bottom of the queue
     * @param broadcastUpdate true if this operation should trigger a QueueUpdateBroadcast. This option should be set to
     */
    public static Future<?> moveQueueItemToBottom(final long itemId,
                                                  final boolean broadcastUpdate) {
        return dbExec.submit(() -> {
            LongList queueIdList = DBReader.getQueueIDList();
            int index = queueIdList.indexOf(itemId);
            if (index >= 0) {
                moveQueueItemHelper(index, queueIdList.size() - 1,
                    broadcastUpdate);
            } else {
                Log.e(TAG, "moveQueueItemToBottom: item not found");
            }
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
    public static Future<?> moveQueueItem(final int from,
                                          final int to, final boolean broadcastUpdate) {
        return dbExec.submit(() -> moveQueueItemHelper(from, to, broadcastUpdate));
    }

    /**
     * Changes the position of a FeedItem in the queue.
     * <p/>
     * This function must be run using the ExecutorService (dbExec).
     *
     * @param from            Source index. Must be in range 0..queue.size()-1.
     * @param to              Destination index. Must be in range 0..queue.size()-1.
     * @param broadcastUpdate true if this operation should trigger a QueueUpdateBroadcast. This option should be set to
     *                        false if the caller wants to avoid unexpected updates of the GUI.
     * @throws IndexOutOfBoundsException if (to < 0 || to >= queue.size()) || (from < 0 || from >= queue.size())
     */
    private static void moveQueueItemHelper(final int from,
                                            final int to, final boolean broadcastUpdate) {
        final PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        final List<FeedItem> queue = DBReader.getQueue(adapter);

        if (queue != null) {
            if (from >= 0 && from < queue.size() && to >= 0 && to < queue.size()) {
                final FeedItem item = queue.remove(from);
                queue.add(to, item);

                adapter.setQueue(queue);
                if (broadcastUpdate) {
                    EventBus.getDefault().post(QueueEvent.moved(item, to));
                }
            }
        } else {
            Log.e(TAG, "moveQueueItemHelper: Could not load queue");
        }
        adapter.close();
    }

    /*
     * Sets the 'read'-attribute of all specified FeedItems
     *
     * @param played  New value of the 'read'-attribute, one of FeedItem.PLAYED, FeedItem.NEW,
     *                FeedItem.UNPLAYED
     * @param itemIds IDs of the FeedItems.
     */
    public static Future<?> markItemPlayed(final int played, final long... itemIds) {
        return markItemPlayed(played, true, itemIds);
    }

    /*
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
        return dbExec.submit(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedItemRead(played, itemIds);
            adapter.close();
            if(broadcastUpdate) {
                EventDistributor.getInstance().sendUnreadItemsUpdateBroadcast();
            }
        });
    }


    /**
     * Sets the 'read'-attribute of a FeedItem to the specified value.
     *  @param item               The FeedItem object
     * @param played             New value of the 'read'-attribute one of FeedItem.PLAYED,
     *                           FeedItem.NEW, FeedItem.UNPLAYED
     * @param resetMediaPosition true if this method should also reset the position of the FeedItem's FeedMedia object.
     */
    public static Future<?> markItemPlayed(FeedItem item, int played, boolean resetMediaPosition) {
        long mediaId = (item.hasMedia()) ? item.getMedia().getId() : 0;
        return markItemPlayed(item.getId(), played, mediaId, resetMediaPosition);
    }

    private static Future<?> markItemPlayed(final long itemId,
                                            final int played,
                                            final long mediaId,
                                            final boolean resetMediaPosition) {
        return dbExec.submit(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedItemRead(played, itemId, mediaId,
                    resetMediaPosition);
            adapter.close();

            EventDistributor.getInstance().sendUnreadItemsUpdateBroadcast();
        });
    }

    /**
     * Sets the 'read'-attribute of all NEW FeedItems of a specific Feed to UNPLAYED.
     *
     * @param feedId  ID of the Feed.
     */
    public static Future<?> markFeedSeen(final long feedId) {
        return dbExec.submit(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedItems(FeedItem.NEW, FeedItem.UNPLAYED, feedId);
            adapter.close();

            EventDistributor.getInstance().sendUnreadItemsUpdateBroadcast();
        });
    }

    /**
     * Sets the 'read'-attribute of all FeedItems of a specific Feed to PLAYED.
     *
     * @param feedId  ID of the Feed.
     */
    public static Future<?> markFeedRead(final long feedId) {
        return dbExec.submit(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedItems(FeedItem.PLAYED, feedId);
            adapter.close();

            EventDistributor.getInstance().sendUnreadItemsUpdateBroadcast();
        });
    }

    /**
     * Sets the 'read'-attribute of all FeedItems to PLAYED.
     */
    public static Future<?> markAllItemsRead() {
        return dbExec.submit(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedItems(FeedItem.PLAYED);
            adapter.close();

            EventDistributor.getInstance().sendUnreadItemsUpdateBroadcast();
        });
    }

    /**
     * Sets the 'read'-attribute of all NEW FeedItems to UNPLAYED.
     */
    public static Future<?> markNewItemsSeen() {
        return dbExec.submit(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedItems(FeedItem.NEW, FeedItem.UNPLAYED);
            adapter.close();

            EventDistributor.getInstance().sendUnreadItemsUpdateBroadcast();
        });
    }

    static Future<?> addNewFeed(final Context context, final Feed... feeds) {
        return dbExec.submit(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setCompleteFeed(feeds);
            adapter.close();

            if (ClientConfig.gpodnetCallbacks.gpodnetEnabled()) {
                for (Feed feed : feeds) {
                    GpodnetPreferences.addAddedFeed(feed.getDownload_url());
                }
            }

            BackupManager backupManager = new BackupManager(context);
            backupManager.dataChanged();
        });
    }

    static Future<?> setCompleteFeed(final Feed... feeds) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setCompleteFeed(feeds);
            adapter.close();
        });
    }

    /**
     * Saves a FeedMedia object in the database. This method will save all attributes of the FeedMedia object. The
     * contents of FeedComponent-attributes (e.g. the FeedMedia's 'item'-attribute) will not be saved.
     *
     * @param media   The FeedMedia object.
     */
    public static Future<?> setFeedMedia(final FeedMedia media) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setMedia(media);
            adapter.close();
        });
    }

    /**
     * Saves the 'position', 'duration' and 'last played time' attributes of a FeedMedia object
     *
     * @param media   The FeedMedia object.
     */
    public static Future<?> setFeedMediaPlaybackInformation(final FeedMedia media) {
        return dbExec.submit(() -> {
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
     * @param item    The FeedItem object.
     */
    public static Future<?> setFeedItem(final FeedItem item) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setSingleFeedItem(item);
            adapter.close();
            EventBus.getDefault().post(FeedItemEvent.updated(item));
        });
    }

    /**
     * Saves a FeedImage object in the database. This method will save all attributes of the FeedImage object. The
     * contents of FeedComponent-attributes (e.g. the FeedImages's 'feed'-attribute) will not be saved.
     *
     * @param image   The FeedImage object.
     */
    public static Future<?> setFeedImage(final FeedImage image) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setImage(image);
            adapter.close();
        });
    }

    /**
     * Updates download URL of a feed
     */
    public static Future<?> updateFeedDownloadURL(final String original, final String updated) {
        Log.d(TAG, "updateFeedDownloadURL(original: " + original + ", updated: " + updated +")");
        return dbExec.submit(() -> {
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
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedPreferences(preferences);
            adapter.close();
            EventDistributor.getInstance().sendFeedUpdateBroadcast();
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

    /**
     * Saves the FlattrStatus of a FeedItem object in the database.
     *
     * @param startFlattrClickWorker true if FlattrClickWorker should be started after the FlattrStatus has been saved
     */
    private static Future<?> setFeedItemFlattrStatus(final Context context,
                                                     final FeedItem item,
                                                     final boolean startFlattrClickWorker) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedItemFlattrStatus(item);
            adapter.close();
            if (startFlattrClickWorker) {
                new FlattrClickWorker(context).executeAsync();
            }
        });
    }

    /**
     * Saves the FlattrStatus of a Feed object in the database.
     *
     * @param startFlattrClickWorker true if FlattrClickWorker should be started after the FlattrStatus has been saved
     */
    private static Future<?> setFeedFlattrStatus(final Context context,
                                                 final Feed feed,
                                                 final boolean startFlattrClickWorker) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedFlattrStatus(feed);
            adapter.close();
            if (startFlattrClickWorker) {
                new FlattrClickWorker(context).executeAsync();
            }
        });
    }

    /**
     * Saves if a feed's last update failed
     *
     * @param lastUpdateFailed true if last update failed
     */
    public static Future<?> setFeedLastUpdateFailed(final long feedId,
                                                    final boolean lastUpdateFailed) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedLastUpdateFailed(feedId, lastUpdateFailed);
            adapter.close();
        });
    }

    public static Future<?> setFeedCustomTitle(Feed feed) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedCustomTitle(feed.getId(), feed.getCustomTitle());
            adapter.close();
            EventDistributor.getInstance().sendFeedUpdateBroadcast();
        });
    }


    /**
     * format an url for querying the database
     * (postfix a / and apply percent-encoding)
     */
    private static String formatURIForQuery(String uri) {
        try {
            return URLEncoder.encode(uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage());
            return "";
        }
    }


    /**
     * Set flattr status of the passed thing (either a FeedItem or a Feed)
     *
     * @param context
     * @param thing
     * @param startFlattrClickWorker true if FlattrClickWorker should be started after the FlattrStatus has been saved
     * @return
     */
    public static Future<?> setFlattredStatus(Context context, FlattrThing thing, boolean startFlattrClickWorker) {
        // must propagate this to back db
        if (thing instanceof FeedItem) {
            return setFeedItemFlattrStatus(context, (FeedItem) thing, startFlattrClickWorker);
        } else if (thing instanceof Feed) {
            return setFeedFlattrStatus(context, (Feed) thing, startFlattrClickWorker);
        }  else if (thing instanceof SimpleFlattrThing) {
            // SimpleFlattrThings are generated on the fly and do not have DB backing
        } else {
            Log.e(TAG, "flattrQueue processing - thing is neither FeedItem nor Feed nor SimpleFlattrThing");
        }

        return null;
    }

    /**
     * Reset flattr status to unflattrd for all items
     */
    public static Future<?> clearAllFlattrStatus() {
        Log.d(TAG, "clearAllFlattrStatus()");
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.clearAllFlattrStatus();
            adapter.close();
        });
    }

    /**
     * Set flattr status of the feeds/feeditems in flattrList to flattred at the given timestamp,
     * where the information has been retrieved from the flattr API
     */
    public static Future<?> setFlattredStatus(final List<Flattr> flattrList) {
        Log.d(TAG, "setFlattredStatus to status retrieved from flattr api running with " + flattrList.size() + " items");
        // clear flattr status in db
        clearAllFlattrStatus();

        // submit list with flattred things having normalized URLs to db
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            for (Flattr flattr : flattrList) {
                adapter.setItemFlattrStatus(formatURIForQuery(flattr.getThing().getUrl()), new FlattrStatus(flattr.getCreated().getTime()));
            }
            adapter.close();
        });
    }

    /**
     * Sort the FeedItems in the queue with the given Comparator.
     * @param comparator      FeedItem comparator
     * @param broadcastUpdate true if this operation should trigger a QueueUpdateBroadcast. This option should be set to
     */
    public static Future<?> sortQueue(final Comparator<FeedItem> comparator, final boolean broadcastUpdate) {
        return dbExec.submit(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            final List<FeedItem> queue = DBReader.getQueue(adapter);

            if (queue != null) {
                Collections.sort(queue, comparator);
                adapter.setQueue(queue);
                if (broadcastUpdate) {
                    EventBus.getDefault().post(QueueEvent.sorted(queue));
                }
            } else {
                Log.e(TAG, "sortQueue: Could not load queue");
            }
            adapter.close();
        });
    }

    /**
     * Sets the 'auto_download'-attribute of specific FeedItem.
     *
     * @param feedItem  FeedItem.
     * @param autoDownload true enables auto download, false disables it
     */
    public static Future<?> setFeedItemAutoDownload(final FeedItem feedItem,
                                                    final boolean autoDownload) {
        return dbExec.submit(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedItemAutoDownload(feedItem, autoDownload ? 1 : 0);
            adapter.close();
            EventDistributor.getInstance().sendUnreadItemsUpdateBroadcast();
        });
    }

    public static Future<?> saveFeedItemAutoDownloadFailed(final FeedItem feedItem) {
        return dbExec.submit(() -> {
            int failedAttempts = feedItem.getFailedAutoDownloadAttempts() + 1;
            long autoDownload;
            if(!feedItem.getAutoDownload() || failedAttempts >= 10) {
                autoDownload = 0; // giving up, disable auto download
                feedItem.setAutoDownload(false);
            } else {
                long now = System.currentTimeMillis();
                autoDownload = (now / 10) * 10 + failedAttempts;
            }
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedItemAutoDownload(feedItem, autoDownload);
            adapter.close();
            EventDistributor.getInstance().sendUnreadItemsUpdateBroadcast();
        });
    }

    /**
     * Sets the 'auto_download'-attribute of specific FeedItem.
     *
     * @param feed         This feed's episodes will be processed.
     * @param autoDownload If true, auto download will be enabled for the feed's episodes. Else,
     */
    public static Future<?> setFeedsItemsAutoDownload(final Feed feed,
                                                      final boolean autoDownload) {
        Log.d(TAG, (autoDownload ? "Enabling" : "Disabling") + " auto download for items of feed " + feed.getId());
        return dbExec.submit(() -> {
            final PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedsItemsAutoDownload(feed, autoDownload);
            adapter.close();
            EventDistributor.getInstance().sendUnreadItemsUpdateBroadcast();
        });
    }


    /**
     * Set filter of the feed
     *  @param feedId  The feed's ID
     * @param filterValues Values that represent properties to filter by
     */
    public static Future<?> setFeedItemsFilter(final long feedId,
                                               final Set<String> filterValues) {
        Log.d(TAG, "setFeedItemsFilter() called with: " + "feedId = [" + feedId + "], filterValues = [" + filterValues + "]");
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setFeedItemFilter(feedId, filterValues);
            adapter.close();
            EventBus.getDefault().post(new FeedEvent(FeedEvent.Action.FILTER_CHANGED, feedId));
        });
    }

}
