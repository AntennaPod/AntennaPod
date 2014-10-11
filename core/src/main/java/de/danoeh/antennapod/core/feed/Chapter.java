package de.danoeh.antennapod.core.feed;

public abstract class Chapter extends FeedComponent {

	/** Defines starting point in milliseconds. */
	protected long start;
	protected String title;
	protected String link;

	public Chapter() {
	}
	
	public Chapter(long start) {
		super();
		this.start = start;
	}

	public Chapter(long start, String title, FeedItem item, String link) {
		super();
		this.start = start;
		this.title = title;
		this.link = link;
	}

	public abstract int getChapterType();

	public long getStart() {
		return start;
	}

	public String getTitle() {
		return title;
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

	public void setLink(String link) {
		this.link = link;
	}

    @Override
    public String getHumanReadableIdentifier() {
        return title;
    }
}
