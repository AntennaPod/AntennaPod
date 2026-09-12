package de.danoeh.antennapod.parser.media.vorbis;

import androidx.annotation.NonNull;
import org.apache.commons.io.EndianUtils;
import org.apache.commons.io.IOUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

public abstract class VorbisCommentReader {
    private static final String TAG = "VorbisCommentReader";
    private static final int SECOND_PAGE_MAX_LENGTH = 64 * 1024 * 1024;
    private static final int PACKET_TYPE_COMMENT = 3;
    private static final byte[] FLAC_MAGIC = {'f', 'L', 'a', 'C'};
    private static final byte[] FLAC_IN_OGG_MAGIC = {0x7F, 'F', 'L', 'A', 'C'};
    private static final int FLAC_IN_OGG_HEADER_LENGTH = 8;
    private static final int FLAC_LAST_BLOCK_FLAG = 0x80;
    private static final int FLAC_BLOCK_TYPE_MASK = 0x7F;
    private static final int FLAC_BLOCK_TYPE_COMMENT = 4;
    private static final int FLAC_METADATA_MAX_LENGTH = 16 * 1024 * 1024;

    private InputStream input;
    private long currentValueLength = 0;

    VorbisCommentReader(InputStream input) {
        this.input = input;
    }

    public void readInputStream() throws VorbisCommentReaderException {
        try {
            findCommentHeader();
            VorbisCommentHeader commentHeader = readCommentHeader();
            Log.d(TAG, commentHeader.toString());
            for (int i = 0; i < commentHeader.getUserCommentLength(); i++) {
                readUserComment();
            }
        } catch (IOException e) {
            Log.d(TAG, "Vorbis parser: " + e.getMessage());
        }
    }

    private void readUserComment() throws VorbisCommentReaderException {
        try {
            long vectorLength = EndianUtils.readSwappedUnsignedInteger(input);
            if (vectorLength > 20 * 1024 * 1024) {
                String keyPart = readUtf8String(10);
                throw new VorbisCommentReaderException("User comment unrealistically long. "
                        + "key=" + keyPart + ", length=" + vectorLength);
            }
            String key = readContentVectorKey(vectorLength).toLowerCase(Locale.US);
            Log.d(TAG, "key=" + key + ", length=" + vectorLength);
            currentValueLength = vectorLength - key.length() - 1;
            onContentVector(key);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readUtf8String(long length) throws IOException {
        byte[] buffer = new byte[(int) length];
        IOUtils.readFully(input, buffer);
        Charset charset = Charset.forName("UTF-8");
        return charset.newDecoder().decode(ByteBuffer.wrap(buffer)).toString();
    }

    private void findCommentHeader() throws IOException {
        PushbackInputStream sniffedInput = new PushbackInputStream(input, FLAC_MAGIC.length);
        byte[] magic = new byte[FLAC_MAGIC.length];
        IOUtils.readFully(sniffedInput, magic);
        if (Arrays.equals(magic, FLAC_MAGIC)) {
            // Native FLAC keeps the comments in a metadata block instead of an ogg page
            input = sniffedInput;
            findFlacCommentBlock();
            return;
        }
        sniffedInput.unread(magic);
        input = new VorbisInputStream(sniffedInput);

        byte[] buffer = new byte[64]; // Enough space for some bytes. Used circularly.
        final byte[] oggCommentHeader = new byte[]{ PACKET_TYPE_COMMENT, 'v', 'o', 'r', 'b', 'i', 's' };
        for (int bytesRead = 0; bytesRead < SECOND_PAGE_MAX_LENGTH; bytesRead++) {
            buffer[bytesRead % buffer.length] = (byte) input.read();
            if (bufferMatches(buffer, oggCommentHeader, bytesRead)) {
                return;
            } else if (bufferMatches(buffer, "OpusTags".getBytes(), bytesRead)) {
                return;
            } else if (bufferMatches(buffer, FLAC_IN_OGG_MAGIC, bytesRead)) {
                IOUtils.skipFully(input, FLAC_IN_OGG_HEADER_LENGTH);
                findFlacCommentBlock();
                return;
            }
        }
        throw new IOException("No comment header found");
    }

    /**
     * Skips the FLAC metadata blocks until the one holding the comments is reached.
     */
    private void findFlacCommentBlock() throws IOException {
        byte[] header = new byte[4]; // Last-block flag and type, followed by the length
        long bytesRead = 0;
        while (true) {
            IOUtils.readFully(input, header);
            if ((header[0] & FLAC_BLOCK_TYPE_MASK) == FLAC_BLOCK_TYPE_COMMENT) {
                return;
            }
            // 24 bit big endian
            long blockLength = ((header[1] & 0xffL) << 16) | ((header[2] & 0xffL) << 8) | (header[3] & 0xffL);
            bytesRead += header.length + blockLength;
            if ((header[0] & FLAC_LAST_BLOCK_FLAG) != 0 || bytesRead > FLAC_METADATA_MAX_LENGTH) {
                throw new IOException("No comment block found");
            }
            IOUtils.skipFully(input, blockLength);
        }
    }

    /**
     * Reads backwards in haystack, starting at position. Checks if the bytes match needle.
     * Uses haystack circularly, so when reading at (-1), it reads at (length - 1).
     */
    boolean bufferMatches(byte[] haystack, byte[] needle, int position) {
        for (int i = 0; i < needle.length; i++) {
            int posInHaystack = position - i;
            while (posInHaystack < 0) {
                posInHaystack += haystack.length;
            }
            posInHaystack = posInHaystack % haystack.length;
            if (haystack[posInHaystack] != needle[needle.length - 1 - i]) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    private VorbisCommentHeader readCommentHeader() throws IOException, VorbisCommentReaderException {
        try {
            long vendorLength = EndianUtils.readSwappedUnsignedInteger(input);
            String vendorName = readUtf8String(vendorLength);
            long userCommentLength = EndianUtils.readSwappedUnsignedInteger(input);
            return new VorbisCommentHeader(vendorName, userCommentLength);
        } catch (UnsupportedEncodingException e) {
            throw new VorbisCommentReaderException(e);
        }
    }

    private String readContentVectorKey(long vectorLength) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < vectorLength; i++) {
            char c = (char) input.read();
            if (c == '=') {
                return builder.toString();
            } else {
                builder.append(c);
            }
        }
        return null; // no key found
    }

    /**
     * Is called for every content vector that the reader finds. Implementations should call
     * super for the keys they do not handle, which skips the value without loading it into
     * memory. Values can be several megabytes large, for example embedded cover images.
     */
    protected void onContentVector(String key) throws IOException, VorbisCommentReaderException {
        IOUtils.skipFully(input, currentValueLength);
    }

    /**
     * Reads the value of the content vector that is currently being handled.
     */
    protected final String readValue() throws IOException {
        return readUtf8String(currentValueLength);
    }
}
