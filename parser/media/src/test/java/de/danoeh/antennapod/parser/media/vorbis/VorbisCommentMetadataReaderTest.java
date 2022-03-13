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
    }

    public void testRealFileAuphonic(String filename) throws IOException, VorbisCommentReaderException {
        InputStream inputStream = getClass().getClassLoader()
                .getResource(filename).openStream();
        VorbisCommentMetadataReader reader = new VorbisCommentMetadataReader(inputStream);
        reader.readInputStream();
        assertEquals("Summary", reader.getDescription());
    }
}
