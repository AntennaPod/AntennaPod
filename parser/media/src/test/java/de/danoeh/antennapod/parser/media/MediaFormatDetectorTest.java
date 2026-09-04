package de.danoeh.antennapod.parser.media;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import org.junit.Test;

public class MediaFormatDetectorTest {

    @Test
    public void testMagicId3_detectsId3() throws Exception {
        assertEquals(MediaFormatDetector.Format.ID3,
                MediaFormatDetector.detect(new ByteArrayInputStream(new byte[] {0x49, 0x44, 0x33})).format);
    }

    @Test
    public void testMagicOgg_detectsOgg() throws Exception {
        assertEquals(MediaFormatDetector.Format.OGG,
                MediaFormatDetector.detect(new ByteArrayInputStream(new byte[] {0x4F, 0x67, 0x67, 0x53})).format);
    }

    @Test
    public void testMagicM4a_detectsM4a() throws Exception {
        assertEquals(MediaFormatDetector.Format.M4A,
                MediaFormatDetector.detect(new ByteArrayInputStream(new byte[] {0, 0, 0, 0, 0x66, 0x74, 0x79, 0x70})).format);
    }

    @Test
    public void testMagicZeroed_detectsUnknown() throws Exception {
        assertEquals(MediaFormatDetector.Format.UNKNOWN,
                MediaFormatDetector.detect(new ByteArrayInputStream(new byte[] {0, 0, 0, 0, 0, 0, 0, 0})).format);
    }

    @Test
    public void testMagicEmpty_detectsUnknown() throws Exception {
        assertEquals(MediaFormatDetector.Format.UNKNOWN,
                MediaFormatDetector.detect(new ByteArrayInputStream(new byte[] {})).format);
    }

    @Test
    public void testMagicShort_detectsUnknown() throws Exception {
        assertEquals(MediaFormatDetector.Format.UNKNOWN,
                MediaFormatDetector.detect(new ByteArrayInputStream(new byte[] {0x66, 0x74, 0x79})).format);
    }

    @Test
    public void testRawMp3FrameMagic_detectsUnknown() throws Exception {
        assertEquals(MediaFormatDetector.Format.UNKNOWN,
                MediaFormatDetector.detect(new ByteArrayInputStream(new byte[] {(byte) 0xFF, (byte) 0xFB, 0x00, 0x00})).format);
    }
}
