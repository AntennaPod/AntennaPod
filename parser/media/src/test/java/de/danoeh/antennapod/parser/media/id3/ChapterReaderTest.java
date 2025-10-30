package de.danoeh.antennapod.parser.media.id3;

import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.EmbeddedChapterImage;
import de.danoeh.antennapod.parser.media.id3.model.FrameHeader;
import org.apache.commons.io.input.CountingInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ChapterReaderTest {
    private static final byte CHAPTER_WITHOUT_SUBFRAME_START_TIME = 23;
    private static final byte[] CHAPTER_WITHOUT_SUBFRAME = {
            'C', 'H', '1', 0, // String ID for mapping to CTOC
            0, 0, 0, CHAPTER_WITHOUT_SUBFRAME_START_TIME, // Start time
            0, 0, 0, 0, // End time
            0, 0, 0, 0, // Start offset
            0, 0, 0, 0 // End offset
    };

    @Test
    public void testReadFullTagWithChapter() throws IOException, ID3ReaderException {
        byte[] chapter = Id3ReaderTest.concat(
                Id3ReaderTest.generateFrameHeader(ChapterReader.FRAME_ID_CHAPTER, CHAPTER_WITHOUT_SUBFRAME.length),
                CHAPTER_WITHOUT_SUBFRAME);
        byte[] data = Id3ReaderTest.concat(
                Id3ReaderTest.generateId3Header(chapter.length),
                chapter);
        CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(data));
        ChapterReader reader = new ChapterReader(inputStream);
        reader.readInputStream();
        assertEquals(1, reader.getChapters().size());
        assertEquals(CHAPTER_WITHOUT_SUBFRAME_START_TIME, reader.getChapters().get(0).getStart());
    }

    @Test
    public void testReadFullTagWithMultipleChapters() throws IOException, ID3ReaderException {
        byte[] chapter = Id3ReaderTest.concat(
                Id3ReaderTest.generateFrameHeader(ChapterReader.FRAME_ID_CHAPTER, CHAPTER_WITHOUT_SUBFRAME.length),
                CHAPTER_WITHOUT_SUBFRAME);
        byte[] data = Id3ReaderTest.concat(
                Id3ReaderTest.generateId3Header(2 * chapter.length),
                chapter,
                chapter);
        CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(data));
        ChapterReader reader = new ChapterReader(inputStream);
        reader.readInputStream();
        assertEquals(2, reader.getChapters().size());
        assertEquals(CHAPTER_WITHOUT_SUBFRAME_START_TIME, reader.getChapters().get(0).getStart());
        assertEquals(CHAPTER_WITHOUT_SUBFRAME_START_TIME, reader.getChapters().get(1).getStart());
    }

    @Test
    public void testReadChapterWithoutSubframes() throws IOException, ID3ReaderException {
        FrameHeader header = new FrameHeader(ChapterReader.FRAME_ID_CHAPTER,
                CHAPTER_WITHOUT_SUBFRAME.length, (short) 0);
        CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(CHAPTER_WITHOUT_SUBFRAME));
        Chapter chapter = new ChapterReader(inputStream).readChapter(header);
        assertEquals(CHAPTER_WITHOUT_SUBFRAME_START_TIME, chapter.getStart());
    }

    @Test
    public void testReadChapterWithTitle() throws IOException, ID3ReaderException {
        byte[] title = {
            ID3Reader.ENCODING_ISO,
            'H', 'e', 'l', 'l', 'o', // Title
            0 // Null-terminated
        };
        byte[] chapterData = Id3ReaderTest.concat(
            CHAPTER_WITHOUT_SUBFRAME,
            Id3ReaderTest.generateFrameHeader(ChapterReader.FRAME_ID_TITLE, title.length),
            title);
        FrameHeader header = new FrameHeader(ChapterReader.FRAME_ID_CHAPTER, chapterData.length, (short) 0);
        CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(chapterData));
        ChapterReader reader = new ChapterReader(inputStream);
        Chapter chapter = reader.readChapter(header);
        assertEquals(CHAPTER_WITHOUT_SUBFRAME_START_TIME, chapter.getStart());
        assertEquals("Hello", chapter.getTitle());
    }

    @Test
    public void testReadTitleWithGarbage() throws IOException, ID3ReaderException {
        byte[] titleSubframeContent = {
                ID3Reader.ENCODING_ISO,
                'A', // Title
                0, // Null-terminated
                42, 42, 42, 42 // Garbage, should be ignored
        };
        FrameHeader header = new FrameHeader(ChapterReader.FRAME_ID_TITLE, titleSubframeContent.length, (short) 0);
        CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(titleSubframeContent));
        ChapterReader reader = new ChapterReader(inputStream);
        Chapter chapter = new Chapter();
        reader.readChapterSubFrame(header, chapter);
        assertEquals("A", chapter.getTitle());

        // Should skip the garbage and point to the next frame
        assertEquals(titleSubframeContent.length, reader.getPosition());
    }

    @Test
    public void testRealFileUltraschall() throws IOException, ID3ReaderException {
        CountingInputStream inputStream = new CountingInputStream(getClass().getClassLoader()
                .getResource("ultraschall5.mp3").openStream());
        ChapterReader reader = new ChapterReader(inputStream);
        reader.readInputStream();
        List<Chapter> chapters = reader.getChapters();

        assertEquals(3, chapters.size());

        assertEquals(0, chapters.get(0).getStart());
        assertEquals(4004, chapters.get(1).getStart());
        assertEquals(7999, chapters.get(2).getStart());

        assertEquals("Marke 1", chapters.get(0).getTitle());
        assertEquals("Marke 2", chapters.get(1).getTitle());
        assertEquals("Marke 3", chapters.get(2).getTitle());

        assertEquals("https://example.com", chapters.get(0).getLink());
        assertEquals("https://example.com", chapters.get(1).getLink());
        assertEquals("https://example.com", chapters.get(2).getLink());

        assertEquals(EmbeddedChapterImage.makeUrl(16073, 2750569), chapters.get(0).getImageUrl());
        assertEquals(EmbeddedChapterImage.makeUrl(2766765, 15740), chapters.get(1).getImageUrl());
        assertEquals(EmbeddedChapterImage.makeUrl(2782628, 2750569), chapters.get(2).getImageUrl());
    }

    @Test
    public void testRealFileAuphonic() throws IOException, ID3ReaderException {
        CountingInputStream inputStream = new CountingInputStream(getClass().getClassLoader()
                .getResource("auphonic.mp3").openStream());
        ChapterReader reader = new ChapterReader(inputStream);
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

        assertEquals(EmbeddedChapterImage.makeUrl(765, 308), chapters.get(0).getImageUrl());
        assertEquals(EmbeddedChapterImage.makeUrl(1271, 308), chapters.get(1).getImageUrl());
        assertEquals(EmbeddedChapterImage.makeUrl(1771, 308), chapters.get(2).getImageUrl());
        assertEquals(EmbeddedChapterImage.makeUrl(2259, 308), chapters.get(3).getImageUrl());
    }

    @Test
    public void testRealFileHindenburgJournalistPro() throws IOException, ID3ReaderException {
        CountingInputStream inputStream = new CountingInputStream(getClass().getClassLoader()
                .getResource("hindenburg-journalist-pro.mp3").openStream());
        ChapterReader reader = new ChapterReader(inputStream);
        reader.readInputStream();
        List<Chapter> chapters = reader.getChapters();

        assertEquals(2, chapters.size());

        assertEquals(0, chapters.get(0).getStart());
        assertEquals(5006, chapters.get(1).getStart());

        assertEquals("Chapter Marker 1", chapters.get(0).getTitle());
        assertEquals("Chapter Marker 2", chapters.get(1).getTitle());

        assertEquals("https://example.com/chapter1url", chapters.get(0).getLink());
        assertEquals("https://example.com/chapter2url", chapters.get(1).getLink());

        assertEquals(EmbeddedChapterImage.makeUrl(5330, 4015), chapters.get(0).getImageUrl());
        assertEquals(EmbeddedChapterImage.makeUrl(9498, 4364), chapters.get(1).getImageUrl());
    }

    @Test
    public void testRealFileMp3chapsPy() throws IOException, ID3ReaderException {
        CountingInputStream inputStream = new CountingInputStream(getClass().getClassLoader()
                .getResource("mp3chaps-py.mp3").openStream());
        ChapterReader reader = new ChapterReader(inputStream);
        reader.readInputStream();
        List<Chapter> chapters = reader.getChapters();

        assertEquals(4, chapters.size());

        assertEquals(0, chapters.get(0).getStart());
        assertEquals(7000, chapters.get(1).getStart());
        assertEquals(9000, chapters.get(2).getStart());
        assertEquals(11000, chapters.get(3).getStart());

        assertEquals("Start", chapters.get(0).getTitle());
        assertEquals("Chapter 1", chapters.get(1).getTitle());
        assertEquals("Chapter 2", chapters.get(2).getTitle());
        assertEquals("Chapter 3", chapters.get(3).getTitle());
    }
}
