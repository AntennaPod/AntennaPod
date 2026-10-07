package de.danoeh.antennapod.parser.media.m4a;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class M4AMetadataReaderTest {

    @Test
    public void testRealFileFfmpeg() throws IOException {
        InputStream inputStream = getClass().getClassLoader()
                .getResource("ffmpeg.m4a").openStream();
        M4AMetadataReader reader = new M4AMetadataReader(inputStream);
        reader.readInputStream();
        assertEquals("Ünïcödé tëst — “smart quotes” ½ ≠ ⅓ · Ελληνικά · 日本語 · 한국어 · العربية 📝",
                reader.getDescription());
    }

    @Test
    public void testRealFileWithoutDescription() throws IOException {
        InputStream inputStream = getClass().getClassLoader()
                .getResource("nero-chapters.m4a").openStream();
        M4AMetadataReader reader = new M4AMetadataReader(inputStream);
        reader.readInputStream();
        assertNull(reader.getDescription());
    }
}
