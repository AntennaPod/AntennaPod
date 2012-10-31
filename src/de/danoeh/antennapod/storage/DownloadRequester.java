package de.danoeh.antennapod.storage;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.webkit.URLUtil;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedFile;
import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.service.download.DownloadService;
import de.danoeh.antennapod.util.FileNameGenerator;
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
			item.setDownload_url(URLChecker.prepareURL(item.getDownload_url()));
			item.setFile_url(dest.toString());
			downloads.put(item.getDownload_url(), item);

			DownloadService.Request request = new DownloadService.Request(
					item.getFile_url(), item.getDownload_url());

			if (!DownloadService.isRunning) {
				Intent launchIntent = new Intent(context, DownloadService.class);
				launchIntent.putExtra(DownloadService.EXTRA_REQUEST, request);
				context.startService(launchIntent);
			} else {
				Intent queueIntent = new Intent(
						DownloadService.ACTION_ENQUEUE_DOWNLOAD);
				queueIntent.putExtra(DownloadService.EXTRA_REQUEST, request);
				context.sendBroadcast(queueIntent);
			}
			context.sendBroadcast(new Intent(ACTION_DOWNLOAD_QUEUED));
		} else {
			Log.e(TAG, "URL " + item.getDownload_url()
					+ " is already being downloaded");
		}
	}

	public void downloadFeed(Context context, Feed feed)
			throws DownloadRequestException {
		if (feedFileValid(feed)) {
			download(context, feed, new File(getFeedfilePath(context),
					getFeedfileName(feed)));
		}
	}

	public void downloadImage(Context context, FeedImage image)
			throws DownloadRequestException {
		if (feedFileValid(image)) {
			download(context, image, new File(getImagefilePath(context),
					getImagefileName(image)));
		}
	}

	public void downloadMedia(Context context, FeedMedia feedmedia)
			throws DownloadRequestException {
		if (feedFileValid(feedmedia)) {
			download(context, feedmedia,
					new File(getMediafilePath(context, feedmedia),
							getMediafilename(feedmedia)));
		}
	}

	/**
	 * Throws a DownloadRequestException if the feedfile or the download url of
	 * the feedfile is null.
	 * 
	 * @throws DownloadRequestException
	 */
	private boolean feedFileValid(FeedFile f) throws DownloadRequestException {
		if (f == null) {
			throw new DownloadRequestException("Feedfile was null");
		} else if (f.getDownload_url() == null) {
			throw new DownloadRequestException("File has no download URL");
		} else {
			return true;
		}
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
	public void cancelDownload(final Context context, final String downloadUrl) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Cancelling download with url " + downloadUrl);
		Intent cancelIntent = new Intent(DownloadService.ACTION_CANCEL_DOWNLOAD);
		cancelIntent.putExtra(DownloadService.EXTRA_DOWNLOAD_URL, downloadUrl);
		context.sendBroadcast(cancelIntent);
	}

	/** Cancels all running downloads */
	public void cancelAllDownloads(Context context) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Cancelling all running downloads");
		context.sendBroadcast(new Intent(
				DownloadService.ACTION_CANCEL_ALL_DOWNLOADS));
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
		if (downloads.remove(f.getDownload_url()) == null) {
			Log.e(TAG,
					"Could not remove object with url " + f.getDownload_url());
		}
	}

	/** Get the number of uncompleted Downloads */
	public int getNumberOfDownloads() {
		return downloads.size();
	}

	public String getFeedfilePath(Context context)
			throws DownloadRequestException {
		return getExternalFilesDirOrThrowException(context, FEED_DOWNLOADPATH)
				.toString() + "/";
	}

	public String getFeedfileName(Feed feed) {
		String filename = feed.getDownload_url();
		if (feed.getTitle() != null && !feed.getTitle().isEmpty()) {
			filename = feed.getTitle();
		}
		return "feed-"
				+ FileNameGenerator.generateFileName(filename);
	}

	public String getImagefilePath(Context context)
			throws DownloadRequestException {
		return getExternalFilesDirOrThrowException(context, IMAGE_DOWNLOADPATH)
				.toString() + "/";
	}

	public String getImagefileName(FeedImage image) {
		String filename = image.getDownload_url();
		if (image.getFeed() != null && image.getFeed().getTitle() != null) {
			filename = image.getFeed().getTitle();
		}
		return "image-"
				+ FileNameGenerator.generateFileName(filename);
	}

	public String getMediafilePath(Context context, FeedMedia media)
			throws DownloadRequestException {
		File externalStorage = getExternalFilesDirOrThrowException(
				context,
				MEDIA_DOWNLOADPATH
						+ FileNameGenerator.generateFileName(media.getItem()
								.getFeed().getTitle()) + "/");
		return externalStorage.toString();
	}

	private File getExternalFilesDirOrThrowException(Context context,
			String type) throws DownloadRequestException {
		File result = context.getExternalFilesDir(type);
		if (result == null) {
			throw new DownloadRequestException(
					"Failed to access external storage");
		}
		return result;
	}

	public String getMediafilename(FeedMedia media) {
		return URLUtil.guessFileName(media.getDownload_url(), null,
				media.getMime_type());
	}

}
