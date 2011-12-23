package de.podfetcher.feed;


/**
 * Data Object for a XML message
 * @author daniel
 *
 */
public class FeedItem extends FeedComponent{
	public String title;
	public String description;
	public String link;
	public String pubDate;
	public FeedMedia media;
	public Feed feed;
	public boolean read;

	public FeedItem() {
			this.read = false;
	}
	
	public FeedItem(String title, String description, String link,
			String pubDate, FeedMedia media, Feed feed) {
		super();
		this.title = title;
		this.description = description;
		this.link = link;
		this.pubDate = pubDate;
		this.media = media;
		this.feed = feed;
		this.read = false;
	}
}
