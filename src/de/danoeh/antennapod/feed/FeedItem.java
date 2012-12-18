package de.danoeh.antennapod.feed;

import java.lang.ref.SoftReference;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.PodcastApp;

/**
 * Data Object for a XML message
 * 
 * @author daniel
 * 
 */
public class FeedItem extends FeedComponent {

	/** The id/guid that can be found in the rss/atom feed. Might not be set. */
	private String itemIdentifier;
	private String title;
	/**
	 * The description of a feeditem. This field should only be set by the
	 * parser.
	 */
	private String description;
	/**
	 * The content of the content-encoded tag of a feeditem. This field should
	 * only be set by the parser.
	 */
	private String contentEncoded;

	private SoftReference<String> cachedDescription;
	private SoftReference<String> cachedContentEncoded;

	private String link;
	private Date pubDate;
	private FeedMedia media;
	private Feed feed;
	private boolean read;
	private String paymentLink;
	private List<Chapter> chapters;

	public FeedItem() {
		this.read = true;
	}

	public void updateFromOther(FeedItem other) {
		super.updateFromOther(other);
		if (other.title != null) {
			title = other.title;
		}
		if (other.getDescription() != null) {
			description = other.getDescription();
		}
		if (other.getContentEncoded() != null) {
			contentEncoded = other.contentEncoded;
		}
		if (other.link != null) {
			link = other.link;
		}
		if (other.pubDate != null && other.pubDate != pubDate) {
			pubDate = other.pubDate;
		}
		if (other.media != null) {
			if (media == null) {
				media = other.media;
			} else if (media.compareWithOther(other)) {
				media.updateFromOther(other);
			}
		}
		if (other.paymentLink != null) {
			paymentLink = other.paymentLink;
		}
		if (other.chapters != null) {
			if (chapters == null) {
				chapters = other.chapters;
			}
		}
	}

	/**
	 * Moves the 'description' and 'contentEncoded' field of feeditem to their
	 * SoftReference fields.
	 */
	protected void cacheDescriptions() {
		if (description != null) {
			cachedDescription = new SoftReference<String>(description);
		}
		if (contentEncoded != null) {
			cachedContentEncoded = new SoftReference<String>(contentEncoded);
		}
		description = null;
		contentEncoded = null;
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

	/**
	 * Returns the value that uniquely identifies this FeedItem. If the
	 * itemIdentifier attribute is not null, it will be returned. Else it will
	 * try to return the title. If the title is not given, it will use the link
	 * of the entry.
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
		if (description == null && cachedDescription != null) {
			return cachedDescription.get();
		}
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
	}

	private boolean isInProgress() {
		return (media != null && media.isInProgress());
	}

	public String getContentEncoded() {
		if (contentEncoded == null && cachedContentEncoded != null) {
			return cachedContentEncoded.get();

		}
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

	private boolean isPlaying() {
		if (media != null) {
			if (PodcastApp.getCurrentlyPlayingMediaId() == media.getId()) {
				return true;
			}
		}
		return false;
	}

	public void setCachedDescription(String d) {
		cachedDescription = new SoftReference<String>(d);
	}

	public void setCachedContentEncoded(String c) {
		cachedContentEncoded = new SoftReference<String>(c);
	}

	public enum State {
		NEW, IN_PROGRESS, READ, PLAYING
	}

	public State getState() {
		if (hasMedia()) {
			if (isPlaying()) {
				return State.PLAYING;
			}
			if (isInProgress()) {
				return State.IN_PROGRESS;
			}
		}
		return (isRead() ? State.READ : State.NEW);
	}
}
