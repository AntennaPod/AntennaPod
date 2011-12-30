package de.podfetcher.feed;

import java.util.ArrayList;


import de.podfetcher.storage.*;
import android.content.Context;
import android.database.Cursor;


/**
 * Singleton class
 * Manages all feeds, categories and feeditems
 *  
 *
 *  */
public class FeedManager {
	
	private static FeedManager singleton;
	
	private ArrayList<Feed> feeds;
	private ArrayList<FeedCategory> categories;
	
	Cursor feedlistCursor;

	
	private FeedManager() {
		feeds = new ArrayList<Feed>();
		categories = new ArrayList<FeedCategory>();

	}
	
	public static FeedManager getInstance(){
		if(singleton == null) {
			singleton = new FeedManager();
		}
		return singleton;		
	}

	/** Add and Download a new Feed */
	public void addFeed(Context context, String url) {
		// TODO Check if URL is correct
		PodDBAdapter adapter = new PodDBAdapter(context);
		Feed feed = new Feed(url);
		feed.download_url = url;
		feed.id = adapter.setFeed(feed);
		// Add Feed to Feedlist if not available
		Feed foundFeed = getFeed(feed.id);
		if(foundFeed == null) {
			feeds.add(feed);
		}else {
			feed = foundFeed;
		}
		DownloadRequester req = DownloadRequester.getInstance();
		req.downloadFeed(context, feed);
		
	}

	/** Adds a new Feeditem if its not in the list */
	public void addFeedItem(Context context, FeedItem item) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		// Search list for feeditem
		Feed feed = item.getFeed();
		FeedItem foundItem = searchFeedItemByLink(feed, item.getLink());
		if(foundItem != null) {
			// Update Information
			item.id = foundItem.id;
			foundItem = item;
			item.setRead(foundItem.isRead());
			adapter.setFeedItem(item);
		} else {
			feed.getItems().add(item);	
			item.id = adapter.setFeedItem(item);
		}
	}

	/** Get a Feed by its link */
	private Feed searchFeedByLink(String link) {
		for(Feed feed : feeds) {
			if(feed.getLink().equals(link)) {
				return feed;
			}
		}
		return null;
	}

	/** Get a FeedItem by its link */
	private FeedItem searchFeedItemByLink(Feed feed, String link) {
		for(FeedItem item : feed.getItems()) {
			if(item.getLink().equals(link)) {
				return item;
			}
		}
		return null;
	}

	/** Updates Information of an existing Feed */
	public void setFeed(Context context, Feed feed) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.setFeed(feed);
	}

	/** Get a Feed by its id */
	public Feed getFeed(long id) {
		for(Feed f : feeds) {
			if(f.id == id) {
				return f;
			}
		}
		return null;
	}

	/** Get a Feed Image by its id */
	public FeedImage getFeedImage(long id) {
		for(Feed f : feeds) {
			FeedImage image = f.getImage();
			if(image != null && image.getId() == id) {
				return image;
			}
		}
		return null;
	}
	
	/** Reads the database */
	public void loadDBData(Context context) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		feedlistCursor = adapter.getAllFeedsCursor();
		updateArrays(context);
	}
	
	
	public void updateArrays(Context context) {
		feedlistCursor.requery();
		PodDBAdapter adapter = new PodDBAdapter(context);
		feeds.clear();
		categories.clear();
		extractFeedlistFromCursor(context);		
	}
	
	private void extractFeedlistFromCursor(Context context) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		if(feedlistCursor.moveToFirst()) {
			do {
				Feed feed = new Feed();
				
				feed.id = feedlistCursor.getLong(feedlistCursor.getColumnIndex(PodDBAdapter.KEY_ID));
				feed.setTitle(feedlistCursor.getString(feedlistCursor.getColumnIndex(PodDBAdapter.KEY_TITLE)));
				feed.setLink(feedlistCursor.getString(feedlistCursor.getColumnIndex(PodDBAdapter.KEY_LINK)));
				feed.setDescription(feedlistCursor.getString(feedlistCursor.getColumnIndex(PodDBAdapter.KEY_DESCRIPTION)));
				feed.setImage(adapter.getFeedImage(feed));
				feed.file_url = feedlistCursor.getString(feedlistCursor.getColumnIndex(PodDBAdapter.KEY_FILE_URL));
				feed.download_url = feedlistCursor.getString(feedlistCursor.getColumnIndex(PodDBAdapter.KEY_DOWNLOAD_URL));
				
				// Get FeedItem-Object
				Cursor itemlistCursor = adapter.getAllItemsOfFeedCursor(feed);
				feed.setItems(extractFeedItemsFromCursor(context, itemlistCursor));
				
				feeds.add(feed);
			}while(feedlistCursor.moveToNext());
		}
	}
	
	private ArrayList<FeedItem> extractFeedItemsFromCursor(Context context, Cursor itemlistCursor) {
		ArrayList<FeedItem> items = new ArrayList<FeedItem>();
		PodDBAdapter adapter = new PodDBAdapter(context);
		
		if(itemlistCursor.moveToFirst()) {
			do {
				FeedItem item = new FeedItem();
				
				item.id = itemlistCursor.getLong(itemlistCursor.getColumnIndex(PodDBAdapter.KEY_ID));
				item.setTitle(itemlistCursor.getString(itemlistCursor.getColumnIndex(PodDBAdapter.KEY_TITLE)));
				item.setLink(itemlistCursor.getString(itemlistCursor.getColumnIndex(PodDBAdapter.KEY_LINK)));
				item.setDescription(itemlistCursor.getString(itemlistCursor.getColumnIndex(PodDBAdapter.KEY_DESCRIPTION)));
				item.setPubDate(itemlistCursor.getString(itemlistCursor.getColumnIndex(PodDBAdapter.KEY_PUBDATE)));
				item.setMedia(adapter.getFeedMedia(itemlistCursor.getLong(itemlistCursor.getColumnIndex(PodDBAdapter.KEY_MEDIA))));
				item.setRead((itemlistCursor.getInt(itemlistCursor.getColumnIndex(PodDBAdapter.KEY_READ)) > 0) ? true : false);
				
				items.add(item);
			} while(itemlistCursor.moveToNext());
		}
		return items;
	}

	public ArrayList<Feed> getFeeds() {
		return feeds;
	}
	
	 
	

}
