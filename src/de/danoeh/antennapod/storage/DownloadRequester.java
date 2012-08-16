package de.danoeh.antennapod.storage;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.webkit.URLUtil;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedFile;
import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.service.download.DownloadService;
import de.danoeh.antennapod.util.NumberGenerator;
import de.danoeh.antennapod.util.URLChecker;

public class DownloadRequester {
	private static final String TAG = "DownloadRequester";

	public static String EXTRA_DOWNLOAD_ID = "extra.de.danoeh.antennapod.storage.download_id";
	public static String EXTRA_ITEM_ID = "extra.de.danoeh.antennapod.storage.item_id";

	public static String ACTION_DOWNLOAD_QUEUED = "action.de.danoeh.antennapod.storage.downloadQueued";

	public static String IMAGE_DOWNLOADPATH = "images/";
	public static String FEED_DOWNLOADPATH = "cache/";
	public static String MEDIA_DOWNLOADPATH = "media/";

	private static DownloadRequester downloader;

	Map<String, FeedFile> downloads;

	private DownloadRequester() {
		downloads = new ConcurrentHashMap<String, FeedFile>();
	}

	public static DownloadRequester getInstance() {
		if (downloader == null) {
			downloader = new DownloadRequester();
		}
		return downloader;
	}

	private void download(Context context, FeedFile item, File dest) {
		if (!isDownloadingFile(item)) {
			if (dest.exists()) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "File already exists. Deleting !");
				dest.delete();
			}
			if (AppConfig.DEBUG)
				Log.d(TAG,
						"Requesting download of url " + item.getDownload_url());
			downloads.put(item.getDownload_url(), item);
			item.setDownload_url(URLChecker.prepareURL(item.getDownload_url()));
			item.setFile_url(dest.toString());

			DownloadService.Request request = new DownloadService.Request(
					item.getFile_url(), item.getDownload_url());
			Intent queueIntent = new Intent(
					DownloadService.ACTION_ENQUEUE_DOWNLOAD);
			queueIntent.putExtra(DownloadService.EXTRA_REQUEST, request);
			if (!DownloadService.isRunning) {
				context.startService(new Intent(context, DownloadService.class));
			}
			context.sendBroadcast(queueIntent);
			context.sendBroadcast(new Intent(ACTION_DOWNLOAD_QUEUED));
		} else {
			Log.e(TAG, "URL " + item.getDownload_url()
					+ " is already being downloaded");
		}
	}

	public void downloadFeed(Context context, Feed feed) {
		download(context, feed, new File(getFeedfilePath(context),
				getFeedfileName(feed)));
	}

	public void downloadImage(Context context, FeedImage image) {
		download(context, image, new File(getImagefilePath(context),
				getImagefileName(image)));
	}

	public void downloadMedia(Context context, FeedMedia feedmedia) {
		download(context, feedmedia,
				new File(getMediafilePath(context, feedmedia),
						getMediafilename(feedmedia)));
	}

	/**
	 * Cancels a running download.
	 * */
	public void cancelDownload(final Context context, final FeedFile f) {
		cancelDownload(context, f.getDownload_url());
	}

	/**
	 * Cancels a running download.
	 * */
	public void cancelDownload(final Context context, final String download_url) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Cancelling download with url " + download_url);
		FeedFile download = downloads.remove(download_url);
		if (download != null) {
			download.setFile_url(null);
			notifyDownloadService(context);
		}
	}

	/** Cancels all running downloads */
	public void cancelAllDownloads(Context context) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Cancelling all running downloads");
		DownloadManager dm = (DownloadManager) context
				.getSystemService(Context.DOWNLOAD_SERVICE);
		for (FeedFile f : downloads.values()) {
			dm.remove(f.getDownloadId());
			f.setFile_url(null);
		}
		downloads.clear();
		notifyDownloadService(context);
	}

	/** Returns true if there is at least one Feed in the downloads queue. */
	public boolean isDownloadingFeeds() {
		for (FeedFile f : downloads.values()) {
			if (f.getClass() == Feed.class) {
				return true;
			}
		}
		return false;
	}

	/** Checks if feedfile is in the downloads list */
	public boolean isDownloadingFile(FeedFile item) {
		if (item.getDownload_url() != null) {
			return downloads.containsKey(item.getDownload_url());
		}
		return false;
	}

	public FeedFile getDownload(String downloadUrl) {
		return downloads.get(downloadUrl);
	}

	/** Checks if feedfile with the given download url is in the downloads list */
	public boolean isDownloadingFile(String downloadUrl) {
		return downloads.get(downloadUrl) != null;
	}

	public boolean hasNoDownloads() {
		return downloads.isEmpty();
	}

	public FeedFile getDownloadAt(int index) {
		return downloads.get(index);
	}

	/** Remove an object from the downloads-list of the requester. */
	public void removeDownload(FeedFile f) {
		downloads.remove(f);
	}

	/** Get the number of uncompleted Downloads */
	public int getNumberOfDownloads() {
		return downloads.size();
	}

	public String getFeedfilePath(Context context) {
		return context.getExternalFilesDir(FEED_DOWNLOADPATH).toString() + "/";
	}

	public String getFeedfileName(Feed feed) {
		return "feed-" + NumberGenerator.generateLong(feed.getDownload_url());
	}

	public String getImagefilePath(Context context) {
		return context.getExternalFilesDir(IMAGE_DOWNLOADPATH).toString() + "/";
	}

	public String getImagefileName(FeedImage image) {
		return "image-" + NumberGenerator.generateLong(image.getDownload_url());
	}

	public String getMediafilePath(Context context, FeedMedia media) {
		File externalStorage = context.getExternalFilesDir(MEDIA_DOWNLOADPATH
				+ NumberGenerator.generateLong(media.getItem().getFeed()
						.getTitle()) + "/");
		return externalStorage.toString();
	}

	public String getMediafilename(FeedMedia media) {
		return URLUtil.guessFileName(media.getDownload_url(), null,
				media.getMime_type());
	}

	/** Notifies the DownloadService to check if there are any Downloads left */
	public void notifyDownloadService(Context context) {
		context.sendBroadcast(new Intent(
				DownloadService.ACTION_NOTIFY_DOWNLOADS_CHANGED));
	}
}
