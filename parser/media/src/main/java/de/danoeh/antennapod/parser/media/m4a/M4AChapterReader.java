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

    public M4AChapterReader(InputStream input) {
        inputStream = input;
    }

    /**
     * Read the input stream populating the chapters list
     */
    public void readInputStream() {
        try {
            int chunkSize = this.findAtom("moov.udta.chpl");
            if (chunkSize == -1) {
                Log.d(TAG, "Nero Chapter Box not found");
            } else {
                Log.d(TAG, "Nero Chapter Box found. Chunk Size: " + chunkSize);
                this.parseNeroChapterBox(chunkSize);
            }
        } catch (Exception e) {
            Log.d(TAG, "ERROR: " + e.getMessage());
        }
    }

    /**
     * Find the atom with the given name in the M4A file
     * @param name the name of the atom to find, separated by dots
     * @return the size of the atom if found
     * @throws IOException if an I/O error occurs or the atom is not found
     */
    public int findAtom(String name) throws IOException {
        // Split the name into parts
        String[] parts = name.split("\\.");
        int partIndex = 0;
        // Initialize remaining size to track the current part's size and check if it is exceeded
        int remainingSize = -1;

        // Read the M4A file in chunks of 4 bytes
        byte[] buffer = new byte[4];
        while (true) {
            // Read the size of the current box
            IOUtils.readFully(inputStream, buffer);
            // Get the size of the current box
            int chunkSize = ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN).getInt();
            int dataSize = chunkSize - 8;

            // Read the type of the current box
            IOUtils.readFully(inputStream, buffer);
            String boxType = new String(buffer, StandardCharsets.UTF_8);

            // Check if the current box matches the current part of the name
            if (boxType.equals(parts[partIndex])) {
                if (partIndex == parts.length - 1) {
                    // If the current box is the last part of the name return its size
                    return chunkSize;
                } else {
                    // Else move to the next part of the name
                    partIndex++;
                    // Update the remaining size
                    remainingSize = dataSize;
                }
            } else {
                // Do not check the remaining size of top-level boxes
                if (partIndex > 0) {
                    // Update the remaining size
                    remainingSize -= dataSize;
                    // If the remaining size is exhausted, throw an exception
                    if (remainingSize <= 0) {
                        throw new IOException("Part size exceeded for part \"" + parts[partIndex-1] + "\" while searching atom. Remaining Size: " + remainingSize);
                    }
                }
                // Skip the rest of the box
                IOUtils.skipFully(inputStream, dataSize);

            }
        }
    }

    /**
     * Parse the Nero Chapter Box in the M4A file
     * Assumes that the current position is at the start of the Nero Chapter Box
     * @param chunkSize the size of the Nero Chapter Box
     * @throws IOException if an I/O error occurs
     * @see <a href="https://github.com/Zeugma440/atldotnet/wiki/Focus-on-Chapter-metadata#nero-chapters">Nero Chapter</a>
     */
    private void parseNeroChapterBox(long chunkSize) throws IOException {
        // Read the Nero Chapter Box data into a buffer
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) chunkSize).order(ByteOrder.BIG_ENDIAN);
        IOUtils.readFully(inputStream, byteBuffer.array());
        // Skip the 5-byte header
        // Nero Chapter Box consists of a 5-byte header followed by chapter data
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
}
