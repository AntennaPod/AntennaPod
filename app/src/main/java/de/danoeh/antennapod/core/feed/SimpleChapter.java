package de.danoeh.antennapod.core.feed;

public class SimpleChapter extends Chapter {
	public static final int CHAPTERTYPE_SIMPLECHAPTER = 0;
	
	public SimpleChapter(long start, String title, FeedItem item, String link) {
		super(start, title, item, link);
	}

	@Override
	public int getChapterType() {
		return CHAPTERTYPE_SIMPLECHAPTER;
	}

	public void updateFromOther(SimpleChapter other) {
		super.updateFromOther(other);
		start = other.start;
		if (other.title != null) {
			title = other.title;
		}
		if (other.link != null) {
			link = other.link;
		}
	}
}
