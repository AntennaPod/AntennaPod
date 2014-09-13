package de.danoeh.antennapod.feed;

import wseemann.media.FFmpegChapter;

/**
 * Represents a chapter contained in a MP4 file.
 */
public class MP4Chapter extends Chapter {
    public static final int CHAPTERTYPE_MP4CHAPTER = 4;

    /**
     * Construct a MP4Chapter from an FFmpegChapter.
     */
    public MP4Chapter(FFmpegChapter ch) {
        this.start = ch.getStart();
        this.title = ch.getTitle();
    }

    public MP4Chapter(long start, String title, FeedItem item, String link) {
        super(start, title, item, link);
    }

    @Override
    public int getChapterType() {
        return CHAPTERTYPE_MP4CHAPTER;
    }
}
