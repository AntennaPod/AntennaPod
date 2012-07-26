package de.danoeh.antennapod.feed;

import java.util.ArrayList;
import java.util.Date;

/**
 * Data Object for a whole feed
 * 
 * @author daniel
 * 
 */
public class Feed extends FeedFile {
	private static final String TYPE_RSS2 = "rss";
	private static final String TYPE_ATOM1 = "atom";
	
	private String title;
	/** Link to the website. */
	private String link;
	private String description;
	private String language;
	/** Name of the author */
	private String author;
	private FeedImage image;
	private FeedCategory category;
	private ArrayList<FeedItem> items;
	/** Date of last refresh. */
	private Date lastUpdate;
	private String paymentLink;
	/** Feed type, for example RSS 2 or Atom */
	private String type;

	public Feed(Date lastUpdate) {
		super();
		items = new ArrayList<FeedItem>();
		this.lastUpdate = lastUpdate;
	}

	public Feed(String url, Date lastUpdate) {
		this(lastUpdate);
		this.download_url = url;
	}
	
	/** Returns the number of FeedItems where 'read' is false. */
	public int getNumOfNewItems() {
		int count = 0;
		for (FeedItem item : items) {
			if (!item.isRead()) {
				count++;
			} 
		}
		return count;
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

	public FeedCategory getCategory() {
		return category;
	}

	public void setCategory(FeedCategory category) {
		this.category = category;
	}

	public ArrayList<FeedItem> getItems() {
		return items;
	}

	public void setItems(ArrayList<FeedItem> items) {
		this.items = items;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
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
