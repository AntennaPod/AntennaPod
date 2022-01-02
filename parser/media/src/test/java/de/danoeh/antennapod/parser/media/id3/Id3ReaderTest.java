package de.danoeh.antennapod.parser.media.id3;

import de.danoeh.antennapod.parser.media.id3.model.FrameHeader;
import de.danoeh.antennapod.parser.media.id3.model.TagHeader;
import org.apache.commons.io.input.CountingInputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Id3ReaderTest {
    @Test
    public void testReadString() throws IOException {
        byte[] data = {
            ID3Reader.ENCODING_ISO,
            'T', 'e', 's', 't',
            0 // Null-terminated
        };
        CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(data));
        String string = new ID3Reader(inputStream).readEncodingAndString(1000);
        assertEquals("Test", string);
    }

    @Test
    public void testReadMultipleStrings() throws IOException {
        byte[] data = {
            ID3Reader.ENCODING_ISO,
            'F', 'o', 'o',
            0, // Null-terminated
            ID3Reader.ENCODING_ISO,
            'B', 'a', 'r',
            0 // Null-terminated
        };
        CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(data));
        ID3Reader reader = new ID3Reader(inputStream);
        assertEquals("Foo", reader.readEncodingAndString(1000));
        assertEquals("Bar", reader.readEncodingAndString(1000));
    }

    @Test
    public void testReadingLimit() throws IOException {
        byte[] data = {
            ID3Reader.ENCODING_ISO,
            'A', 'B', 'C', 'D'
        };
        CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(data));
        ID3Reader reader = new ID3Reader(inputStream);
        assertEquals("ABC", reader.readEncodingAndString(4)); // Includes encoding
        assertEquals('D', reader.readByte());
    }

    @Test
    public void testReadUtf16RespectsBom() throws IOException {
        byte[] data = {
            ID3Reader.ENCODING_UTF16_WITH_BOM,
            (byte) 0xff, (byte) 0xfe, // BOM: Little-endian
            'A', 0, 'B', 0, 'C', 0,
            0, 0, // Null-terminated
            ID3Reader.ENCODING_UTF16_WITH_BOM,
            (byte) 0xfe, (byte) 0xff, // BOM: Big-endian
            0, 'D', 0, 'E', 0, 'F',
            0, 0, // Null-terminated
        };
        CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(data));
        ID3Reader reader = new ID3Reader(inputStream);
        assertEquals("ABC", reader.readEncodingAndString(1000));
        assertEquals("DEF", reader.readEncodingAndString(1000));
    }

    @Test
    public void testReadUtf16NullPrefix() throws IOException {
        byte[] data = {
            ID3Reader.ENCODING_UTF16_WITH_BOM,
            (byte) 0xff, (byte) 0xfe, // BOM
            0x00, 0x01, // Latin Capital Letter A with macron (Ā)
            0, 0, // Null-terminated
        };
        CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(data));
        String string = new ID3Reader(inputStream).readEncodingAndString(1000);
        assertEquals("Ā", string);
    }

    @Test
    public void testReadingLimitUtf16() throws IOException {
        byte[] data = {
            ID3Reader.ENCODING_UTF16_WITHOUT_BOM,
            'A', 0, 'B', 0, 'C', 0, 'D', 0
        };
        CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(data));
        ID3Reader reader = new ID3Reader(inputStream);
        reader.readEncodingAndString(6); // Includes encoding, produces broken string
        assertTrue("Should respect limit even if it breaks a symbol", reader.getPosition() <= 6);
    }

    @Test
    public void testReadTagHeader() throws IOException, ID3ReaderException {
        byte[] data = generateId3Header(23);
        CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(data));
        TagHeader header = new ID3Reader(inputStream).readTagHeader();
        assertEquals("ID3", header.getId());
        assertEquals(42, header.getVersion());
        assertEquals(23, header.getSize());
    }

    @Test
    public void testReadFrameHeader() throws IOException {
        byte[] data = generateFrameHeader("CHAP", 42);
        CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(data));
        FrameHeader header = new ID3Reader(inputStream).readFrameHeader();
        assertEquals("CHAP", header.getId());
        assertEquals(42, header.getSize());
    }

    public static byte[] generateFrameHeader(String id, int size) {
        return concat(
            id.getBytes(StandardCharsets.ISO_8859_1), // Frame ID
            new byte[] {
                (byte) (size >> 24), (byte) (size >> 16),
                (byte) (size >> 8), (byte) (size), // Size
                0, 0 // Flags
            });
    }

    static byte[] generateId3Header(int size) {
        return new byte[] {
                'I', 'D', '3', // Identifier
                0, 42, // Version
                0, // Flags
                (byte) (size >> 24), (byte) (size >> 16),
                (byte) (size >> 8), (byte) (size), // Size
        };
    }

    static byte[] concat(byte[]... arrays) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            for (byte[] array : arrays) {
                outputStream.write(array);
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return outputStream.toByteArray();
    }
}
