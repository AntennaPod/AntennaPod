package de.danoeh.antennapod.feed;

import java.util.Date;
import java.util.List;

/**
 * Data Object for a XML message
 * 
 * @author daniel
 * 
 */
public class FeedItem extends FeedComponent {

	/** The id/guid that can be found in the rss/atom feed. Might not be set.*/
	private String itemIdentifier;
	private String title;
	private String description;
	private String contentEncoded;
	private String link;
	private Date pubDate;
	private FeedMedia media;
	private Feed feed;
	protected boolean read;
	private String paymentLink;
	private List<Chapter> chapters;

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

	/** Get the chapter that fits the position. */
	public Chapter getCurrentChapter(int position) {
		Chapter current = null;
		if (chapters != null) {
			current = chapters.get(0);
			for (Chapter sc : chapters) {
				if (sc.getStart() > position) {
					break;
				} else {
					current = sc;
				}
			}
		}
		return current;
	}

	/** Calls getCurrentChapter with current position. */
	public Chapter getCurrentChapter() {
		return getCurrentChapter(media.getPosition());
	}
	
	/** Returns the value that uniquely identifies this FeedItem. 
	 *  If the itemIdentifier attribute is not null, it will be returned.
	 *  Else it will try to return the title. If the title is not given, it will
	 *  use the link of the entry.
	 * */
	public String getIdentifyingValue() {
		if (itemIdentifier != null) {
			return itemIdentifier;
		} else if (title != null) {
			return title;
		} else {
			return link;
		}
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
		return read || isInProgress();
	}

	public void setRead(boolean read) {
		this.read = read;
		if (media != null) {
			media.setPosition(0);
		}
	}

	public boolean isInProgress() {
		return (media != null && media.isInProgress());
	}

	public String getContentEncoded() {
		return contentEncoded;
	}

	public void setContentEncoded(String contentEncoded) {
		this.contentEncoded = contentEncoded;
	}

	public String getPaymentLink() {
		return paymentLink;
	}

	public void setPaymentLink(String paymentLink) {
		this.paymentLink = paymentLink;
	}

	public List<Chapter> getChapters() {
		return chapters;
	}

	public void setChapters(List<Chapter> chapters) {
		this.chapters = chapters;
	}

	public String getItemIdentifier() {
		return itemIdentifier;
	}

	public void setItemIdentifier(String itemIdentifier) {
		this.itemIdentifier = itemIdentifier;
	}

	public boolean hasMedia() {
		return media != null;
	}

}
