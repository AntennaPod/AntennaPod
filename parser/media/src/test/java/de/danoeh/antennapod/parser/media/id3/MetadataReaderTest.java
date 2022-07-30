package de.danoeh.antennapod.parser.media.id3;

import org.apache.commons.io.input.CountingInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class MetadataReaderTest {
    @Test
    public void testRealFileUltraschall() throws IOException, ID3ReaderException {
        CountingInputStream inputStream = new CountingInputStream(getClass().getClassLoader()
                .getResource("ultraschall5.mp3").openStream());
        Id3MetadataReader reader = new Id3MetadataReader(inputStream);
        reader.readInputStream();
        assertEquals("Description", reader.getComment());
    }

    @Test
    public void testRealFileAuphonic() throws IOException, ID3ReaderException {
        CountingInputStream inputStream = new CountingInputStream(getClass().getClassLoader()
                .getResource("auphonic.mp3").openStream());
        Id3MetadataReader reader = new Id3MetadataReader(inputStream);
        reader.readInputStream();
        assertEquals("Summary", reader.getComment());
    }

    @Test
    public void testRealFileHindenburgJournalistPro() throws IOException, ID3ReaderException {
        CountingInputStream inputStream = new CountingInputStream(getClass().getClassLoader()
                .getResource("hindenburg-journalist-pro.mp3").openStream());
        Id3MetadataReader reader = new Id3MetadataReader(inputStream);
        reader.readInputStream();
        assertEquals("This is the summary of this podcast episode. This file was made with"
                + " Hindenburg Journalist Pro version 1.85, build number 2360.", reader.getComment());
    }

    @Test
    public void testRealFileMp3chapsPy() throws IOException, ID3ReaderException {
        CountingInputStream inputStream = new CountingInputStream(getClass().getClassLoader()
                .getResource("mp3chaps-py.mp3").openStream());
        Id3MetadataReader reader = new Id3MetadataReader(inputStream);
        reader.readInputStream();
        assertEquals("2021.08.13", reader.getComment());
    }
}
