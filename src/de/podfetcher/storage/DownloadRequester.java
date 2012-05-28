package de.podfetcher.storage;

import java.util.ArrayList;
import java.io.File;
import java.util.concurrent.Callable;

import de.podfetcher.feed.*;
import de.podfetcher.service.DownloadService;
import de.podfetcher.util.NumberGenerator;
import de.podfetcher.R;

import android.util.Log;
import android.database.Cursor;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Messenger;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.content.ComponentName;
import android.os.Message;
import android.os.RemoteException;
import android.content.Intent;

public class DownloadRequester {
	private static final String TAG = "DownloadRequester";

	public static String EXTRA_DOWNLOAD_ID = "extra.de.podfetcher.storage.download_id";
	public static String EXTRA_ITEM_ID = "extra.de.podfetcher.storage.item_id";

	public static String ACTION_FEED_DOWNLOAD_COMPLETED = "action.de.podfetcher.storage.feed_download_completed";
	public static String ACTION_MEDIA_DOWNLOAD_COMPLETED = "action.de.podfetcher.storage.media_download_completed";
	public static String ACTION_IMAGE_DOWNLOAD_COMPLETED = "action.de.podfetcher.storage.image_download_completed";

	private static boolean STORE_ON_SD = true;
	public static String IMAGE_DOWNLOADPATH = "images/";
	public static String FEED_DOWNLOADPATH = "cache/";
	public static String MEDIA_DOWNLOADPATH = "media/";


	private static DownloadRequester downloader;
	private DownloadManager manager; 

	public ArrayList<FeedFile> feeds;
	public ArrayList<FeedFile> images;
	public ArrayList<FeedFile> media;

	private DownloadRequester(){
		feeds = new ArrayList<FeedFile>();
		images = new ArrayList<FeedFile>();
		media = new ArrayList<FeedFile>();

	}

	public static DownloadRequester getInstance() {
		if(downloader == null) {
			downloader = new DownloadRequester();
		}
		return downloader;
	}

	private void download(Context context, ArrayList<FeedFile> type, FeedFile item, File dest, boolean visibleInUI) {
		Log.d(TAG, "Requesting download of url "+ item.getDownload_url());
		type.add(item);
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(item.getDownload_url()));
		//request.allowScanningByMediaScanner();

		request.setDestinationUri(Uri.fromFile(dest));
		request.setVisibleInDownloadsUi(visibleInUI);
		// TODO Set Allowed Network Types
		DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		context.startService(new Intent(context, DownloadService.class));
		item.setDownloadId(manager.enqueue(request));
		item.setFile_url(dest.toString());	
	}
	public void downloadFeed(Context context, Feed feed) {
		download(context, feeds, feed, 
				new File(getFeedfilePath(context), getFeedfileName(feed)),
				true);
	}

	public void downloadImage(Context context, FeedImage image) {
		download(context, images, image, 
				new File(getImagefilePath(context), getImagefileName(image)),
				true);
	}

	public void downloadMedia(Context context, FeedMedia feedmedia) {
		download(context, media, feedmedia,
				new File(getMediafilePath(context, feedmedia), getMediafilename(feedmedia)),
				true);
	}

	public void cancelDownload(final Context context, final long id) {
		Log.d(TAG, "Cancelling download with id " + id);
		DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		int removed = manager.remove(id);	
		if (removed > 0) {
			// Delete downloads in lists
			Feed feed = getFeed(id);
			if (feed != null) {
				feeds.remove(feed);
			} else {
				FeedImage image = getFeedImage(id);
				if (image != null) {
					images.remove(image);
				} else {
					FeedMedia m = getFeedMedia(id);
					if (media != null) {
						media.remove(m);
					}
				}
			}
		}
	}

	/** Get a Feed by its download id */
	public Feed getFeed(long id) {
		for(FeedFile f: feeds) {
			if(f.getDownloadId() == id) {
				return (Feed) f;
			}
		}
		return null;
	}

	/** Get a FeedImage by its download id */
	public FeedImage getFeedImage(long id) {
		for(FeedFile f: images) {
			if(f.getDownloadId() == id) {
				return (FeedImage) f;
			}
		}
		return null;
	}


	/** Get media by its download id */
	public FeedMedia getFeedMedia(long id) {
		for(FeedFile f: media) {
			if(f.getDownloadId() == id) {
				return (FeedMedia) f;
			}
		}
		return null;
	}

	public void removeFeed(Feed f) {
		feeds.remove(f);	
	}

	public void removeFeedMedia(FeedMedia m) {
		media.remove(m);
	}

	public void removeFeedImage(FeedImage fi) {
		images.remove(fi);
	}


	/** Get the number of uncompleted Downloads */
	public int getNumberOfDownloads() {
		return feeds.size() + images.size() + media.size();
	}

	public int getNumberOfFeedDownloads() {
		return feeds.size();
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
		return context.getExternalFilesDir(MEDIA_DOWNLOADPATH).toString() + "/";
	}

	public String getMediafilename(FeedMedia media) {
		return "media-" + NumberGenerator.generateLong(media.getDownload_url());	
	}


	/* ------------ Methods for communicating with the DownloadService ------------- */
	private Messenger mService = null;
	boolean mIsBound;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);

			try {
				Message msg = Message.obtain(null, DownloadService.MSG_QUERY_DOWNLOADS_LEFT);
				Log.d(TAG, "Sending message to DownloadService.");
				mService.send(msg);
			} catch(RemoteException e) {
				Log.e(TAG, "An Exception happened while communication with the DownloadService");
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			mService = null;
			Log.i(TAG, "Closed connection with DownloadService.");
		}
	};


	/** Notifies the DownloadService to check if there are any Downloads left */
	public void notifyDownloadService(Context context) {
		context.bindService(new Intent(context, DownloadService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
		context.unbindService(mConnection);
		mIsBound = false;
	}
}
