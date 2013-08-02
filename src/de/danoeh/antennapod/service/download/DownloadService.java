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
import java.util.concurrent.*;

import javax.xml.parsers.ParserConfigurationException;

import de.danoeh.antennapod.storage.DBTasks;
import de.danoeh.antennapod.storage.DBWriter;
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
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.webkit.URLUtil;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.DownloadActivity;
import de.danoeh.antennapod.activity.DownloadLogActivity;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.syndication.handler.FeedHandler;
import de.danoeh.antennapod.syndication.handler.UnsupportedFeedtypeException;
import de.danoeh.antennapod.util.ChapterUtils;
import de.danoeh.antennapod.util.DownloadError;
import de.danoeh.antennapod.util.InvalidFeedException;

public class DownloadService extends Service {
	private static final String TAG = "DownloadService";

	public static String ACTION_ALL_FEED_DOWNLOADS_COMPLETED = "action.de.danoeh.antennapod.storage.all_feed_downloads_completed";

	public static final String ACTION_ENQUEUE_DOWNLOAD = "action.de.danoeh.antennapod.service.enqueueDownload";
	public static final String ACTION_CANCEL_DOWNLOAD = "action.de.danoeh.antennapod.service.cancelDownload";
	public static final String ACTION_CANCEL_ALL_DOWNLOADS = "action.de.danoeh.antennapod.service.cancelAllDownloads";

	/** Extra for ACTION_CANCEL_DOWNLOAD */
	public static final String EXTRA_DOWNLOAD_URL = "downloadUrl";

	/**
	 * Sent by the DownloadService when the content of the downloads list
	 * changes.
	 */
	public static final String ACTION_DOWNLOADS_CONTENT_CHANGED = "action.de.danoeh.antennapod.service.downloadsContentChanged";

	public static final String EXTRA_DOWNLOAD_ID = "extra.de.danoeh.antennapod.service.download_id";

	/** Extra for ACTION_ENQUEUE_DOWNLOAD intent. */
	public static final String EXTRA_REQUEST = "request";

	private CopyOnWriteArrayList<DownloadStatus> completedDownloads;

	private ExecutorService syncExecutor;
	private ExecutorService downloadExecutor;
	/** Number of threads of downloadExecutor. */
	private static final int NUM_PARALLEL_DOWNLOADS = 4;

	private DownloadRequester requester;
	private NotificationCompat.Builder notificationCompatBuilder;
	private Notification.BigTextStyle notificationBuilder;
	private int NOTIFICATION_ID = 2;
	private int REPORT_ID = 3;

	private List<Downloader> downloads;

	/** Number of completed downloads which are currently being handled. */
	private volatile int downloadsBeingHandled;

	private volatile boolean shutdownInitiated = false;
	/** True if service is running. */
	public static boolean isRunning = false;

	private Handler handler;

	private NotificationUpdater notificationUpdater;
	private ScheduledFuture notificationUpdaterFuture;
	private static final int SCHED_EX_POOL_SIZE = 1;
	private ScheduledThreadPoolExecutor schedExecutor;

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
		handler = new Handler();
		completedDownloads = new CopyOnWriteArrayList<DownloadStatus>(
				new ArrayList<DownloadStatus>());
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
                        downloadsBeingHandled -= 1;
                        queryDownloads();
                    }
                });
                return t;
            }
        });
		downloadExecutor = Executors.newFixedThreadPool(NUM_PARALLEL_DOWNLOADS,
				new ThreadFactory() {

					@Override
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r);
						t.setPriority(Thread.MIN_PRIORITY);
						return t;
					}
				});
		schedExecutor = new ScheduledThreadPoolExecutor(SCHED_EX_POOL_SIZE,
				new ThreadFactory() {

					@Override
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r);
						t.setPriority(Thread.MIN_PRIORITY);
						return t;
					}
				}, new RejectedExecutionHandler() {

					@Override
					public void rejectedExecution(Runnable r,
							ThreadPoolExecutor executor) {
						Log.w(TAG, "SchedEx rejected submission of new task");
					}
				});
		setupNotificationBuilders();
		requester = DownloadRequester.getInstance();
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
		unregisterReceiver(cancelDownloadReceiver);
		unregisterReceiver(downloadQueued);
	}

	@SuppressLint("NewApi")
	private void setupNotificationBuilders() {
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, new Intent(
				this, DownloadActivity.class),
				PendingIntent.FLAG_UPDATE_CURRENT);

		Bitmap icon = BitmapFactory.decodeResource(getResources(),
				R.drawable.stat_notify_sync);

		if (android.os.Build.VERSION.SDK_INT >= 16) {
			notificationBuilder = new Notification.BigTextStyle(
					new Notification.Builder(this).setOngoing(true)
							.setContentIntent(pIntent).setLargeIcon(icon)
							.setSmallIcon(R.drawable.stat_notify_sync));
		} else {
			notificationCompatBuilder = new NotificationCompat.Builder(this)
					.setOngoing(true).setContentIntent(pIntent)
					.setLargeIcon(icon)
					.setSmallIcon(R.drawable.stat_notify_sync);
		}
		if (AppConfig.DEBUG)
			Log.d(TAG, "Notification set up");
	}

	/**
	 * Updates the contents of the service's notifications. Should be called
	 * before setupNotificationBuilders.
	 */
	@SuppressLint("NewApi")
	private Notification updateNotifications() {
		String contentTitle = getString(R.string.download_notification_title);
		String downloadsLeft = requester.getNumberOfDownloads()
				+ getString(R.string.downloads_left);
		if (android.os.Build.VERSION.SDK_INT >= 16) {

			if (notificationBuilder != null) {

				StringBuilder bigText = new StringBuilder("");
				for (int i = 0; i < downloads.size(); i++) {
					Downloader downloader = downloads.get(i);
					final DownloadRequest request = downloader
							.getDownloadRequest();
					if (request.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
						if (request.getTitle() != null) {
							if (i > 0) {
								bigText.append("\n");
							}
							bigText.append("\u2022 " + request.getTitle());
						}
					} else if (request.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
						if (request.getTitle() != null) {
							if (i > 0) {
								bigText.append("\n");
							}
							bigText.append("\u2022 " + request.getTitle()
									+ " (" + request.getProgressPercent()
									+ "%)");
						}
					}

				}
				notificationBuilder.setSummaryText(downloadsLeft);
				notificationBuilder.setBigContentTitle(contentTitle);
				if (bigText != null) {
					notificationBuilder.bigText(bigText.toString());
				}
				return notificationBuilder.build();
			}
		} else {
			if (notificationCompatBuilder != null) {
				notificationCompatBuilder.setContentTitle(contentTitle);
				notificationCompatBuilder.setContentText(downloadsLeft);
				return notificationCompatBuilder.getNotification();
			}
		}
		return null;
	}

	private Downloader getDownloader(String downloadUrl) {
		for (Downloader downloader : downloads) {
			if (downloader.getDownloadRequest().getSource().equals(downloadUrl)) {
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
					d.cancel();
					removeDownload(d);
				} else {
					Log.e(TAG, "Could not cancel download with url " + url);
				}

			} else if (intent.getAction().equals(ACTION_CANCEL_ALL_DOWNLOADS)) {
				for (Downloader d : downloads) {
					d.cancel();
					DownloadRequester.getInstance().removeDownload(
							d.getDownloadRequest());
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
		DownloadRequest request = intent.getParcelableExtra(EXTRA_REQUEST);
		if (request == null) {
			throw new IllegalArgumentException(
					"ACTION_ENQUEUE_DOWNLOAD intent needs request extra");
		}
		if (shutdownInitiated) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Cancelling shutdown; new download was queued");
			shutdownInitiated = false;
		}

		Downloader downloader = getDownloader(request);
		if (downloader != null) {
			downloads.add(downloader);
			downloadExecutor.submit(downloader);
			sendBroadcast(new Intent(ACTION_DOWNLOADS_CONTENT_CHANGED));
		}

		queryDownloads();
	}

	private BroadcastReceiver downloadQueued = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			onDownloadQueued(intent);
		}

	};

	private Downloader getDownloader(DownloadRequest request) {
		if (URLUtil.isHttpUrl(request.getSource())) {
			return new HttpDownloader(new DownloaderCallback() {

				@Override
				public void onDownloadCompleted(final Downloader downloader) {
					handler.post(new Runnable() {

						@Override
						public void run() {
							DownloadService.this
									.onDownloadCompleted(downloader);
						}
					});
				}
			}, request);
		}
		Log.e(TAG,
				"Could not find appropriate downloader for "
						+ request.getSource());
		return null;
	}

	@SuppressLint("NewApi")
	public void onDownloadCompleted(final Downloader downloader) {
		final AsyncTask<Void, Void, Void> handlerTask = new AsyncTask<Void, Void, Void>() {
			boolean successful;

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				if (!successful) {
					queryDownloads();
				}
			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				removeDownload(downloader);
			}

			@Override
			protected Void doInBackground(Void... params) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Received 'Download Complete' - message.");
				downloadsBeingHandled += 1;
				DownloadStatus status = downloader.getResult();
				successful = status.isSuccessful();

				final int type = status.getFeedfileType();
				if (successful) {
					if (type == Feed.FEEDFILETYPE_FEED) {
						handleCompletedFeedDownload(downloader
								.getDownloadRequest());
					} else if (type == FeedImage.FEEDFILETYPE_FEEDIMAGE) {
						handleCompletedImageDownload(status, downloader.getDownloadRequest());
					} else if (type == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
						handleCompletedFeedMediaDownload(status, downloader.getDownloadRequest());
					}
				} else {
					if (!successful && !status.isCancelled()) {
						Log.e(TAG, "Download failed");
						saveDownloadStatus(status);
					}
					sendDownloadHandledIntent();
					downloadsBeingHandled -= 1;
				}

				return null;
			}
		};
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			handlerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			handlerTask.execute();
		}
	}

	/**
	 * Remove download from the DownloadRequester list and from the
	 * DownloadService list.
	 */
	private void removeDownload(final Downloader d) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Removing downloader: "
					+ d.getDownloadRequest().getSource());
		boolean rc = downloads.remove(d);
		if (AppConfig.DEBUG)
			Log.d(TAG, "Result of downloads.remove: " + rc);
		DownloadRequester.getInstance().removeDownload(d.getDownloadRequest());
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
		DBWriter.addDownloadStatus(this, status);
	}

	private void sendDownloadHandledIntent() {
		EventDistributor.getInstance().sendDownloadHandledBroadcast();
	}

	/**
	 * Creates a notification at the end of the service lifecycle to notify the
	 * user about the number of completed downloads. A report will only be
	 * created if the number of successfully downloaded feeds is bigger than 1
	 * or if there is at least one failed download which is not an image or if
	 * there is at least one downloaded media file.
	 */
	private void updateReport() {
		// check if report should be created
		boolean createReport = false;
		int successfulDownloads = 0;
		int failedDownloads = 0;

		// a download report is created if at least one download has failed
		// (excluding failed image downloads)
		for (DownloadStatus status : completedDownloads) {
			if (status.isSuccessful()) {
				successfulDownloads++;
			} else if (!status.isCancelled()) {
				if (status.getFeedfileType() != FeedImage.FEEDFILETYPE_FEEDIMAGE) {
					createReport = true;
				}
				failedDownloads++;
			}
		}

		if (createReport) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Creating report");
			// create notification object
			Notification notification = new NotificationCompat.Builder(this)
					.setTicker(
							getString(de.danoeh.antennapod.R.string.download_report_title))
					.setContentTitle(
							getString(de.danoeh.antennapod.R.string.download_report_title))
					.setContentText(
							String.format(
									getString(R.string.download_report_content),
									successfulDownloads, failedDownloads))
					.setSmallIcon(R.drawable.stat_notify_sync)
					.setLargeIcon(
							BitmapFactory.decodeResource(getResources(),
									R.drawable.stat_notify_sync))
					.setContentIntent(
							PendingIntent.getActivity(this, 0, new Intent(this,
									DownloadLogActivity.class), 0))
					.setAutoCancel(true).getNotification();
			NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			nm.notify(REPORT_ID, notification);
		} else {
			if (AppConfig.DEBUG)
				Log.d(TAG, "No report is created");
		}
		completedDownloads.clear();
	}

	/** Check if there's something else to download, otherwise stop */
	void queryDownloads() {
		int numOfDownloads = downloads.size();
		if (AppConfig.DEBUG) {
			Log.d(TAG, numOfDownloads + " downloads left");
			Log.d(TAG, "Downloads being handled: " + downloadsBeingHandled);
			Log.d(TAG, "ShutdownInitiated: " + shutdownInitiated);
		}

		if (numOfDownloads == 0 && downloadsBeingHandled <= 0) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Starting shutdown");
			shutdownInitiated = true;
			updateReport();
			cancelNotificationUpdater();
			stopForeground(true);
		} else {
			setupNotificationUpdater();
			startForeground(NOTIFICATION_ID, updateNotifications());
		}
	}

	/** Is called whenever a Feed is downloaded */
	private void handleCompletedFeedDownload(DownloadRequest request) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Handling completed Feed Download");
		syncExecutor.execute(new FeedSyncThread(request));

	}

	/** Is called whenever a Feed-Image is downloaded */
	private void handleCompletedImageDownload(DownloadStatus status, DownloadRequest request) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Handling completed Image Download");
		syncExecutor.execute(new ImageHandlerThread(status, request));
	}

	/** Is called whenever a FeedMedia is downloaded. */
	private void handleCompletedFeedMediaDownload(DownloadStatus status, DownloadRequest request) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Handling completed FeedMedia Download");
		syncExecutor.execute(new MediaHandlerThread(status, request));
	}

	/**
	 * Takes a single Feed, parses the corresponding file and refreshes
	 * information in the manager
	 */
	class FeedSyncThread implements Runnable {
		private static final String TAG = "FeedSyncThread";

		private DownloadRequest request;

		private int reason;
		private boolean successful;

		public FeedSyncThread(DownloadRequest request) {
			if (request == null) {
				throw new IllegalArgumentException("Request must not be null");
			}

			this.request = request;
		}

		public void run() {
			Feed savedFeed = null;

			Feed feed = new Feed(request.getSource(), new Date());
			feed.setFile_url(request.getDestination());
			feed.setDownloaded(true);

			reason = 0;
			String reasonDetailed = null;
			successful = true;
			FeedHandler feedHandler = new FeedHandler();

			try {
				feed = feedHandler.parseFeed(feed);
				if (AppConfig.DEBUG)
					Log.d(TAG, feed.getTitle() + " parsed");
				if (checkFeedData(feed) == false) {
					throw new InvalidFeedException();
				}
				// Save information of feed in DB
				savedFeed = DBTasks.updateFeed(DownloadService.this, feed);
				// Download Feed Image if provided and not downloaded
				if (savedFeed.getImage() != null
						&& savedFeed.getImage().isDownloaded() == false) {
					if (AppConfig.DEBUG)
						Log.d(TAG, "Feed has image; Downloading....");
					savedFeed.getImage().setFeed(savedFeed);
					final Feed savedFeedRef = savedFeed;
					handler.post(new Runnable() {

						@Override
						public void run() {
							try {
								requester.downloadImage(DownloadService.this,
										savedFeedRef.getImage());
							} catch (DownloadRequestException e) {
								e.printStackTrace();
								DBWriter.addDownloadStatus(
                                        DownloadService.this,
                                        new DownloadStatus(
                                                savedFeedRef.getImage(),
                                                savedFeedRef
                                                        .getImage()
                                                        .getHumanReadableIdentifier(),
                                                DownloadError.ERROR_REQUEST_ERROR,
                                                false, e.getMessage()));
							}
						}
					});

				}

			} catch (SAXException e) {
				successful = false;
				e.printStackTrace();
				reason = DownloadError.ERROR_PARSER_EXCEPTION;
				reasonDetailed = e.getMessage();
			} catch (IOException e) {
				successful = false;
				e.printStackTrace();
				reason = DownloadError.ERROR_PARSER_EXCEPTION;
				reasonDetailed = e.getMessage();
			} catch (ParserConfigurationException e) {
				successful = false;
				e.printStackTrace();
				reason = DownloadError.ERROR_PARSER_EXCEPTION;
				reasonDetailed = e.getMessage();
			} catch (UnsupportedFeedtypeException e) {
				e.printStackTrace();
				successful = false;
				reason = DownloadError.ERROR_UNSUPPORTED_TYPE;
				reasonDetailed = e.getMessage();
			} catch (InvalidFeedException e) {
				e.printStackTrace();
				successful = false;
				reason = DownloadError.ERROR_PARSER_EXCEPTION;
				reasonDetailed = e.getMessage();
			}

			// cleanup();
			if (savedFeed == null) {
				savedFeed = feed;
			}

			saveDownloadStatus(new DownloadStatus(savedFeed,
					savedFeed.getHumanReadableIdentifier(), reason, successful,
					reasonDetailed));
			sendDownloadHandledIntent();
			downloadsBeingHandled -= 1;
			handler.post(new Runnable() {

				@Override
				public void run() {
					queryDownloads();

				}
			});
		}

		/** Checks if the feed was parsed correctly. */
		private boolean checkFeedData(Feed feed) {
			if (feed.getTitle() == null) {
				Log.e(TAG, "Feed has no title.");
				return false;
			}
			if (!hasValidFeedItems(feed)) {
				Log.e(TAG, "Feed has invalid items");
				return false;
			}
			if (AppConfig.DEBUG)
				Log.d(TAG, "Feed appears to be valid.");
			return true;

		}

		private boolean hasValidFeedItems(Feed feed) {
			for (FeedItem item : feed.getItemsArray()) {
				if (item.getTitle() == null) {
					Log.e(TAG, "Item has no title");
					return false;
				}
				if (item.getPubDate() == null) {
					Log.e(TAG,
							"Item has no pubDate. Using current time as pubDate");
					if (item.getTitle() != null) {
						Log.e(TAG, "Title of invalid item: " + item.getTitle());
					}
					item.setPubDate(new Date());
				}
			}
			return true;
		}

		/** Delete files that aren't needed anymore */
		private void cleanup(Feed feed) {
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
		
		private DownloadRequest request;
		private DownloadStatus status;

		public ImageHandlerThread(DownloadStatus status, DownloadRequest request) {
			if (status == null) {
				throw new IllegalArgumentException("Status must not be null");
			}
			if (request == null) {
				throw new IllegalArgumentException("Request must not be null");
			}
			this.status = status;
			this.request = request;
		}

		@Override
		public void run() {
			FeedImage image = FeedManager.getInstance().getFeedImage(request.getFeedfileId());
			if (image == null) {
				throw new IllegalStateException("Could not find downloaded image in database");
			}
			
			image.setFile_url(request.getDestination());
			image.setDownloaded(true);

			saveDownloadStatus(status);
			sendDownloadHandledIntent();
			DBWriter.setFeedImage(DownloadService.this, image);
			downloadsBeingHandled -= 1;
			handler.post(new Runnable() {

				@Override
				public void run() {
					queryDownloads();

				}
			});
		}
	}

	/** Handles a completed media download. */
	class MediaHandlerThread implements Runnable {

		private DownloadRequest request;
		private DownloadStatus status;
		
		public MediaHandlerThread(DownloadStatus status, DownloadRequest request) {
			if (status == null) {
				throw new IllegalArgumentException("Status must not be null");
			}
			if (request == null) {
				throw new IllegalArgumentException("Request must not be null");
			}
			
			this.status = status;
			this.request = request;
		}

		@Override
		public void run() {
			FeedMedia media = FeedManager.getInstance().getFeedMedia(
					request.getFeedfileId());
			if (media == null) {
				throw new IllegalStateException(
						"Could not find downloaded media object in database");
			}
			boolean chaptersRead = false;
			media.setDownloaded(true);
			media.setFile_url(request.getDestination());
			
			// Get duration
			MediaPlayer mediaplayer = new MediaPlayer();
			try {
				mediaplayer.setDataSource(media.getFile_url());
				mediaplayer.prepare();
				media.setDuration(mediaplayer.getDuration());
				if (AppConfig.DEBUG)
					Log.d(TAG, "Duration of file is " + media.getDuration());
				mediaplayer.reset();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				mediaplayer.release();
			}

			if (media.getItem().getChapters() == null) {
				ChapterUtils.loadChaptersFromFileUrl(media);
				if (media.getItem().getChapters() != null) {
					chaptersRead = true;
				}
			}

			saveDownloadStatus(status);
			sendDownloadHandledIntent();

            try {
                if (chaptersRead) {
                    DBWriter.setFeedItem(DownloadService.this, media.getItem()).get();
                }
                DBWriter.setFeedMedia(DownloadService.this, media).get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!FeedManager.getInstance().isInQueue(media.getItem())) {
				FeedManager.getInstance().addQueueItem(DownloadService.this,
						media.getItem());
			}

			downloadsBeingHandled -= 1;
			handler.post(new Runnable() {

				@Override
				public void run() {
					queryDownloads();

				}
			});
		}
	}

	/** Schedules the notification updater task if it hasn't been scheduled yet. */
	private void setupNotificationUpdater() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Setting up notification updater");
		if (notificationUpdater == null) {
			notificationUpdater = new NotificationUpdater();
			notificationUpdaterFuture = schedExecutor.scheduleAtFixedRate(
					notificationUpdater, 5L, 5L, TimeUnit.SECONDS);
		}
	}

	private void cancelNotificationUpdater() {
		boolean result = false;
		if (notificationUpdaterFuture != null) {
			result = notificationUpdaterFuture.cancel(true);
		}
		notificationUpdater = null;
		notificationUpdaterFuture = null;
		Log.d(TAG, "NotificationUpdater cancelled. Result: " + result);
	}

	private class NotificationUpdater implements Runnable {
		public void run() {
			handler.post(new Runnable() {
				@Override
				public void run() {
					Notification n = updateNotifications();
					if (n != null) {
						NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
						nm.notify(NOTIFICATION_ID, n);
					}
				}
			});
		}
	}

	public List<Downloader> getDownloads() {
		return downloads;
	}

}
