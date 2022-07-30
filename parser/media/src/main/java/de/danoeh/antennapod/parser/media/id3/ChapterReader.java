package de.danoeh.antennapod.parser.media.id3;

import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.EmbeddedChapterImage;
import de.danoeh.antennapod.parser.media.id3.model.FrameHeader;
import org.apache.commons.io.input.CountingInputStream;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads ID3 chapters.
 * See https://id3.org/id3v2-chapters-1.0
 */
public class ChapterReader extends ID3Reader {
    private static final String TAG = "ID3ChapterReader";

    public static final String FRAME_ID_CHAPTER = "CHAP";
    public static final String FRAME_ID_TITLE = "TIT2";
    public static final String FRAME_ID_LINK = "WXXX";
    public static final String FRAME_ID_PICTURE = "APIC";
    public static final String MIME_IMAGE_URL = "-->";
    public static final int IMAGE_TYPE_COVER = 3;

    private final List<Chapter> chapters = new ArrayList<>();

    public ChapterReader(CountingInputStream input) {
        super(input);
    }

    @Override
    protected void readFrame(@NonNull FrameHeader frameHeader) throws IOException, ID3ReaderException {
        if (FRAME_ID_CHAPTER.equals(frameHeader.getId())) {
            Log.d(TAG, "Handling frame: " + frameHeader.toString());
            Chapter chapter = readChapter(frameHeader);
            Log.d(TAG, "Chapter done: " + chapter);
            chapters.add(chapter);
        } else {
            super.readFrame(frameHeader);
        }
    }

    public Chapter readChapter(@NonNull FrameHeader frameHeader) throws IOException, ID3ReaderException {
        int chapterStartedPosition = getPosition();
        String elementId = readIsoStringNullTerminated(100);
        long startTime = readInt();
        skipBytes(12); // Ignore end time, start offset, end offset

        Chapter chapter = new Chapter();
        chapter.setStart(startTime);
        chapter.setChapterId(elementId);

        // Read sub-frames
        while (getPosition() < chapterStartedPosition + frameHeader.getSize()) {
            FrameHeader subFrameHeader = readFrameHeader();
            readChapterSubFrame(subFrameHeader, chapter);
        }
        return chapter;
    }

    public void readChapterSubFrame(@NonNull FrameHeader frameHeader, @NonNull Chapter chapter)
            throws IOException, ID3ReaderException {
        Log.d(TAG, "Handling subframe: " + frameHeader.toString());
        int frameStartPosition = getPosition();
        switch (frameHeader.getId()) {
            case FRAME_ID_TITLE:
                chapter.setTitle(readEncodingAndString(frameHeader.getSize()));
                Log.d(TAG, "Found title: " + chapter.getTitle());
                break;
            case FRAME_ID_LINK:
                readEncodingAndString(frameHeader.getSize()); // skip description
                String url = readIsoStringNullTerminated(frameStartPosition + frameHeader.getSize() - getPosition());
                try {
                    String decodedLink = URLDecoder.decode(url, "ISO-8859-1");
                    chapter.setLink(decodedLink);
                    Log.d(TAG, "Found link: " + chapter.getLink());
                } catch (IllegalArgumentException iae) {
                    Log.w(TAG, "Bad URL found in ID3 data");
                }
                break;
            case FRAME_ID_PICTURE:
                byte encoding = readByte();
                String mime = readIsoStringNullTerminated(frameHeader.getSize());
                byte type = readByte();
                String description = readEncodedString(encoding, frameHeader.getSize());
                Log.d(TAG, "Found apic: " + mime + "," + description);
                if (MIME_IMAGE_URL.equals(mime)) {
                    String link = readIsoStringNullTerminated(frameHeader.getSize());
                    Log.d(TAG, "Link: " + link);
                    if (TextUtils.isEmpty(chapter.getImageUrl()) || type == IMAGE_TYPE_COVER) {
                        chapter.setImageUrl(link);
                    }
                } else {
                    int alreadyConsumed = getPosition() - frameStartPosition;
                    int rawImageDataLength = frameHeader.getSize() - alreadyConsumed;
                    if (TextUtils.isEmpty(chapter.getImageUrl()) || type == IMAGE_TYPE_COVER) {
                        chapter.setImageUrl(EmbeddedChapterImage.makeUrl(getPosition(), rawImageDataLength));
                    }
                }
                break;
            default:
                Log.d(TAG, "Unknown chapter sub-frame.");
                break;
        }

        // Skip garbage to fill frame completely
        // This also asserts that we are not reading too many bytes from this frame.
        int alreadyConsumed = getPosition() - frameStartPosition;
        skipBytes(frameHeader.getSize() - alreadyConsumed);
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

}
