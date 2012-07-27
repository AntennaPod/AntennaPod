package de.danoeh.antennapod.feed;

public class SimpleChapter extends FeedComponent {
	/** Defines starting point in milliseconds. */
	private long start;
	private String title;
	private FeedItem item;
	private String link;

	public SimpleChapter(FeedItem item, long start, String title, String link) {
		super();
		this.item = item;
		this.start = start;
		this.title = title;
		this.link = link;
	}

	public String getTitle() {
		return title;
	}

	public FeedItem getItem() {
		return item;
	}

	public long getStart() {
		return start;
	}

	public void setItem(FeedItem item) {
		this.item = item;
	}

	public String getLink() {
		return link;
	}

}
