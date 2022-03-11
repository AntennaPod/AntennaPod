package de.danoeh.antennapod.parser.media.vorbis;

import de.danoeh.antennapod.model.feed.Chapter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class VorbisCommentChapterReaderTest {

    @Test
    public void testRealFilesAuphonic() throws IOException, VorbisCommentReaderException {
        testRealFileAuphonic("auphonic.ogg");
        testRealFileAuphonic("auphonic.opus");
    }

    public void testRealFileAuphonic(String filename) throws IOException, VorbisCommentReaderException {
        InputStream inputStream = getClass().getClassLoader()
                .getResource(filename).openStream();
        VorbisCommentChapterReader reader = new VorbisCommentChapterReader(inputStream);
        reader.readInputStream();
        List<Chapter> chapters = reader.getChapters();

        assertEquals(4, chapters.size());

        assertEquals(0, chapters.get(0).getStart());
        assertEquals(3000, chapters.get(1).getStart());
        assertEquals(6000, chapters.get(2).getStart());
        assertEquals(9000, chapters.get(3).getStart());

        assertEquals("Chapter 1 - ❤️😊", chapters.get(0).getTitle());
        assertEquals("Chapter 2 - ßöÄ", chapters.get(1).getTitle());
        assertEquals("Chapter 3 - 爱", chapters.get(2).getTitle());
        assertEquals("Chapter 4", chapters.get(3).getTitle());

        assertEquals("https://example.com", chapters.get(0).getLink());
        assertEquals("https://example.com", chapters.get(1).getLink());
        assertEquals("https://example.com", chapters.get(2).getLink());
        assertEquals("https://example.com", chapters.get(3).getLink());
    }
}
