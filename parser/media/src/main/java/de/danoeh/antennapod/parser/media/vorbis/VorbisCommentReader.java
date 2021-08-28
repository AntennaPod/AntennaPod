package de.danoeh.antennapod.parser.media.vorbis;

import androidx.annotation.NonNull;
import org.apache.commons.io.EndianUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Locale;

public abstract class VorbisCommentReader {
    /** Length of first page in an ogg file in bytes. */
    private static final int FIRST_OGG_PAGE_LENGTH = 58;
    private static final int FIRST_OPUS_PAGE_LENGTH = 47;
    private static final int SECOND_PAGE_MAX_LENGTH = 64 * 1024 * 1024;
    private static final int PACKET_TYPE_IDENTIFICATION = 1;
    private static final int PACKET_TYPE_COMMENT = 3;

    /** Called when Reader finds identification header. */
    protected abstract void onVorbisCommentFound();

    protected abstract void onVorbisCommentHeaderFound(VorbisCommentHeader header);

    /**
     * Is called every time the Reader finds a content vector. The handler
     * should return true if it wants to handle the content vector.
     */
    protected abstract boolean onContentVectorKey(String content);

    /**
     * Is called if onContentVectorKey returned true for the key.
     */
    protected abstract void onContentVectorValue(String key, String value) throws VorbisCommentReaderException;

    protected abstract void onNoVorbisCommentFound();

    protected abstract void onEndOfComment();

    protected abstract void onError(VorbisCommentReaderException exception);

    public void readInputStream(InputStream input) throws VorbisCommentReaderException {
        try {
            // look for identification header
            if (findIdentificationHeader(input)) {
                onVorbisCommentFound();
                input = new OggInputStream(input);
                if (findCommentHeader(input)) {
                    VorbisCommentHeader commentHeader = readCommentHeader(input);
                    onVorbisCommentHeaderFound(commentHeader);
                    for (int i = 0; i < commentHeader.getUserCommentLength(); i++) {
                        readUserComment(input);
                    }
                    onEndOfComment();
                } else {
                    onError(new VorbisCommentReaderException("No comment header found"));
                }
            } else {
                onNoVorbisCommentFound();
            }
        } catch (IOException e) {
            onError(new VorbisCommentReaderException(e));
        }
    }

    private void readUserComment(InputStream input) throws VorbisCommentReaderException {
        try {
            long vectorLength = EndianUtils.readSwappedUnsignedInteger(input);
            String key = readContentVectorKey(input, vectorLength).toLowerCase(Locale.US);
            boolean readValue = onContentVectorKey(key);
            if (readValue) {
                String value = readUtf8String(input, (int) (vectorLength - key.length() - 1));
                onContentVectorValue(key, value);
            } else {
                IOUtils.skipFully(input, vectorLength - key.length() - 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readUtf8String(InputStream input, long length) throws IOException {
        byte[] buffer = new byte[(int) length];
        IOUtils.readFully(input, buffer);
        Charset charset = Charset.forName("UTF-8");
        return charset.newDecoder().decode(ByteBuffer.wrap(buffer)).toString();
    }

    /**
     * Looks for an identification header in the first page of the file. If an
     * identification header is found, it will be skipped completely and the
     * method will return true, otherwise false.
     */
    private boolean findIdentificationHeader(InputStream input) throws IOException {
        byte[] buffer = new byte[FIRST_OPUS_PAGE_LENGTH];
        IOUtils.readFully(input, buffer);
        final byte[] oggIdentificationHeader = new byte[]{ PACKET_TYPE_IDENTIFICATION, 'v', 'o', 'r', 'b', 'i', 's' };
        for (int i = 6; i < buffer.length; i++) {
            if (bufferMatches(buffer, oggIdentificationHeader, i)) {
                IOUtils.skip(input, FIRST_OGG_PAGE_LENGTH - FIRST_OPUS_PAGE_LENGTH);
                return true;
            } else if (bufferMatches(buffer, "OpusHead".getBytes(), i)) {
                return true;
            }
        }
        return false;
    }

    private boolean findCommentHeader(InputStream input) throws IOException {
        byte[] buffer = new byte[64]; // Enough space for some bytes. Used circularly.
        final byte[] oggCommentHeader = new byte[]{ PACKET_TYPE_COMMENT, 'v', 'o', 'r', 'b', 'i', 's' };
        for (int bytesRead = 0; bytesRead < SECOND_PAGE_MAX_LENGTH; bytesRead++) {
            buffer[bytesRead % buffer.length] = (byte) input.read();
            if (bufferMatches(buffer, oggCommentHeader, bytesRead)) {
                return true;
            } else if (bufferMatches(buffer, "OpusTags".getBytes(), bytesRead)) {
                return true;
            }
        }
        return false;
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
    private VorbisCommentHeader readCommentHeader(InputStream input) throws IOException, VorbisCommentReaderException {
        try {
            long vendorLength = EndianUtils.readSwappedUnsignedInteger(input);
            String vendorName = readUtf8String(input, vendorLength);
            long userCommentLength = EndianUtils.readSwappedUnsignedInteger(input);
            return new VorbisCommentHeader(vendorName, userCommentLength);
        } catch (UnsupportedEncodingException e) {
            throw new VorbisCommentReaderException(e);
        }
    }

    private String readContentVectorKey(InputStream input, long vectorLength) throws IOException {
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
}
