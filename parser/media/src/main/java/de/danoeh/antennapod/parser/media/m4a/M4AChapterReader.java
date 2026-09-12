package de.danoeh.antennapod.parser.media.m4a;

import android.util.Log;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.model.feed.Chapter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class M4AChapterReader extends M4AReader {
    private static final String TAG = "M4AChapterReader";
    private static final String ATOM_NERO_CHAPTERS = "moov.udta.chpl";

    private final List<Chapter> chapters = new ArrayList<>();

    public M4AChapterReader(InputStream input) {
        super(input, ATOM_NERO_CHAPTERS);
    }

    /**
     * Read the input stream populating the chapters list
     */
    public void readInputStream() {
        try {
            readAtoms();
        } catch (Exception e) {
            Log.d(TAG, "ERROR: " + e.getMessage());
        }
    }

    @Override
    protected void onAtom(String path, long chunkSize) throws IOException {
        if (!ATOM_NERO_CHAPTERS.equals(path)) {
            throw new IllegalArgumentException("Received atom that is not chapter");
        }
        Log.d(TAG, "Nero Chapter Atom found. Data Size: " + chunkSize);
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) chunkSize).order(ByteOrder.BIG_ENDIAN);
        readFully(byteBuffer.array());
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
}
