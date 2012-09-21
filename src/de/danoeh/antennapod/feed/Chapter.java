package de.danoeh.antennapod.feed;

public abstract class Chapter extends FeedComponent {

	/** Defines starting point in milliseconds. */
	protected long start;
	protected String title;
	protected FeedItem item;
	protected String link;

	public Chapter(long start) {
		super();
		this.start = start;
	}

	public Chapter(long start, String title, FeedItem item, String link) {
		super();
		this.start = start;
		this.title = title;
		this.item = item;
		this.link = link;
	}

	public abstract int getChapterType();

	public long getStart() {
		return start;
	}

	public String getTitle() {
		return title;
	}

	public FeedItem getItem() {
		return item;
	}

	public String getLink() {
		return link;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setItem(FeedItem item) {
		this.item = item;
	}

	public void setLink(String link) {
		this.link = link;
	}

}
