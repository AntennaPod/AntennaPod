package de.danoeh.antennapod.model.feed;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ChapterTest {

    private List<Chapter> buildChapters(long... startTimesMs) {
        List<Chapter> chapters = new ArrayList<>();
        for (long start : startTimesMs) {
            chapters.add(new Chapter(start, "chapter-" + start, null, null));
        }
        return chapters;
    }

    @Test
    public void testGetPreviousChapterIndexNoChapters() {
        assertEquals(-1, Chapter.getPreviousChapterIndex(null, 5000));
        assertEquals(-1, Chapter.getPreviousChapterIndex(new ArrayList<Chapter>(), 5000));
    }

    @Test
    public void testGetPreviousChapterIndexBeforeFirstChapter() {
        List<Chapter> chapters = buildChapters(10000, 20000, 30000);
        assertEquals(0, Chapter.getPreviousChapterIndex(chapters, 5000));
    }

    @Test
    public void testGetPreviousChapterIndexRestartsCurrentChapter() {
        List<Chapter> chapters = buildChapters(10000, 20000, 30000);
        // Well inside the second chapter: seek back to its start
        assertEquals(1, Chapter.getPreviousChapterIndex(chapters, 25000));
    }

    @Test
    public void testGetPreviousChapterIndexWithinThresholdGoesToPrevious() {
        List<Chapter> chapters = buildChapters(10000, 20000, 30000);
        // Within the first seconds of the second chapter: go to the previous one
        assertEquals(0, Chapter.getPreviousChapterIndex(chapters, 21000));
    }

    @Test
    public void testGetPreviousChapterIndexInsideFirstChapter() {
        List<Chapter> chapters = buildChapters(10000, 20000, 30000);
        assertEquals(0, Chapter.getPreviousChapterIndex(chapters, 15000));
    }

    @Test
    public void testGetPreviousChapterIndexSingleChapter() {
        List<Chapter> chapters = buildChapters(10000);
        assertEquals(0, Chapter.getPreviousChapterIndex(chapters, 15000));
    }
}
