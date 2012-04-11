package de.podfetcher.storage;

import java.util.ArrayList;
import java.io.File;

import de.podfetcher.feed.*;
import de.podfetcher.service.DownloadService;
import de.podfetcher.util.NumberGenerator;

import android.util.Log;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;


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
				new File(context.getExternalFilesDir(MEDIA_DOWNLOADPATH), "media-" + media.size()),
				true);
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
}
