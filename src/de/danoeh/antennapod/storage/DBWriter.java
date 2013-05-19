package de.danoeh.antennapod.storage;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.preferences.PlaybackPreferences;
import de.danoeh.antennapod.service.PlaybackService;
import de.danoeh.antennapod.service.download.DownloadStatus;

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

	public static void deleteFeedMediaOfItem(final Context context,
			final long itemId) {
		dbExec.submit(new Runnable() {
			@Override
			public void run() {
				final FeedItem item = DBReader.getFeedItem(context, itemId);
				if (item != null && item.hasMedia()) {
					final FeedMedia media = item.getMedia();
					boolean result = false;
					if (media.isDownloaded()) {
						// delete downloaded media file
						File mediaFile = new File(media.getFile_url());
						if (mediaFile.exists()) {
							result = mediaFile.delete();
						}
						media.setDownloaded(false);
						media.setFile_url(null);
						setFeedMedia(context, media);

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

	public static void deleteFeed(final Context context, final long feedId) {
		dbExec.submit(new Runnable() {
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

					PodDBAdapter adapter = new PodDBAdapter(context);
					adapter.open();
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

	public static void clearPlaybackHistory(final Context context) {
		dbExec.submit(new Runnable() {

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

	public static void addItemToPlaybackHistory(final Context context,
			final FeedItem item) {
		if (item.hasMedia()
				&& item.getMedia().getPlaybackCompletionDate() != null) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Adding new item to playback history");
			EventDistributor.getInstance().sendPlaybackHistoryUpdateBroadcast();
		}

	}

	private static void cleanupDownloadLog(final PodDBAdapter adapter) {
		final int DOWNLOAD_LOG_SIZE = 50;
		final long logSize = adapter.getDownloadLogSize();
		if (logSize > DOWNLOAD_LOG_SIZE) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Cleaning up download log");
			adapter.removeDownloadLogItems(logSize - DOWNLOAD_LOG_SIZE);
		}
	}

	public static void addDownloadStatus(final Context context,
			final DownloadStatus status) {
		dbExec.submit(new Runnable() {

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

	public static void addQueueItemAt(final Context context, final long itemId,
			final int index, final boolean performAutoDownload) {
		dbExec.submit(new Runnable() {

			@Override
			public void run() {
				final PodDBAdapter adapter = new PodDBAdapter(context);
				adapter.open();
				final List<FeedItem> queue = DBReader
						.getQueue(context, adapter);
				FeedItem item = null;

				if (queue != null) {
					boolean queueModified = false;
					boolean unreadItemsModfied = false;

					if (!itemListContains(queue, itemId)) {
						item = DBReader.getFeedItem(context, itemId);
						if (item != null) {
							queue.add(index, item);
							queueModified = true;
							if (!item.isRead()) {
								item.setRead(true);
								unreadItemsModfied = true;
							}
						}
					}
					if (queueModified) {
						adapter.setQueue(queue);
						EventDistributor.getInstance()
								.sendQueueUpdateBroadcast();
					}
					if (unreadItemsModfied && item != null) {
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

	public static void addQueueItem(final Context context,
			final long... itemIds) {
		if (itemIds.length > 0) {
			dbExec.submit(new Runnable() {

				@Override
				public void run() {
					final PodDBAdapter adapter = new PodDBAdapter(context);
					adapter.open();
					final List<FeedItem> queue = DBReader.getQueue(context,
							adapter);

					if (queue != null) {
						boolean queueModified = false;
						boolean unreadItemsModfied = false;
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
										unreadItemsModfied = true;
									}
								}
							}
						}
						if (queueModified) {
							adapter.setQueue(queue);
							EventDistributor.getInstance()
									.sendQueueUpdateBroadcast();
						}
						if (unreadItemsModfied) {
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
			});
		}
	}

	public static void clearQueue(final Context context) {
		dbExec.submit(new Runnable() {

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

	public static void removeQueueItem(final Context context,
			final long itemId, final boolean performAutoDownload) {
		dbExec.submit(new Runnable() {

			@Override
			public void run() {
				final PodDBAdapter adapter = new PodDBAdapter(context);
				adapter.open();
				final List<FeedItem> queue = DBReader
						.getQueue(context, adapter);
				FeedItem item = null;

				if (queue != null) {
					boolean queueModified = false;

					if (itemListContains(queue, itemId)) {
						item = DBReader.getFeedItem(context, itemId);
						if (item != null) {
							queue.remove(item);
							queueModified = true;
						}
					}
					if (queueModified) {
						adapter.setQueue(queue);
						EventDistributor.getInstance()
								.sendQueueUpdateBroadcast();
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

	public void moveQueueItem(final Context context, final int from,
			final int to, final boolean broadcastUpdate) {
		dbExec.submit(new Runnable() {

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
				}
				adapter.close();
			}
		});
	}

	public static void markItemRead(final Context context, final long itemId,
			final boolean read) {
		markItemRead(context, itemId, read, 0, false);
	}

	public static void markItemRead(final Context context, final long itemId,
			final boolean read, final long mediaId,
			final boolean resetMediaPosition) {
		dbExec.submit(new Runnable() {

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

	public static void markFeedRead(final Context context, final long feedId) {
		dbExec.submit(new Runnable() {

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

	public static void markAllItemsRead(final Context context) {
		dbExec.submit(new Runnable() {

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

	static void addNewFeed(final Context context, final Feed feed) {
		dbExec.submit(new Runnable() {

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
	
	static void setCompleteFeed(final Context context, final Feed feed) {
		dbExec.submit(new Runnable() {

			@Override
			public void run() {
				PodDBAdapter adapter = new PodDBAdapter(context);
				adapter.open();
				adapter.setCompleteFeed(feed);
				adapter.close();
				
				EventDistributor.getInstance().sendFeedUpdateBroadcast();
			}});

	}

	private static void setFeedMedia(final Context context,
			final FeedMedia media) {
		dbExec.submit(new Runnable() {

			@Override
			public void run() {
				PodDBAdapter adapter = new PodDBAdapter(context);
				adapter.open();
				adapter.setMedia(media);
				adapter.close();
			}});
		

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
