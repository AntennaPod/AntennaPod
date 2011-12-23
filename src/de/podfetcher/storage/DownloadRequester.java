package de.podfetcher.storage;

import java.util.ArrayList;
import java.io.File;

import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedImage;
import de.podfetcher.feed.FeedMedia;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;


public class DownloadRequester {
	public static String EXTRA_DOWNLOAD_ID = "extra.de.podfetcher.storage.download_id";
	public static String EXTRA_ITEM_ID = "extra.de.podfetcher.storage.item_id";
	
	public static String ACTION_FEED_DOWNLOAD_COMPLETED = "action.de.podfetcher.storage.feed_download_completed";
	public static String ACTION_MEDIA_DOWNLOAD_COMPLETED = "action.de.podfetcher.storage.media_download_completed";
	public static String ACTION_IMAGE_DOWNLOAD_COMPLETED = "action.de.podfetcher.storage.image_download_completed";
	
	private static boolean STORE_ON_SD = true;
	public static String IMAGE_DOWNLOADPATH = "images";
	public static String FEED_DOWNLOADPATH = "cache";
	public static String MEDIA_DOWNLOADPATH = "media";
	
	
	private static DownloadRequester downloader;
	
	public ArrayList<Intent> feeds;
	public ArrayList<Intent> images;
	public ArrayList<Intent> media;
	
	private DownloadRequester(){
		feeds = new ArrayList<Intent>();
		images = new ArrayList<Intent>();
		media = new ArrayList<Intent>();
		
	}
	
	public static DownloadRequester getInstance() {
		if(downloader == null) {
			downloader = new DownloadRequester();
		}
		return downloader;
	}
	
	private void download(Context context, ArrayList<Intent> type, String str_uri, File dest, boolean visibleInUI, String action, long id) {
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(str_uri));
		//request.allowScanningByMediaScanner();

		request.setDestinationUri(Uri.fromFile(dest));
		request.setVisibleInDownloadsUi(visibleInUI);
		// TODO Set Allowed Network Types
		DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		Intent i = new Intent(action);
		i.putExtra(EXTRA_DOWNLOAD_ID, manager.enqueue(request));
		i.putExtra(EXTRA_ITEM_ID, id);
		type.add(i);
		
	}
	public void downloadFeed(Context context, Feed feed) {
		download(context, feeds, feed.download_url, 
				new File(getFeedfilePath(id), getFeedfileName(id)),
				true, ACTION_FEED_DOWNLOAD_COMPLETED, feed.id);
	}
	
	public void downloadImage(Context context, FeedImage image) {
		download(context, images, image.download_url,
				new File(context.getExternalFilesDir(IMAGE_DOWNLOADPATH), "image-" + image.id),
				true, ACTION_IMAGE_DOWNLOAD_COMPLETED, image.id);
	}
	
	public void downloadMedia(Context context, FeedMedia feedmedia) {
		download(context, media, feedmedia.download_url,
				new File(context.getExternalFilesDir(MEDIA_DOWNLOADPATH), "media-" + feedmedia.id),
				true, ACTION_MEDIA_DOWNLOAD_COMPLETED, feedmedia.id);
	}
	
	public void removeFeedByID(long id) {
		int len = feeds.size();
		for(int x = 0; x < len; x++) {
			if(feeds.get(x).getLongExtra(EXTRA_ITEM_ID, -1) == id) {
				feeds.remove(x);
				break;
			}
		}
	}
	
	public void removeMediaByID(long id) {
		int len = media.size();
		for(int x = 0; x < len; x++) {
			if(media.get(x).getLongExtra(EXTRA_ITEM_ID, -1) == id) {
				media.remove(x);
				break;
			}
		}
	}
	
	public void removeImageByID(long id) {
		int len = images.size();
		for(int x = 0; x < len; x++) {
			if(images.get(x).getLongExtra(EXTRA_ITEM_ID, -1) == id) {
				images.remove(x);
				break;
			}
		}
	}
	
	/* Returns the stored intent by looking for the right download id */
	public Intent getItemIntent(long id) {
		for(Intent i : feeds) {
			if(i.getLongExtra(EXTRA_DOWNLOAD_ID, -1) == id) {
				return i;
			}
		}
		for(Intent i : media) {
			if(i.getLongExtra(EXTRA_DOWNLOAD_ID, -1) == id) {
				return i;
			}
		}
		for(Intent i : images) {
			if(i.getLongExtra(EXTRA_DOWNLOAD_ID, -1) == id) {
				return i;
			}
		}
		return null;
	}

	public String getFeedfilePath() {
		return context.getExternalFilesDir(FEED_DOWNLOADPATH);	
	}

	public String getFeedfileName(long id) {
		return "feed-" + id;
	}
}
