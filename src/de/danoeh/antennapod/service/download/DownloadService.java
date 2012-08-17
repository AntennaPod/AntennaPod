/** 
 * Registers a DownloadReceiver and waits for all Downloads 
 * to complete, then stops
 * */

package de.danoeh.antennapod.service.download;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.webkit.URLUtil;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.activity.DownloadActivity;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.asynctask.DownloadStatus;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedFile;
import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.syndication.handler.FeedHandler;
import de.danoeh.antennapod.syndication.handler.UnsupportedFeedtypeException;
import de.danoeh.antennapod.util.DownloadError;
import de.danoeh.antennapod.util.InvalidFeedException;

public class DownloadService extends Service {
	private static final String TAG = "DownloadService";

	public static String ACTION_ALL_FEED_DOWNLOADS_COMPLETED = "action.de.danoeh.antennapod.storage.all_feed_downloads_completed";

	/**
	 * If the DownloadService receives this intent, it will execute
	 * queryDownloads()
	 */
	public static final String ACTION_ENQUEUE_DOWNLOAD = "action.de.danoeh.antennapod.service.enqueueDownload";
	public static final String ACTION_CANCEL_DOWNLOAD = "action.de.danoeh.antennapod.service.cancelDownload";
	public static final String ACTION_CANCEL_ALL_DOWNLOADS = "action.de.danoeh.antennapod.service.cancelAllDownloads";

	/** Extra for ACTION_CANCEL_DOWNLOAD */
	public static final String EXTRA_DOWNLOAD_URL = "downloadUrl";

	public static final String ACTION_DOWNLOAD_HANDLED = "action.de.danoeh.antennapod.service.download_handled";
	/**
	 * Sent by the DownloadService when the content of the downloads list
	 * changes.
	 */
	public static final String ACTION_DOWNLOADS_CONTENT_CHANGED = "action.de.danoeh.antennapod.service.downloadsContentChanged";

	public static final String EXTRA_DOWNLOAD_ID = "extra.de.danoeh.antennapod.service.download_id";

	/** Extra for ACTION_ENQUEUE_DOWNLOAD intent. */
	public static final String EXTRA_REQUEST = "request";

	// Download types for ACTION_DOWNLOAD_HANDLED
	public static final String EXTRA_DOWNLOAD_TYPE = "extra.de.danoeh.antennapod.service.downloadType";
	public static final int DOWNLOAD_TYPE_FEED = 1;
	public static final int DOWNLOAD_TYPE_MEDIA = 2;
	public static final int DOWNLOAD_TYPE_IMAGE = 3;

	private ArrayList<DownloadStatus> completedDownloads;

	private ExecutorService syncExecutor;
	private ExecutorService downloadExecutor;

	private DownloadRequester requester;
	private FeedManager manager;
	private NotificationCompat.Builder notificationBuilder;
	private int NOTIFICATION_ID = 2;
	private int REPORT_ID = 3;
	/** Needed to determine the duration of a media file */
	private MediaPlayer mediaplayer;

	private List<Downloader> downloads;

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
		if (intent.getParcelableExtra(EXTRA_REQUEST) != null) {
			onDownloadQueued(intent);
		}
		return Service.START_NOT_STICKY;
	}

	@SuppressLint("NewApi")
	@Override
	public void onCreate() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Service started");
		isRunning = true;
		completedDownloads = new ArrayList<DownloadStatus>();
		downloads = new ArrayList<Downloader>();
		registerReceiver(downloadQueued, new IntentFilter(
				ACTION_ENQUEUE_DOWNLOAD));

		IntentFilter cancelDownloadReceiverFilter = new IntentFilter();
		cancelDownloadReceiverFilter.addAction(ACTION_CANCEL_ALL_DOWNLOADS);
		cancelDownloadReceiverFilter.addAction(ACTION_CANCEL_DOWNLOAD);
		registerReceiver(cancelDownloadReceiver, cancelDownloadReceiverFilter);
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
					}
				});
				return t;
			}
		});
		downloadExecutor = Executors.newFixedThreadPool(2, new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setPriority(Thread.MIN_PRIORITY);
				return t;
			}
		});
		manager = FeedManager.getInstance();
		requester = DownloadRequester.getInstance();
		mediaplayer = new MediaPlayer();
		setupNotification();

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
		unregisterReceiver(cancelDownloadReceiver);
		unregisterReceiver(downloadQueued);
		createReport();
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

	private Downloader getDownloader(String downloadUrl) {
		for (Downloader downloader : downloads) {
			if (downloader.getStatus().getFeedFile().getDownload_url()
					.equals(downloadUrl)) {
				return downloader;
			}
		}
		return null;
	}

	private BroadcastReceiver cancelDownloadReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ACTION_CANCEL_DOWNLOAD)) {
				String url = intent.getStringExtra(EXTRA_DOWNLOAD_URL);
				if (url == null) {
					throw new IllegalArgumentException(
							"ACTION_CANCEL_DOWNLOAD intent needs download url extra");
				}
				if (AppConfig.DEBUG)
					Log.d(TAG, "Cancelling download with url " + url);
				Downloader d = getDownloader(url);
				if (d != null) {
					d.interrupt();
					removeDownload(d);
				} else {
					Log.e(TAG, "Could not cancel download with url " + url);
				}

			} else if (intent.getAction().equals(ACTION_CANCEL_ALL_DOWNLOADS)) {
				for (Downloader d : downloads) {
					d.interrupt();
					DownloadRequester.getInstance().removeDownload(
							d.getStatus().getFeedFile());
					d.getStatus().getFeedFile().setFile_url(null);
					if (AppConfig.DEBUG)
						Log.d(TAG, "Cancelled all downloads");
				}
				downloads.clear();
				sendBroadcast(new Intent(ACTION_DOWNLOADS_CONTENT_CHANGED));
			}
			queryDownloads();
		}

	};

	private void onDownloadQueued(Intent intent) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Received enqueue request");
		Request request = intent.getParcelableExtra(EXTRA_REQUEST);
		if (request == null) {
			throw new IllegalArgumentException(
					"ACTION_ENQUEUE_DOWNLOAD intent needs request extra");
		}
		if (shutdownInitiated) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Cancelling shutdown; new download was queued");
			shutdownInitiated = false;
			setupNotification();
		}

		DownloadRequester requester = DownloadRequester.getInstance();
		FeedFile feedfile = requester.getDownload(request.source);
		if (feedfile != null) {

			DownloadStatus status = new DownloadStatus(feedfile);
			Downloader downloader = getDownloader(status);
			if (downloader != null) {
				downloads.add(downloader);
				downloadExecutor.submit(downloader);
				sendBroadcast(new Intent(ACTION_DOWNLOADS_CONTENT_CHANGED));
			}
		} else {
			Log.e(TAG,
					"Could not find feedfile in download requester when trying to enqueue new download");
			queryDownloads();

		}
	}

	private BroadcastReceiver downloadQueued = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			onDownloadQueued(intent);
		}

	};

	private Downloader getDownloader(DownloadStatus status) {
		if (URLUtil.isHttpUrl(status.getFeedFile().getDownload_url())) {
			return new HttpDownloader(this, status);
		}
		Log.e(TAG, "Could not find appropriate downloader for "
				+ status.getFeedFile().getDownload_url());
		return null;
	}

	@SuppressLint("NewApi")
	public void onDownloadCompleted(final Downloader downloader) {
		AsyncTask<Void, Void, Void> handler = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Received 'Download Complete' - message.");
				DownloadStatus status = downloader.getStatus();
				boolean successful = status.isSuccessful();
				int reason = status.getReason();

				FeedFile download = status.getFeedFile();
				if (download != null) {
					if (successful) {
						if (download.getClass() == Feed.class) {
							handleCompletedFeedDownload(status);
						} else if (download.getClass() == FeedImage.class) {
							handleCompletedImageDownload(status);
						} else if (download.getClass() == FeedMedia.class) {
							handleCompletedFeedMediaDownload(status);
						}
					} else {
						if (!successful
								&& reason != DownloadError.ERROR_DOWNLOAD_CANCELLED) {
							Log.e(TAG, "Download failed");
						}
						saveDownloadStatus(status);
						sendDownloadHandledIntent(getDownloadType(download));

					}
				}
				removeDownload(downloader);
				return null;
			}
		};
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			handler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			handler.execute();
		}
	}

	/**
	 * Remove download from the DownloadRequester list and from the
	 * DownloadService list.
	 */
	private void removeDownload(Downloader d) {
		downloads.remove(d);
		DownloadRequester.getInstance().removeDownload(
				d.getStatus().getFeedFile());
		sendBroadcast(new Intent(ACTION_DOWNLOADS_CONTENT_CHANGED));
	}

	/**
	 * Adds a new DownloadStatus object to the list of completed downloads and
	 * saves it in the database
	 * 
	 * @param status
	 *            the download that is going to be saved
	 */
	private void saveDownloadStatus(DownloadStatus status) {
		completedDownloads.add(status);
		manager.addDownloadStatus(this, status);
	}

	/** Returns correct value for EXTRA_DOWNLOAD_TYPE. */
	private int getDownloadType(FeedFile f) {
		if (f.getClass() == Feed.class) {
			return DOWNLOAD_TYPE_FEED;
		} else if (f.getClass() == FeedImage.class) {
			return DOWNLOAD_TYPE_IMAGE;
		} else if (f.getClass() == FeedMedia.class) {
			return DOWNLOAD_TYPE_MEDIA;
		} else {
			return 0;
		}
	}

	private void sendDownloadHandledIntent(int type) {
		Intent intent = new Intent(ACTION_DOWNLOAD_HANDLED);
		intent.putExtra(EXTRA_DOWNLOAD_TYPE, type);

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
	void queryDownloads() {
		int numOfDownloads = downloads.size();
		if (AppConfig.DEBUG)
			Log.d(TAG, numOfDownloads + " downloads left");
		if (AppConfig.DEBUG)
			Log.d(TAG, "ShutdownInitiated: " + shutdownInitiated);

		if (numOfDownloads == 0) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Starting shutdown");
			shutdownInitiated = true;
			stopForeground(true);
		} else {
			// update notification
			notificationBuilder.setContentText(numOfDownloads
					+ " Downloads left");
			NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			nm.notify(NOTIFICATION_ID, notificationBuilder.getNotification());
		}
	}

	/** Is called whenever a Feed is downloaded */
	private void handleCompletedFeedDownload(DownloadStatus status) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Handling completed Feed Download");
		syncExecutor.execute(new FeedSyncThread(status));

	}

	/** Is called whenever a Feed-Image is downloaded */
	private void handleCompletedImageDownload(DownloadStatus status) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Handling completed Image Download");
		syncExecutor.execute(new ImageHandlerThread(status));
	}

	/** Is called whenever a FeedMedia is downloaded. */
	private void handleCompletedFeedMediaDownload(DownloadStatus status) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Handling completed FeedMedia Download");
		syncExecutor.execute(new MediaHandlerThread(status));
	}

	/**
	 * Takes a single Feed, parses the corresponding file and refreshes
	 * information in the manager
	 */
	class FeedSyncThread implements Runnable {
		private static final String TAG = "FeedSyncThread";

		private Feed feed;
		private DownloadStatus status;

		private int reason;
		private boolean successful;

		public FeedSyncThread(DownloadStatus status) {
			this.feed = (Feed) status.getFeedFile();
			this.status = status;
		}

		public void run() {
			Feed savedFeed = null;
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
				// Save information of feed in DB
				savedFeed = manager.updateFeed(DownloadService.this, feed);
				// Download Feed Image if provided and not downloaded
				if (savedFeed.getImage() != null
						&& savedFeed.getImage().isDownloaded() == false) {
					if (AppConfig.DEBUG)
						Log.d(TAG, "Feed has image; Downloading....");
					savedFeed.getImage().setFeed(savedFeed);
					requester.downloadImage(DownloadService.this,
							savedFeed.getImage());
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

			// cleanup();
			if (savedFeed == null) {
				savedFeed = feed;
			}

			saveDownloadStatus(new DownloadStatus(savedFeed, reason, successful));
			sendDownloadHandledIntent(DOWNLOAD_TYPE_FEED);
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
		private DownloadStatus status;

		public ImageHandlerThread(DownloadStatus status) {
			this.image = (FeedImage) status.getFeedFile();
			this.status = status;
		}

		@Override
		public void run() {
			image.setDownloaded(true);

			status.setCompletionDate(new Date());
			saveDownloadStatus(status);
			sendDownloadHandledIntent(DOWNLOAD_TYPE_IMAGE);
			manager.setFeedImage(DownloadService.this, image);
			if (image.getFeed() != null) {
				manager.setFeed(DownloadService.this, image.getFeed());
			} else {
				Log.e(TAG,
						"Image has no feed, image might not be saved correctly!");
			}
			queryDownloads();
		}
	}

	/** Handles a completed media download. */
	class MediaHandlerThread implements Runnable {
		private FeedMedia media;
		private DownloadStatus status;
		private DownloadService service;

		public MediaHandlerThread(DownloadStatus status) {
			super();
			this.media = (FeedMedia) status.getFeedFile();
			this.status = status;
		}

		@Override
		public void run() {
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

			status.setCompletionDate(new Date());
			saveDownloadStatus(status);
			sendDownloadHandledIntent(DOWNLOAD_TYPE_MEDIA);
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
				if (AppConfig.DEBUG)
					Log.d(TAG, "Item is already in queue");
			}
			queryDownloads();
		}
	}

	/** Is used to request a new download. */
	public static class Request implements Parcelable {
		private String destination;
		private String source;

		public Request(String destination, String source) {
			super();
			this.destination = destination;
			this.source = source;
		}

		private Request(Parcel in) {
			destination = in.readString();
			source = in.readString();
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(destination);
			dest.writeString(source);
		}

		public static final Parcelable.Creator<Request> CREATOR = new Parcelable.Creator<Request>() {
			public Request createFromParcel(Parcel in) {
				return new Request(in);
			}

			public Request[] newArray(int size) {
				return new Request[size];
			}
		};

		public String getDestination() {
			return destination;
		}

		public String getSource() {
			return source;
		}

	}

	public List<Downloader> getDownloads() {
		return downloads;
	}

}
