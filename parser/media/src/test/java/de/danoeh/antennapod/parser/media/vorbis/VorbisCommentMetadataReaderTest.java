package de.danoeh.antennapod.parser.media.vorbis;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class VorbisCommentMetadataReaderTest {

    @Test
    public void testRealFilesAuphonic() throws IOException, VorbisCommentReaderException {
        testRealFileAuphonic("auphonic.ogg");
        testRealFileAuphonic("auphonic.opus");
        testRealFileAuphonic("opus-comment.opus");
    }

    public void testRealFileAuphonic(String filename) throws IOException, VorbisCommentReaderException {
        InputStream inputStream = getClass().getClassLoader()
                .getResource(filename).openStream();
        VorbisCommentMetadataReader reader = new VorbisCommentMetadataReader(inputStream);
        reader.readInputStream();
        assertEquals("Summary", reader.getDescription());
    }

    @Test
    public void testRealFileFfmpegOgg() throws IOException, VorbisCommentReaderException {
        InputStream inputStream = getClass().getClassLoader()
                .getResource("ffmpeg.ogg").openStream();
        VorbisCommentMetadataReader reader = new VorbisCommentMetadataReader(inputStream);
        reader.readInputStream();
        assertEquals("Ünïcödé tëst — “smart quotes” ½ ≠ ⅓ · Ελληνικά · 日本語 · 한국어 · العربية 📝",
                reader.getDescription());
    }

    @Test
    public void testRealFileFfmpegOpus() throws IOException, VorbisCommentReaderException {
        InputStream inputStream = getClass().getClassLoader()
                .getResource("ffmpeg.opus").openStream();
        VorbisCommentMetadataReader reader = new VorbisCommentMetadataReader(inputStream);
        reader.readInputStream();
        assertEquals("Ünïcödé tëst — “smart quotes” ½ ≠ ⅓ · Ελληνικά · 日本語 · 한국어 · العربية 📝",
                reader.getDescription());
    }

    @Test
    public void testRealFileFfmpegSynopsis() throws IOException, VorbisCommentReaderException {
        InputStream inputStream = getClass().getClassLoader()
                .getResource("ffmpeg-synopsis.ogg").openStream();
        VorbisCommentMetadataReader reader = new VorbisCommentMetadataReader(inputStream);
        reader.readInputStream();
        assertEquals("This is the synopsis", reader.getDescription());
    }
}
