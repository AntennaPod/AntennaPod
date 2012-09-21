package de.danoeh.antennapod.feed;

public class SimpleChapter extends Chapter {
	public static final int CHAPTERTYPE_SIMPLECHAPTER = 0;
	
	public SimpleChapter(long start, String title, FeedItem item, String link) {
		super(start, title, item, link);
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

	@Override
	public int getChapterType() {
		return CHAPTERTYPE_SIMPLECHAPTER;
	}

}
