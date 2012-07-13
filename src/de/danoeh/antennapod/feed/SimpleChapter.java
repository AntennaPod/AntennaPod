package de.danoeh.antennapod.feed;

public class SimpleChapter extends FeedComponent {
	public long getStart() {
		return start;
	}
		
	public SimpleChapter(long start, String title) {
		super();
		this.start = start;
		this.title = title;
	}

	public String getTitle() {
		return title;
	}
	/** Defines starting point in milliseconds. */
	private long start;
	private String title;
	
	
}
