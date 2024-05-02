package de.danoeh.antennapod.parser.media.m4a;

import de.danoeh.antennapod.model.feed.Chapter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class M4AChapterReaderTest {

    @Test
    public void testRealFilesAuphonic() throws IOException {
        testRealFileAuphonic("nero-chapters.m4a");
    }

    public void testRealFileAuphonic(String filename) throws IOException {
        InputStream inputStream = getClass().getClassLoader()
                .getResource(filename).openStream();
        M4AChapterReader reader = new M4AChapterReader(inputStream);
        reader.readInputStream();
        List<Chapter> chapters = reader.getChapters();

        assertEquals(3, chapters.size());

        assertEquals(0, chapters.get(0).getStart());
        assertEquals(23000, chapters.get(1).getStart());
        assertEquals(67000, chapters.get(2).getStart());

        assertEquals("An intro to video chapters", chapters.get(0).getTitle());
        assertEquals("How to set up video chapters", chapters.get(1).getTitle());
        assertEquals("See video chapters in action", chapters.get(2).getTitle());
    }
}
