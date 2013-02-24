package de.danoeh.antennapod.feed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.SyncResult;
import android.preference.PreferenceManager;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.util.EpisodeFilter;

/**
 * Data Object for a whole feed
 * 
 * @author daniel
 * 
 */
public class Feed extends FeedFile {
	public static final int FEEDFILETYPE_FEED = 0;
	public static final String TYPE_RSS2 = "rss";
	public static final String TYPE_RSS091 = "rss";
	public static final String TYPE_ATOM1 = "atom";

	private String title;
	/** Contains 'id'-element in Atom feed. */
	private String feedIdentifier;
	/** Link to the website. */
	private String link;
	private String description;
	private String language;
	/** Name of the author */
	private String author;
	private FeedImage image;
	private List<FeedItem> items;
	/** Date of last refresh. */
	private Date lastUpdate;
	private String paymentLink;
	/** Feed type, for example RSS 2 or Atom */
	private String type;

	public Feed(Date lastUpdate) {
		super();
		items = Collections.synchronizedList(new ArrayList<FeedItem>());
		this.lastUpdate = lastUpdate;
	}

	/**
	 * This constructor is used for requesting a feed download. It should NOT be
	 * used if the title of the feed is already known.
	 * */
	public Feed(String url, Date lastUpdate) {
		this(lastUpdate);
		this.download_url = url;
	}

	/**
	 * This constructor is used for requesting a feed download. It should be
	 * used if the title of the feed is already known.
	 * */
	public Feed(String url, Date lastUpdate, String title) {
		this(url, lastUpdate);
		this.title = title;
	}

	/**
	 * Returns the number of FeedItems where 'read' is false. If the 'display
	 * only episodes' - preference is set to true, this method will only count
	 * items with episodes.
	 * */
	public int getNumOfNewItems() {
		int count = 0;
		for (FeedItem item : items) {
			if (item.getState() == FeedItem.State.NEW) {
				if (!UserPreferences.isDisplayOnlyEpisodes()
						|| item.getMedia() != null) {
					count++;
				}
			}
		}
		return count;
	}

	/**
	 * Returns the number of FeedItems where the media started to play but
	 * wasn't finished yet.
	 * */
	public int getNumOfStartedItems() {
		int count = 0;

		for (FeedItem item : items) {
			FeedItem.State state = item.getState();
			if (state == FeedItem.State.IN_PROGRESS
					|| state == FeedItem.State.PLAYING) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Returns true if at least one item in the itemlist is unread.
	 * 
	 * @param enableEpisodeFilter
	 *            true if this method should only count items with episodes if
	 *            the 'display only episodes' - preference is set to true by the
	 *            user.
	 */
	public boolean hasNewItems(boolean enableEpisodeFilter) {
		for (FeedItem item : items) {
			if (item.getState() == FeedItem.State.NEW) {
				if (!(enableEpisodeFilter && UserPreferences
						.isDisplayOnlyEpisodes()) || item.getMedia() != null) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the number of FeedItems.
	 * 
	 * @param enableEpisodeFilter
	 *            true if this method should only count items with episodes if
	 *            the 'display only episodes' - preference is set to true by the
	 *            user.
	 * */
	public int getNumOfItems(boolean enableEpisodeFilter) {
		if (enableEpisodeFilter && UserPreferences.isDisplayOnlyEpisodes()) {
			return EpisodeFilter.countItemsWithEpisodes(items);
		} else {
			return items.size();
		}
	}

	/**
	 * Returns the item at the specified index.
	 * 
	 * @param enableEpisodeFilter
	 *            true if this method should ignore items without episdodes if
	 *            the episodes filter has been enabled by the user.
	 */
	public FeedItem getItemAtIndex(boolean enableEpisodeFilter, int position) {
		if (enableEpisodeFilter && UserPreferences.isDisplayOnlyEpisodes()) {
			return EpisodeFilter.accessEpisodeByIndex(items, position);
		} else {
			return items.get(position);
		}
	}

	/**
	 * Returns the value that uniquely identifies this Feed. If the
	 * feedIdentifier attribute is not null, it will be returned. Else it will
	 * try to return the title. If the title is not given, it will use the link
	 * of the feed.
	 * */
	public String getIdentifyingValue() {
		if (feedIdentifier != null && !feedIdentifier.isEmpty()) {
			return feedIdentifier;
		} else if (title != null && !title.isEmpty()) {
			return title;
		} else {
			return link;
		}
	}

	@Override
	public String getHumanReadableIdentifier() {
		if (title != null) {
			return title;
		} else {
			return download_url;
		}
	}

	/** Calls cacheDescriptions on all items. */
	protected void cacheDescriptionsOfItems() {
		if (items != null) {
			for (FeedItem item : items) {
				item.cacheDescriptions();
			}
		}
	}

	public void updateFromOther(Feed other) {
		super.updateFromOther(other);
		if (other.title != null) {
			title = other.title;
		}
		if (other.feedIdentifier != null) {
			feedIdentifier = other.feedIdentifier;
		}
		if (other.link != null) {
			link = other.link;
		}
		if (other.description != null) {
			description = other.description;
		}
		if (other.language != null) {
			language = other.language;
		}
		if (other.author != null) {
			author = other.author;
		}
		if (other.paymentLink != null) {
			paymentLink = other.paymentLink;
		}
	}

	public boolean compareWithOther(Feed other) {
		if (super.compareWithOther(other)) {
			return true;
		}
		if (!title.equals(other.title)) {
			return true;
		}
		if (other.feedIdentifier != null) {
			if (feedIdentifier == null
					|| !feedIdentifier.equals(other.feedIdentifier)) {
				return true;
			}
		}
		if (other.link != null) {
			if (link == null || !link.equals(other.link)) {
				return true;
			}
		}
		if (other.description != null) {
			if (description == null || !description.equals(other.description)) {
				return true;
			}
		}
		if (other.language != null) {
			if (language == null || !language.equals(other.language)) {
				return true;
			}
		}
		if (other.author != null) {
			if (author == null || !author.equals(other.author)) {
				return true;
			}
		}
		if (other.paymentLink != null) {
			if (paymentLink == null || !paymentLink.equals(other.paymentLink)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getTypeAsInt() {
		return FEEDFILETYPE_FEED;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public FeedImage getImage() {
		return image;
	}

	public void setImage(FeedImage image) {
		this.image = image;
	}

	List<FeedItem> getItems() {
		return items;
	}

	public void setItems(ArrayList<FeedItem> items) {
		this.items = Collections.synchronizedList(items);
	}

	/** Returns an array that contains all the feeditems of this feed. */
	public FeedItem[] getItemsArray() {
		return items.toArray(new FeedItem[items.size()]);
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public String getFeedIdentifier() {
		return feedIdentifier;
	}

	public void setFeedIdentifier(String feedIdentifier) {
		this.feedIdentifier = feedIdentifier;
	}

	public String getPaymentLink() {
		return paymentLink;
	}

	public void setPaymentLink(String paymentLink) {
		this.paymentLink = paymentLink;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
