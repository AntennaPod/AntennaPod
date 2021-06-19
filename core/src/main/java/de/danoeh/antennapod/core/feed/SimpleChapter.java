package de.danoeh.antennapod.core.feed;

import de.danoeh.antennapod.model.feed.Chapter;

public class SimpleChapter extends Chapter {
    public static final int CHAPTERTYPE_SIMPLECHAPTER = 0;

    public SimpleChapter(long start, String title, String link, String imageUrl) {
        super(start, title, link, imageUrl);
    }

    @Override
    public int getChapterType() {
        return CHAPTERTYPE_SIMPLECHAPTER;
    }
}
