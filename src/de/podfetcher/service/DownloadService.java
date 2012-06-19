/** 
 * Registers a DownloadReceiver and waits for all Downloads 
 * to complete, then stops
 * */

package de.podfetcher.service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.podfetcher.activity.DownloadActivity;
import de.podfetcher.activity.MediaplayerActivity;
import de.podfetcher.asynctask.DownloadStatus;
import de.podfetcher.feed.*;
import de.podfetcher.service.PlaybackService.LocalBinder;
import de.podfetcher.storage.DownloadRequester;
import de.podfetcher.syndication.handler.FeedHandler;
import android.R;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;

public class DownloadService extends Service {
	private static final String TAG = "DownloadService";

	public static String ACTION_ALL_FEED_DOWNLOADS_COMPLETED = "action.de.podfetcher.storage.all_feed_downloads_completed";
	public static final String ACTION_FEED_SYNC_COMPLETED = "action.de.podfetcher.service.feed_sync_completed";
	
	public static final String ACTION_DOWNLOAD_HANDLED = "action.de.podfetcher.service.download_handled";
	/** True if handled feed has an image. */
	public static final String EXTRA_FEED_HAS_IMAGE = "extra.de.podfetcher.service.feed_has_image";
	/** ID of DownloadStatus. */
	public static final String EXTRA_STATUS_ID = "extra.de.podfetcher.service.feedfile_id";
	public static final String EXTRA_DOWNLOAD_ID = "extra.de.podfetcher.service.download_id";
	public static final String EXTRA_IMAGE_DOWNLOAD_ID = "extra.de.podfetcher.service.image_download_id";
	

	private ExecutorService syncExecutor;
	private DownloadRequester requester;
	private FeedManager manager;
	private NotificationCompat.Builder notificationBuilder;
	private int NOTIFICATION_ID = 2;
	/** Needed to determine the duration of a media file */
	private MediaPlayer mediaplayer;
	private DownloadManager downloadManager;

	private volatile boolean shutdownInitiated = false;

	private final IBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		public DownloadService getService() {
			return DownloadService.this;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		queryDownloads();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "Service started");
		registerReceiver(downloadReceiver, createIntentFilter());
		syncExecutor = Executors.newSingleThreadExecutor();
		manager = FeedManager.getInstance();
		requester = DownloadRequester.getInstance();
		mediaplayer = new MediaPlayer();
		downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
		setupNotification();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "Service shutting down");
		sendBroadcast(new Intent(ACTION_FEED_SYNC_COMPLETED));
		mediaplayer.release();
		unregisterReceiver(downloadReceiver);
	}

	private IntentFilter createIntentFilter() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		return filter;
	}

	/** Shuts down Executor service and prepares for shutdown */
	private void initiateShutdown() {
		Log.d(TAG, "Initiating shutdown");
		// Wait until PoolExecutor is done
		Thread waiter = new Thread() {
			@Override
			public void run() {
				syncExecutor.shutdown();
				try {
					Log.d(TAG, "Starting to wait for termination");
					boolean b = syncExecutor.awaitTermination(20L,
							TimeUnit.SECONDS);
					Log.d(TAG, "Stopping waiting for termination; Result : "
							+ b);

					stopSelf();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		waiter.start();
	}

	private void setupNotification() {
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, new Intent(
				this, DownloadActivity.class),
				PendingIntent.FLAG_UPDATE_CURRENT);

		Bitmap icon = BitmapFactory.decodeResource(null,
				R.drawable.stat_notify_sync_noanim);
		notificationBuilder = new NotificationCompat.Builder(this)
				.setContentTitle("Downloading Podcast data")
				.setContentText(
						requester.getNumberOfDownloads() + " Downloads left")
				.setOngoing(true).setContentIntent(pIntent).setLargeIcon(icon)
				.setSmallIcon(R.drawable.stat_notify_sync_noanim);

		startForeground(NOTIFICATION_ID, notificationBuilder.getNotification());
		Log.d(TAG, "Notification set up");
	}

	private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int status = -1;
			boolean successful = false;
			int reason = 0;
			Log.d(TAG, "Received 'Download Complete' - message.");
			long downloadId = intent.getLongExtra(
					DownloadManager.EXTRA_DOWNLOAD_ID, 0);
			// get status
			DownloadManager.Query q = new DownloadManager.Query();
			q.setFilterById(downloadId);
			Cursor c = downloadManager.query(q);
			if (c.moveToFirst()) {
				status = c.getInt(c
						.getColumnIndex(DownloadManager.COLUMN_STATUS));
			}
			FeedFile download = requester.getFeedFile(downloadId);
			if (download != null) {
				if (status == DownloadManager.STATUS_SUCCESSFUL) {

					if (download.getClass() == Feed.class) {
						handleCompletedFeedDownload(context, (Feed) download);
					} else if (download.getClass() == FeedImage.class) {
						handleCompletedImageDownload(context,
								(FeedImage) download);
					} else if (download.getClass() == FeedMedia.class) {
						handleCompletedFeedMediaDownload(context,
								(FeedMedia) download);
					}
					successful = true;
					queryDownloads();
				} else if (status == DownloadManager.STATUS_FAILED) {
					reason = c.getInt(c
							.getColumnIndex(DownloadManager.COLUMN_REASON));
					Log.d(TAG, "reason code is " + reason);
					successful = false;
					long statusId = manager.addDownloadStatus(context, new DownloadStatus(download,
							reason, successful));
					sendDownloadHandledIntent(download.getDownloadId(), statusId, false, 0);
					download.setDownloadId(0);
					
				}
				
				c.close();
			}
		}

	};
	
	private void sendDownloadHandledIntent(long downloadId, long statusId, boolean feedHasImage, long imageDownloadId) {
		Intent intent = new Intent(ACTION_DOWNLOAD_HANDLED);
		intent.putExtra(EXTRA_DOWNLOAD_ID, downloadId);
		intent.putExtra(EXTRA_STATUS_ID, statusId);
		intent.putExtra(EXTRA_FEED_HAS_IMAGE, feedHasImage);
		if (feedHasImage) {
			intent.putExtra(EXTRA_IMAGE_DOWNLOAD_ID, imageDownloadId);
		}
		sendBroadcast(intent);
	}

	/** Check if there's something else to download, otherwise stop */
	public synchronized void queryDownloads() {
		int numOfDownloads = requester.getNumberOfDownloads();
		if (!shutdownInitiated && numOfDownloads == 0) {
			shutdownInitiated = true;
			initiateShutdown();
		} else {
			// update notification
			notificationBuilder.setContentText(numOfDownloads
					+ " Downloads left");
			NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			nm.notify(NOTIFICATION_ID, notificationBuilder.getNotification());
		}
	}

	/** Is called whenever a Feed is downloaded */
	private void handleCompletedFeedDownload(Context context, Feed feed) {
		Log.d(TAG, "Handling completed Feed Download");
		syncExecutor.execute(new FeedSyncThread(feed, this));

	}

	/** Is called whenever a Feed-Image is downloaded */
	private void handleCompletedImageDownload(Context context, FeedImage image) {
		Log.d(TAG, "Handling completed Image Download");
		syncExecutor.execute(new ImageHandlerThread(image, this));
	}

	/** Is called whenever a FeedMedia is downloaded. */
	private void handleCompletedFeedMediaDownload(Context context,
			FeedMedia media) {
		Log.d(TAG, "Handling completed FeedMedia Download");
		syncExecutor.execute(new MediaHandlerThread(media, this));
	}

	/**
	 * Takes a single Feed, parses the corresponding file and refreshes
	 * information in the manager
	 */
	class FeedSyncThread implements Runnable {
		private static final String TAG = "FeedSyncThread";

		private Feed feed;
		private DownloadService service;

		public FeedSyncThread(Feed feed, DownloadService service) {
			this.feed = feed;
			this.service = service;
		}

		public void run() {
			long imageId = 0;
			boolean hasImage = false;
			
			FeedManager manager = FeedManager.getInstance();
			FeedHandler handler = new FeedHandler();
			feed.setDownloaded(true);
			feed = handler.parseFeed(feed);
			Log.d(TAG, feed.getTitle() + " parsed");
			// Download Feed Image if provided
			if (feed.getImage() != null) {
				Log.d(TAG, "Feed has image; Downloading....");
				imageId = requester.downloadImage(service, feed.getImage());
				hasImage = true;
			}
			requester.removeDownload(feed);

			cleanup();
			long statusId = manager.addDownloadStatus(service, new DownloadStatus(feed, 0, true));
			sendDownloadHandledIntent(feed.getDownloadId(), statusId, hasImage, imageId);
			feed.setDownloadId(0);
			// Save information of feed in DB
			manager.updateFeed(service, feed);
			queryDownloads();
		}

		/** Delete files that aren't needed anymore */
		private void cleanup() {
			if (new File(feed.getFile_url()).delete())
				Log.d(TAG, "Successfully deleted cache file.");
			else
				Log.e(TAG, "Failed to delete cache file.");
			feed.setFile_url(null);
		}

	}

	/** Handles a completed image download. */
	class ImageHandlerThread implements Runnable {
		private FeedImage image;
		private DownloadService service;

		public ImageHandlerThread(FeedImage image, DownloadService service) {
			this.image = image;
			this.service = service;
		}

		@Override
		public void run() {
			image.setDownloaded(true);
			requester.removeDownload(image);
			
			long statusId = manager.addDownloadStatus(service, new DownloadStatus(image, 0, true));
			sendDownloadHandledIntent(image.getDownloadId(), statusId, false, 0);
			image.setDownloadId(0);
			
			manager.setFeedImage(service, image);
			queryDownloads();
		}
	}

	/** Handles a completed media download. */
	class MediaHandlerThread implements Runnable {
		private FeedMedia media;
		private DownloadService service;

		public MediaHandlerThread(FeedMedia media, DownloadService service) {
			super();
			this.media = media;
			this.service = service;
		}

		@Override
		public void run() {
			requester.removeDownload(media);
			media.setDownloaded(true);
			// Get duration
			try {
				mediaplayer.setDataSource(media.getFile_url());
				mediaplayer.prepare();
			} catch (IOException e) {
				e.printStackTrace();
			}
			media.setDuration(mediaplayer.getDuration());
			Log.d(TAG, "Duration of file is " + media.getDuration());
			mediaplayer.reset();
			long statusId = manager.addDownloadStatus(service, new DownloadStatus(media, 0, true));
			sendDownloadHandledIntent(media.getDownloadId(), statusId, false, 0);
			media.setDownloadId(0);
			manager.setFeedMedia(service, media);
			queryDownloads();
		}
	}
}
