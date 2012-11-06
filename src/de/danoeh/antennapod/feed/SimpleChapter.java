package de.danoeh.antennapod.feed;

public class SimpleChapter extends Chapter {
	public static final int CHAPTERTYPE_SIMPLECHAPTER = 0;
	
	public SimpleChapter(long start, String title, FeedItem item, String link) {
		super(start, title, item, link);
	}

	@Override
	public int getChapterType() {
		return CHAPTERTYPE_SIMPLECHAPTER;
	}

}
