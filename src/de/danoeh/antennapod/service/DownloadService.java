/** 
 * Registers a DownloadReceiver and waits for all Downloads 
 * to complete, then stops
 * */

package de.danoeh.antennapod.service;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.activity.DownloadActivity;
import de.danoeh.antennapod.activity.AudioplayerActivity;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.asynctask.DownloadObserver;
import de.danoeh.antennapod.asynctask.DownloadStatus;
import de.danoeh.antennapod.feed.*;
import de.danoeh.antennapod.service.PlaybackService.LocalBinder;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.syndication.handler.FeedHandler;
import de.danoeh.antennapod.syndication.handler.UnsupportedFeedtypeException;
import de.danoeh.antennapod.util.DownloadError;
import de.danoeh.antennapod.util.InvalidFeedException;
import android.annotation.SuppressLint;
import android.app.Notification;
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
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;

public class DownloadService extends Service {
	private static final String TAG = "DownloadService";

	public static String ACTION_ALL_FEED_DOWNLOADS_COMPLETED = "action.de.danoeh.antennapod.storage.all_feed_downloads_completed";

	public static final String ACTION_DOWNLOAD_HANDLED = "action.de.danoeh.antennapod.service.download_handled";
	/** True if handled feed has an image. */
	public static final String EXTRA_FEED_HAS_IMAGE = "extra.de.danoeh.antennapod.service.feed_has_image";
	/** ID of DownloadStatus. */
	public static final String EXTRA_STATUS_ID = "extra.de.danoeh.antennapod.service.feedfile_id";
	public static final String EXTRA_DOWNLOAD_ID = "extra.de.danoeh.antennapod.service.download_id";
	public static final String EXTRA_IMAGE_DOWNLOAD_ID = "extra.de.danoeh.antennapod.service.image_download_id";

	private ArrayList<DownloadStatus> completedDownloads;

	private ExecutorService syncExecutor;
	private DownloadRequester requester;
	private FeedManager manager;
	private NotificationCompat.Builder notificationBuilder;
	private int NOTIFICATION_ID = 2;
	private int REPORT_ID = 3;
	/** Needed to determine the duration of a media file */
	private MediaPlayer mediaplayer;
	private DownloadManager downloadManager;

	private DownloadObserver downloadObserver;

	private volatile boolean shutdownInitiated = false;
	/** True if service is running. */
	public static boolean isRunning = false;

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

	@SuppressLint("NewApi")
	@Override
	public void onCreate() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Service started");
		isRunning = true;
		completedDownloads = new ArrayList<DownloadStatus>();
		registerReceiver(downloadReceiver, createIntentFilter());
		syncExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setPriority(Thread.MIN_PRIORITY);
				t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

					@Override
					public void uncaughtException(Thread thread, Throwable ex) {
						Log.e(TAG, "Thread exited with uncaught exception");
						ex.printStackTrace();
						queryDownloads();
					}});
				return t;
			}
		});
		manager = FeedManager.getInstance();
		requester = DownloadRequester.getInstance();
		mediaplayer = new MediaPlayer();
		downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
		downloadObserver = new DownloadObserver(this);
		setupNotification();
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			downloadObserver.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			downloadObserver.execute();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Service shutting down");
		isRunning = false;
		mediaplayer.release();
		unregisterReceiver(downloadReceiver);
		downloadObserver.cancel(true);
		createReport();
	}

	private IntentFilter createIntentFilter() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		return filter;
	}

	/** Shuts down Executor service and prepares for shutdown */
	private void initiateShutdown() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Initiating shutdown");
		// Wait until PoolExecutor is done
		Thread waiter = new Thread() {
			@Override
			public void run() {
				syncExecutor.shutdown();
				try {
					if (AppConfig.DEBUG)
						Log.d(TAG, "Starting to wait for termination");
					boolean b = syncExecutor.awaitTermination(20L,
							TimeUnit.SECONDS);
					if (AppConfig.DEBUG)
						Log.d(TAG,
								"Stopping waiting for termination; Result : "
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
				android.R.drawable.stat_notify_sync_noanim);
		notificationBuilder = new NotificationCompat.Builder(this)
				.setContentTitle("Downloading Podcast data")
				.setContentText(
						requester.getNumberOfDownloads() + " Downloads left")
				.setOngoing(true).setContentIntent(pIntent).setLargeIcon(icon)
				.setSmallIcon(android.R.drawable.stat_notify_sync_noanim);

		startForeground(NOTIFICATION_ID, notificationBuilder.getNotification());
		if (AppConfig.DEBUG)
			Log.d(TAG, "Notification set up");
	}

	private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
		@SuppressLint("NewApi")
		@Override
		public void onReceive(final Context context, final Intent intent) {
			AsyncTask<Void, Void, Void> handler = new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... params) {
					int status = -1;
					boolean successful = false;
					int reason = 0;
					if (AppConfig.DEBUG)
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
					if (downloadId == 0) {
						Log.d(TAG, "Download ID was null");
					}
					FeedFile download = requester.getFeedFile(downloadId);
					if (download != null) {
						if (status == DownloadManager.STATUS_SUCCESSFUL) {

							if (download.getClass() == Feed.class) {
								handleCompletedFeedDownload(context,
										(Feed) download);
							} else if (download.getClass() == FeedImage.class) {
								handleCompletedImageDownload(context,
										(FeedImage) download);
							} else if (download.getClass() == FeedMedia.class) {
								handleCompletedFeedMediaDownload(context,
										(FeedMedia) download);
							}
							successful = true;

						} else if (status == DownloadManager.STATUS_FAILED) {
							reason = c
									.getInt(c
											.getColumnIndex(DownloadManager.COLUMN_REASON));
							Log.e(TAG, "Download failed");
							Log.e(TAG, "reason code is " + reason);
							successful = false;
							long statusId = saveDownloadStatus(new DownloadStatus(
									download, reason, successful));
							requester.removeDownload(download);
							sendDownloadHandledIntent(download.getDownloadId(),
									statusId, false, 0);
							download.setDownloadId(0);

						}
						queryDownloads();
						c.close();
					}
					return null;
				}
			};
			if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
				handler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				handler.execute();
			}
		}

	};

	/**
	 * Adds a new DownloadStatus object to the list of completed downloads and
	 * saves it in the database
	 * 
	 * @param status
	 *            the download that is going to be saved
	 */
	private long saveDownloadStatus(DownloadStatus status) {
		completedDownloads.add(status);
		return manager.addDownloadStatus(this, status);
	}

	private void sendDownloadHandledIntent(long downloadId, long statusId,
			boolean feedHasImage, long imageDownloadId) {
		Intent intent = new Intent(ACTION_DOWNLOAD_HANDLED);
		intent.putExtra(EXTRA_DOWNLOAD_ID, downloadId);
		intent.putExtra(EXTRA_STATUS_ID, statusId);
		intent.putExtra(EXTRA_FEED_HAS_IMAGE, feedHasImage);
		if (feedHasImage) {
			intent.putExtra(EXTRA_IMAGE_DOWNLOAD_ID, imageDownloadId);
		}
		sendBroadcast(intent);
	}

	/**
	 * Creates a notification at the end of the service lifecycle to notify the
	 * user about the number of completed downloads. A report will only be
	 * created if the number of feeds is > 1 or if at least one media file was
	 * downloaded.
	 */
	private void createReport() {
		// check if report should be created
		boolean createReport = false;
		int feedCount = 0;
		for (DownloadStatus status : completedDownloads) {
			if (status.getFeedFile().getClass() == Feed.class) {
				feedCount++;
				if (feedCount > 1) {
					createReport = true;
					break;
				}
			} else if (status.getFeedFile().getClass() == FeedMedia.class) {
				createReport = true;
				break;
			}
		}
		if (createReport) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Creating report");
			int successfulDownloads = 0;
			int failedDownloads = 0;
			for (DownloadStatus status : completedDownloads) {
				if (status.isSuccessful()) {
					successfulDownloads++;
				} else {
					failedDownloads++;
				}
			}
			// create notification object
			Notification notification = new NotificationCompat.Builder(this)
					.setTicker(
							getString(de.danoeh.antennapod.R.string.download_report_title))
					.setContentTitle(
							getString(de.danoeh.antennapod.R.string.download_report_title))
					.setContentText(
							successfulDownloads + " Downloads succeeded, "
									+ failedDownloads + " failed")
					.setSmallIcon(android.R.drawable.stat_notify_sync)
					.setLargeIcon(
							BitmapFactory.decodeResource(null,
									android.R.drawable.stat_notify_sync))
					.setContentIntent(
							PendingIntent.getActivity(this, 0, new Intent(this,
									MainActivity.class), 0))
					.setAutoCancel(true).getNotification();
			NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			nm.notify(REPORT_ID, notification);

		} else {
			if (AppConfig.DEBUG)
				Log.d(TAG, "No report is created");
		}
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
		if (AppConfig.DEBUG)
			Log.d(TAG, "Handling completed Feed Download");
		syncExecutor.execute(new FeedSyncThread(feed, this));

	}

	/** Is called whenever a Feed-Image is downloaded */
	private void handleCompletedImageDownload(Context context, FeedImage image) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Handling completed Image Download");
		syncExecutor.execute(new ImageHandlerThread(image, this));
	}

	/** Is called whenever a FeedMedia is downloaded. */
	private void handleCompletedFeedMediaDownload(Context context,
			FeedMedia media) {
		if (AppConfig.DEBUG)
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

		private int reason;
		private boolean successful;

		public FeedSyncThread(Feed feed, DownloadService service) {
			this.feed = feed;
			this.service = service;
		}

		public void run() {
			Feed savedFeed = null;
			long imageId = 0;
			boolean hasImage = false;
			long downloadId = feed.getDownloadId();
			reason = 0;
			successful = true;
			FeedManager manager = FeedManager.getInstance();
			FeedHandler handler = new FeedHandler();
			feed.setDownloaded(true);

			try {
				feed = handler.parseFeed(feed);
				if (AppConfig.DEBUG)
					Log.d(TAG, feed.getTitle() + " parsed");
				if (checkFeedData(feed) == false) {
					throw new InvalidFeedException();
				}
				feed.setDownloadId(0);
				// Save information of feed in DB
				savedFeed = manager.updateFeed(service, feed);
				// Download Feed Image if provided and not downloaded
				if (savedFeed.getImage() != null
						&& savedFeed.getImage().isDownloaded() == false) {
					if (AppConfig.DEBUG)
						Log.d(TAG, "Feed has image; Downloading....");
					savedFeed.getImage().setFeed(savedFeed);
					imageId = requester.downloadImage(service, savedFeed.getImage());
					hasImage = true;
				}

			} catch (SAXException e) {
				successful = false;
				e.printStackTrace();
				reason = DownloadError.ERROR_PARSER_EXCEPTION;
			} catch (IOException e) {
				successful = false;
				e.printStackTrace();
				reason = DownloadError.ERROR_PARSER_EXCEPTION;
			} catch (ParserConfigurationException e) {
				successful = false;
				e.printStackTrace();
				reason = DownloadError.ERROR_PARSER_EXCEPTION;
			} catch (UnsupportedFeedtypeException e) {
				e.printStackTrace();
				successful = false;
				reason = DownloadError.ERROR_UNSUPPORTED_TYPE;
			} catch (InvalidFeedException e) {
				e.printStackTrace();
				successful = false;
				reason = DownloadError.ERROR_PARSER_EXCEPTION;
			}

			requester.removeDownload(feed);
			//cleanup();
			if (savedFeed == null) {
				savedFeed = feed;
			}
			long statusId = saveDownloadStatus(new DownloadStatus(savedFeed,
					reason, successful));
			sendDownloadHandledIntent(downloadId, statusId, hasImage, imageId);
			queryDownloads();
		}

		/** Checks if the feed was parsed correctly. */
		private boolean checkFeedData(Feed feed) {
			if (feed.getTitle() == null) {
				Log.e(TAG, "Feed has no title.");
				return false;
			} else {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Feed appears to be valid.");
				return true;
			}
		}

		/** Delete files that aren't needed anymore */
		private void cleanup() {
			if (feed.getFile_url() != null) {
				if (new File(feed.getFile_url()).delete())
					if (AppConfig.DEBUG)
						Log.d(TAG, "Successfully deleted cache file.");
					else
						Log.e(TAG, "Failed to delete cache file.");
				feed.setFile_url(null);
			} else if (AppConfig.DEBUG) {
				Log.d(TAG, "Didn't delete cache file: File url is not set.");
			}
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

			long statusId = saveDownloadStatus(new DownloadStatus(image, 0,
					true));
			sendDownloadHandledIntent(image.getDownloadId(), statusId, false, 0);
			image.setDownloadId(0);
			manager.setFeedImage(service, image);
			if (image.getFeed() != null) {
				manager.setFeed(service, image.getFeed());
			} else {
				Log.e(TAG, "Image has no feed, image might not be saved correctly!");
			}
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
			if (AppConfig.DEBUG)
				Log.d(TAG, "Duration of file is " + media.getDuration());
			mediaplayer.reset();
			long statusId = saveDownloadStatus(new DownloadStatus(media, 0,
					true));
			sendDownloadHandledIntent(media.getDownloadId(), statusId, false, 0);
			media.setDownloadId(0);
			manager.setFeedMedia(service, media);
			boolean autoQueue = PreferenceManager.getDefaultSharedPreferences(
					getApplicationContext()).getBoolean(
					PodcastApp.PREF_AUTO_QUEUE, true);
			
			if (!manager.isInQueue(media.getItem())) {
				// Auto-queue
				if (autoQueue) {
					if (AppConfig.DEBUG)
						Log.d(TAG, "Autoqueue is enabled. Adding item to queue");
					manager.addQueueItem(DownloadService.this, media.getItem());
				} else {
					if (AppConfig.DEBUG)
						Log.d(TAG, "Autoqueue is disabled");
				}
			} else {
				if (AppConfig.DEBUG) Log.d(TAG, "Item is already in queue");
			}
			
			queryDownloads();
		}
	}

	public DownloadObserver getDownloadObserver() {
		return downloadObserver;
	}

}
