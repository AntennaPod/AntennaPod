package de.podfetcher.feed;

import java.util.ArrayList;

import de.podfetcher.storage.*;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

/**
 * Singleton class Manages all feeds, categories and feeditems
 * 
 * 
 * */
public class FeedManager {
	private static final String TAG = "FeedManager";

	private static FeedManager singleton;

	private ArrayList<Feed> feeds;
	private ArrayList<FeedCategory> categories;
	private DownloadRequester requester;

	private FeedManager() {
		feeds = new ArrayList<Feed>();
		categories = new ArrayList<FeedCategory>();
		requester = DownloadRequester.getInstance();

	}

	public static FeedManager getInstance() {
		if (singleton == null) {
			singleton = new FeedManager();
		}
		return singleton;
	}

	public void refreshAllFeeds(Context context) {
		Log.d(TAG, "Refreshing all feeds.");
		for (Feed feed : feeds) {
			requester.downloadFeed(context, feed);
		}
	}

	private void addNewFeed(Context context, Feed feed) {
		feeds.add(feed);
		feed.setId(setFeed(context, feed));
		for (FeedItem item : feed.getItems()) {
			setFeedItem(context, item);
		}
	}

	/** Adds a new Feeditem if its not in the list */
	public void addFeedItem(Context context, FeedItem item) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		// Search list for feeditem
		Feed feed = item.getFeed();
		FeedItem foundItem = searchFeedItemByLink(feed, item.getLink());
		if (foundItem != null) {
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

	public void updateFeed(Context context, Feed newFeed) {
		// Look up feed in the feedslist
		Feed savedFeed = searchFeedByLink(newFeed.getLink());
		if (savedFeed == null) {
			Log.d(TAG,
					"Found no existing Feed with title " + newFeed.getTitle()
							+ ". Adding as new one.");
			// Add a new Feed
			addNewFeed(context, newFeed);
		} else {
			Log.d(TAG, "Feed with title " + newFeed.getTitle()
					+ " already exists. Syncing new with existing one.");
			// Look for new or updated Items
			for (FeedItem item : newFeed.getItems()) {
				FeedItem oldItem = searchFeedItemByLink(savedFeed,
						item.getLink());
				if (oldItem != null) {
					FeedItem newItem = searchFeedItemByLink(newFeed,
							item.getLink());
					if (newItem != null) {
						newItem.setRead(oldItem.isRead());
					}
				}
			}
			newFeed.setId(savedFeed.getId());
			savedFeed = newFeed;
			setFeed(context, newFeed);
		}

	}

	/** Get a Feed by its link */
	private Feed searchFeedByLink(String link) {
		for (Feed feed : feeds) {
			if (feed.getLink().equals(link)) {
				return feed;
			}
		}
		return null;
	}

	/** Get a FeedItem by its link */
	private FeedItem searchFeedItemByLink(Feed feed, String link) {
		for (FeedItem item : feed.getItems()) {
			if (item.getLink().equals(link)) {
				return item;
			}
		}
		return null;
	}

	/** Updates Information of an existing Feed */
	public long setFeed(Context context, Feed feed) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		return adapter.setFeed(feed);
	}

	public long setFeedItem(Context context, FeedItem item) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		return adapter.setFeedItem(item);
	}

	/** Updates information of an existing FeedImage */
	public long setFeedImage(Context context, FeedImage image) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		return adapter.setImage(image);
	}

	/** Updates information of an existing FeedMedia object. */
	public long setFeedMedia(Context context, FeedMedia media) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		return adapter.setMedia(media);
	}

	/** Get a Feed by its id */
	public Feed getFeed(long id) {
		for (Feed f : feeds) {
			if (f.id == id) {
				return f;
			}
		}
		return null;
	}

	/** Get a Feed Image by its id */
	public FeedImage getFeedImage(long id) {
		for (Feed f : feeds) {
			FeedImage image = f.getImage();
			if (image != null && image.getId() == id) {
				return image;
			}
		}
		return null;
	}

	/** Get a Feed Item by its id and its feed */
	public FeedItem getFeedItem(long id, Feed feed) {
		for (FeedItem item : feed.getItems()) {
			if (item.getId() == id) {
				return item;
			}
		}
		Log.e(TAG, "Couldn't find FeedItem with id " + id);
		return null;
	}

	/** Get a FeedMedia object by the id of the Media object and the feed object */
	public FeedMedia getFeedMedia(long id, Feed feed) {
		for (FeedItem item : feed.getItems()) {
			if (item.getMedia().getId() == id) {
				return item.getMedia();
			}
		}
		Log.e(TAG, "Couldn't find FeedMedia with id " + id);
		return null;
	}

	/** Reads the database */
	public void loadDBData(Context context) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		updateArrays(context);
	}

	public void updateArrays(Context context) {
		feeds.clear();
		categories.clear();
		extractFeedlistFromCursor(context);
	}

	private void extractFeedlistFromCursor(Context context) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		Cursor feedlistCursor = adapter.getAllFeedsCursor();
		if (feedlistCursor.moveToFirst()) {
			do {
				Feed feed = new Feed();

				feed.id = feedlistCursor.getLong(feedlistCursor
						.getColumnIndex(PodDBAdapter.KEY_ID));
				feed.setTitle(feedlistCursor.getString(feedlistCursor
						.getColumnIndex(PodDBAdapter.KEY_TITLE)));
				feed.setLink(feedlistCursor.getString(feedlistCursor
						.getColumnIndex(PodDBAdapter.KEY_LINK)));
				feed.setDescription(feedlistCursor.getString(feedlistCursor
						.getColumnIndex(PodDBAdapter.KEY_DESCRIPTION)));
				feed.setImage(adapter.getFeedImage(feedlistCursor
						.getLong(feedlistCursor
								.getColumnIndex(PodDBAdapter.KEY_IMAGE))));
				feed.file_url = feedlistCursor.getString(feedlistCursor
						.getColumnIndex(PodDBAdapter.KEY_FILE_URL));
				feed.download_url = feedlistCursor.getString(feedlistCursor
						.getColumnIndex(PodDBAdapter.KEY_DOWNLOAD_URL));
				feed.setDownloaded(feedlistCursor.getInt(feedlistCursor
						.getColumnIndex(PodDBAdapter.KEY_DOWNLOADED)) > 0);
				// Get FeedItem-Object
				Cursor itemlistCursor = adapter.getAllItemsOfFeedCursor(feed);
				feed.setItems(extractFeedItemsFromCursor(context, feed,
						itemlistCursor));

				feeds.add(feed);
			} while (feedlistCursor.moveToNext());
		}
		adapter.close();
	}

	private ArrayList<FeedItem> extractFeedItemsFromCursor(Context context,
			Feed feed, Cursor itemlistCursor) {
		ArrayList<FeedItem> items = new ArrayList<FeedItem>();
		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		if (itemlistCursor.moveToFirst()) {
			do {
				FeedItem item = new FeedItem();

				item.id = itemlistCursor.getLong(itemlistCursor
						.getColumnIndex(PodDBAdapter.KEY_ID));
				item.setFeed(feed);
				item.setTitle(itemlistCursor.getString(itemlistCursor
						.getColumnIndex(PodDBAdapter.KEY_TITLE)));
				item.setLink(itemlistCursor.getString(itemlistCursor
						.getColumnIndex(PodDBAdapter.KEY_LINK)));
				item.setDescription(itemlistCursor.getString(itemlistCursor
						.getColumnIndex(PodDBAdapter.KEY_DESCRIPTION)));
				item.setPubDate(itemlistCursor.getString(itemlistCursor
						.getColumnIndex(PodDBAdapter.KEY_PUBDATE)));
				item.setMedia(adapter.getFeedMedia(itemlistCursor
						.getLong(itemlistCursor
								.getColumnIndex(PodDBAdapter.KEY_MEDIA)), item));
				item.setRead((itemlistCursor.getInt(itemlistCursor
						.getColumnIndex(PodDBAdapter.KEY_READ)) > 0) ? true
						: false);

				items.add(item);
			} while (itemlistCursor.moveToNext());
		}
		adapter.close();
		return items;
	}

	public ArrayList<Feed> getFeeds() {
		return feeds;
	}

}
