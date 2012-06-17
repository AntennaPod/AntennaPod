package de.podfetcher.feed;

import java.util.Date;


/**
 * Data Object for a XML message
 * @author daniel
 *
 */
public class FeedItem extends FeedComponent implements Comparable<FeedItem>{
	private String title;
	private String description;
	private String link;
	private Date pubDate;
	private FeedMedia media;
	private Feed feed;
	protected boolean read;

	public FeedItem() {
			this.read = true;
	}
	
	public FeedItem(String title, String description, String link,
			Date pubDate, FeedMedia media, Feed feed) {
		super();
		this.title = title;
		this.description = description;
		this.link = link;
		this.pubDate = pubDate;
		this.media = media;
		this.feed = feed;
		this.read = true;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public Date getPubDate() {
		return pubDate;
	}

	public void setPubDate(Date pubDate) {
		this.pubDate = pubDate;
	}

	public FeedMedia getMedia() {
		return media;
	}

	public void setMedia(FeedMedia media) {
		this.media = media;
	}

	public Feed getFeed() {
		return feed;
	}

	public void setFeed(Feed feed) {
		this.feed = feed;
	}

	public boolean isRead() {
		return read;
	}

	@Override
	public int compareTo(FeedItem another) {
		long diff = pubDate.getTime() - another.getPubDate().getTime();
		return (int) Math.signum(diff);
	}
	
	
}
