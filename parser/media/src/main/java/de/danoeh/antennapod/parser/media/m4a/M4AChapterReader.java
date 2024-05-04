package de.danoeh.antennapod.parser.media.m4a;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.model.feed.Chapter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class M4AChapterReader {
    private static final String TAG = "M4AChapterReader";
    private final List<Chapter> chapters = new ArrayList<>();
    private final InputStream inputStream;
    private static final int FTYP_CODE = 0x66747970; // "ftyp"

    public M4AChapterReader(InputStream input) {
        inputStream = input;
    }

    /**
     * Read the input stream populating the chapters list
     */
    public void readInputStream() {
        try {
            isM4A(inputStream);
            int dataSize = this.findAtom("moov.udta.chpl");
            if (dataSize == -1) {
                Log.d(TAG, "Nero Chapter Atom not found");
            } else {
                Log.d(TAG, "Nero Chapter Atom found. Data Size: " + dataSize);
                this.parseNeroChapterAtom(dataSize);
            }
        } catch (Exception e) {
            Log.d(TAG, "ERROR: " + e.getMessage());
        }
    }

    /**
     * Find the atom with the given name in the M4A file
     *
     * @param name the name of the atom to find, separated by dots
     * @return the size of the atom (minus the 8-byte header) if found
     * @throws IOException if an I/O error occurs or the atom is not found
     */
    public int findAtom(String name) throws IOException {
        // Split the name into parts encoded as UTF-8
        String[] parts = name.split("\\.");
        int partIndex = 0;
        // Initialize remaining size to track the current part's size and check if it is exceeded
        int remainingSize = -1;

        // Read the M4A file atom by atom
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        while (true) {
            // Read the atom header
            IOUtils.readFully(inputStream, buffer.array());
            // Get the size of the current atom
            int chunkSize = buffer.getInt();
            int dataSize = chunkSize - 8;

            // Get the atom type
            String atomType = StandardCharsets.UTF_8.decode(buffer).toString();

            // Reset the buffer for reading the atom data
            buffer.clear();

            // Check if the current atom matches the current part of the name
            if (atomType.equals(parts[partIndex])) {
                if (partIndex == parts.length - 1) {
                    // If the current atom is the last part of the name return its size
                    return dataSize;
                } else {
                    // Else move to the next part of the name
                    partIndex++;
                    // Update the remaining size
                    remainingSize = dataSize;
                }
            } else {
                // Do not check the remaining size of top-level atoms
                if (partIndex > 0) {
                    // Update the remaining size
                    remainingSize -= dataSize;
                    // If the remaining size is exhausted, throw an exception
                    if (remainingSize <= 0) {
                        throw new IOException("Part size exceeded for part \"" + parts[partIndex - 1]
                                + "\" while searching atom. Remaining Size: " + remainingSize);
                    }
                }
                // Skip the rest of the atom
                IOUtils.skipFully(inputStream, dataSize);
            }
        }
    }

    /**
     * Parse the Nero Chapter Atom in the M4A file
     * Assumes that the current position is at the start of the Nero Chapter Atom
     *
     * @param chunkSize the size of the Nero Chapter Atom
     * @throws IOException if an I/O error occurs
     * @see <a href="https://github.com/Zeugma440/atldotnet/wiki/Focus-on-Chapter-metadata#nero-chapters">Nero Chapter</a>
     */
    private void parseNeroChapterAtom(long chunkSize) throws IOException {
        // Read the Nero Chapter Atom data into a buffer
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) chunkSize).order(ByteOrder.BIG_ENDIAN);
        IOUtils.readFully(inputStream, byteBuffer.array());
        // Skip the 5-byte header
        // Nero Chapter Atom consists of a 5-byte header followed by chapter data
        // The first 4 bytes are the version and flags, the 5th byte is reserved
        byteBuffer.position(5);
        // Get the chapter count
        int chapterCount = byteBuffer.getInt();
        Log.d(TAG, "Nero Chapter Count: " + chapterCount);

        // Parse each chapter
        for (int i = 0; i < chapterCount; i++) {
            long startTime = byteBuffer.getLong();
            int chapterNameSize = byteBuffer.get();
            byte[] chapterNameBytes = new byte[chapterNameSize];
            byteBuffer.get(chapterNameBytes, 0, chapterNameSize);
            String chapterName = new String(chapterNameBytes, StandardCharsets.UTF_8);

            Chapter chapter = new Chapter();
            chapter.setStart(startTime / 10000);
            chapter.setTitle(chapterName);
            chapter.setChapterId(String.valueOf(i + 1));
            chapters.add(chapter);

            Log.d(TAG, "Nero Chapter " + (i + 1) + ": " + chapter);
        }
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    /**
     * Assert that the input stream is an M4A file by checking the signature
     *
     * @param inputStream the input stream to check
     * @throws IOException if an I/O error occurs
     */
    public static void isM4A(InputStream inputStream) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        IOUtils.readFully(inputStream, byteBuffer.array());

        int ftypSize = byteBuffer.getInt();
        if (byteBuffer.getInt() != FTYP_CODE) {
            throw new IOException("Not an M4A file");
        }
        IOUtils.skipFully(inputStream, ftypSize - 8);
    }
}
