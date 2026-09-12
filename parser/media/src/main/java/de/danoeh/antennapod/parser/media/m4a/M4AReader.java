package de.danoeh.antennapod.parser.media.m4a;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Reads the atom tree of an M4A file. Implementations declare the paths of the atoms they
 * need, such as "moov.udta.chpl". Only the atoms on the way to those paths are read, and
 * reading stops as soon as all of them have been found or turned out to be missing.
 */
public abstract class M4AReader {
    private static final int FTYP_CODE = 0x66747970; // "ftyp"
    private static final int HEADER_LENGTH = 8;
    private static final int LARGE_SIZE_LENGTH = 8;
    private static final int SIZE_UNTIL_END_OF_PARENT = 0;
    private static final int SIZE_IS_LARGE = 1;

    private final CountingInputStream inputStream;
    private final Set<String> pendingPaths = new HashSet<>();

    public M4AReader(InputStream input, String... atomPaths) {
        inputStream = new CountingInputStream(input);
        pendingPaths.addAll(Arrays.asList(atomPaths));
    }

    /**
     * Is called for every declared atom path that the file contains,
     * so that the implementation can read the payload of the atom.
     */
    protected abstract void onAtom(String path, long payloadLength) throws IOException;

    protected void readAtoms() throws IOException {
        assertM4A();
        try {
            readAtoms("", Long.MAX_VALUE);
        } catch (EOFException e) {
            // The file does not contain any further atoms
        }
    }

    private void readAtoms(String parentPath, long available) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH).order(ByteOrder.BIG_ENDIAN);
        while (available >= HEADER_LENGTH && !pendingPaths.isEmpty()) {
            header.clear();
            IOUtils.readFully(inputStream, header.array());
            long size = header.getInt() & 0xffffffffL;
            String type = StandardCharsets.ISO_8859_1.decode(header).toString();
            long headerLength = HEADER_LENGTH;
            if (size == SIZE_IS_LARGE) {
                size = readLargeSize();
                headerLength += LARGE_SIZE_LENGTH;
            } else if (size == SIZE_UNTIL_END_OF_PARENT) {
                size = available;
            }
            if (size < headerLength) {
                throw new IOException("Atom \"" + type + "\" has invalid size " + size);
            }

            String path = parentPath.isEmpty() ? type : parentPath + "." + type;
            long payloadLength = size - headerLength;
            long bytesReadBefore = inputStream.getByteCount();
            if (pendingPaths.remove(path)) {
                onAtom(path, payloadLength);
            } else if (hasPendingChildren(path)) {
                readAtoms(path, payloadLength);
                // Whatever is not inside this atom cannot be anywhere else in the file
                removePendingChildren(path);
            }
            if (pendingPaths.isEmpty()) {
                return;
            }
            // Skip what the handler did not read, so the next atom starts at the right position
            skipBytes(payloadLength - (inputStream.getByteCount() - bytesReadBefore));
            available -= size;
        }
    }

    private boolean hasPendingChildren(String path) {
        for (String pending : pendingPaths) {
            if (pending.startsWith(path + ".")) {
                return true;
            }
        }
        return false;
    }

    private void removePendingChildren(String path) {
        Iterator<String> iterator = pendingPaths.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().startsWith(path + ".")) {
                iterator.remove();
            }
        }
    }

    protected final void readFully(byte[] buffer) throws IOException {
        IOUtils.readFully(inputStream, buffer);
    }

    protected final void skipBytes(long number) throws IOException {
        IOUtils.skipFully(inputStream, number);
    }

    private long readLargeSize() throws IOException {
        byte[] buffer = new byte[LARGE_SIZE_LENGTH];
        IOUtils.readFully(inputStream, buffer);
        return ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN).getLong();
    }

    /**
     * Assert that the input stream is an M4A file by checking the signature
     */
    private void assertM4A() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(HEADER_LENGTH).order(ByteOrder.BIG_ENDIAN);
        IOUtils.readFully(inputStream, byteBuffer.array());

        int ftypSize = byteBuffer.getInt();
        if (byteBuffer.getInt() != FTYP_CODE) {
            throw new IOException("Not an M4A file");
        }
        skipBytes(ftypSize - HEADER_LENGTH);
    }
}
