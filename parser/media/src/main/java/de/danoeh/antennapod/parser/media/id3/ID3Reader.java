package de.danoeh.antennapod.parser.media.id3;

import android.util.Log;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.parser.media.id3.model.FrameHeader;
import de.danoeh.antennapod.parser.media.id3.model.TagHeader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;

/**
 * Reads the ID3 Tag of a given file.
 * See https://id3.org/id3v2.3.0
 */
public class ID3Reader {
    private static final String TAG = "ID3Reader";
    private static final int FRAME_ID_LENGTH = 4;
    public static final byte ENCODING_ISO = 0;
    public static final byte ENCODING_UTF16_WITH_BOM = 1;
    public static final byte ENCODING_UTF16_WITHOUT_BOM = 2;
    public static final byte ENCODING_UTF8 = 3;

    private TagHeader tagHeader;
    private final CountingInputStream inputStream;

    public ID3Reader(CountingInputStream input) {
        inputStream = input;
    }

    public void readInputStream() throws IOException, ID3ReaderException {
        tagHeader = readTagHeader();
        int tagContentStartPosition = getPosition();
        while (getPosition() < tagContentStartPosition + tagHeader.getSize()) {
            FrameHeader frameHeader = readFrameHeader();
            if (frameHeader.getId().charAt(0) < '0' || frameHeader.getId().charAt(0) > 'z') {
                Log.d(TAG, "Stopping because of invalid frame: " + frameHeader.toString());
                return;
            }
            readFrame(frameHeader);
        }
    }

    protected void readFrame(@NonNull FrameHeader frameHeader) throws IOException, ID3ReaderException {
        Log.d(TAG, "Skipping frame: " + frameHeader.getId() + ", size: " + frameHeader.getSize());
        skipBytes(frameHeader.getSize());
    }

    int getPosition() {
        return inputStream.getCount();
    }

    /**
     * Skip a certain number of bytes on the given input stream.
     */
    void skipBytes(int number) throws IOException, ID3ReaderException {
        if (number < 0) {
            throw new ID3ReaderException("Trying to read a negative number of bytes");
        }
        IOUtils.skipFully(inputStream, number);
    }

    byte readByte() throws IOException {
        return (byte) inputStream.read();
    }

    short readShort() throws IOException {
        char firstByte = (char) inputStream.read();
        char secondByte = (char) inputStream.read();
        return (short) ((firstByte << 8) | secondByte);
    }

    int readInt() throws IOException {
        char firstByte = (char) inputStream.read();
        char secondByte = (char) inputStream.read();
        char thirdByte = (char) inputStream.read();
        char fourthByte = (char) inputStream.read();
        return (firstByte << 24) | (secondByte << 16) | (thirdByte << 8) | fourthByte;
    }

    void expectChar(char expected) throws ID3ReaderException, IOException {
        char read = (char) inputStream.read();
        if (read != expected) {
            throw new ID3ReaderException("Expected " + expected + " and got " + read);
        }
    }

    @NonNull
    TagHeader readTagHeader() throws ID3ReaderException, IOException {
        expectChar('I');
        expectChar('D');
        expectChar('3');
        short version = readShort();
        byte flags = readByte();
        int size = unsynchsafe(readInt());
        if ((flags & 0b01000000) != 0) {
            int extendedHeaderSize = readInt();
            skipBytes(extendedHeaderSize - 4);
        }
        return new TagHeader("ID3", size, version, flags);
    }

    @NonNull
    FrameHeader readFrameHeader() throws IOException {
        String id = readPlainBytesToString(FRAME_ID_LENGTH);
        int size = readInt();
        if (tagHeader != null && tagHeader.getVersion() >= 0x0400) {
            size = unsynchsafe(size);
        }
        short flags = readShort();
        return new FrameHeader(id, size, flags);
    }

    private int unsynchsafe(int in) {
        int out = 0;
        int mask = 0x7F000000;

        while (mask != 0) {
            out >>= 1;
            out |= in & mask;
            mask >>= 8;
        }

        return out;
    }

    /**
     * Reads a null-terminated string with encoding.
     */
    protected String readEncodingAndString(int max) throws IOException {
        byte encoding = readByte();
        return readEncodedString(encoding, max - 1);
    }

    protected String readPlainBytesToString(int length) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        int bytesRead = 0;
        while (bytesRead < length) {
            stringBuilder.append((char) readByte());
            bytesRead++;
        }
        return stringBuilder.toString();
    }

    protected String readIsoStringNullTerminated(int max) throws IOException {
        return readEncodedString(ENCODING_ISO, max);
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    String readEncodedString(int encoding, int max) throws IOException {
        if (encoding == ENCODING_UTF16_WITH_BOM || encoding == ENCODING_UTF16_WITHOUT_BOM) {
            return readEncodedString2(Charset.forName("UTF-16"), max);
        } else if (encoding == ENCODING_UTF8) {
            return readEncodedString2(Charset.forName("UTF-8"), max);
        } else {
            return readEncodedString1(Charset.forName("ISO-8859-1"), max);
        }
    }

    /**
     * Reads chars where the encoding uses 1 char per symbol.
     */
    private String readEncodedString1(Charset charset, int max) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int bytesRead = 0;
        while (bytesRead < max) {
            byte c = readByte();
            bytesRead++;
            if (c == 0) {
                break;
            }
            bytes.write(c);
        }
        return charset.newDecoder().decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
    }

    /**
     * Reads chars where the encoding uses 2 chars per symbol.
     */
    private String readEncodedString2(Charset charset, int max) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int bytesRead = 0;
        boolean foundEnd = false;
        while (bytesRead + 1 < max) {
            byte c1 = readByte();
            byte c2 = readByte();
            if (c1 == 0 && c2 == 0) {
                foundEnd = true;
                break;
            }
            bytesRead += 2;
            bytes.write(c1);
            bytes.write(c2);
        }
        if (!foundEnd && bytesRead < max) {
            // Last character
            byte c = readByte();
            if (c != 0) {
                bytes.write(c);
            }
        }
        try {
            return charset.newDecoder().decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
        } catch (MalformedInputException e) {
            return "";
        }
    }
}
