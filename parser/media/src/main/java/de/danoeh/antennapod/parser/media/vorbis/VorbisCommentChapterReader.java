package de.danoeh.antennapod.parser.media.vorbis;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.parser.media.BuildConfig;

public class VorbisCommentChapterReader extends VorbisCommentReader {
    private static final String TAG = "VorbisCommentChptrReadr";

    private static final String CHAPTER_KEY = "chapter\\d\\d\\d.*";
    private static final String CHAPTER_ATTRIBUTE_TITLE = "name";
    private static final String CHAPTER_ATTRIBUTE_LINK = "url";

    private List<Chapter> chapters;

    public VorbisCommentChapterReader() {
    }

    @Override
    public void onVorbisCommentFound() {
        System.out.println("Vorbis comment found");
    }

    @Override
    public void onVorbisCommentHeaderFound(VorbisCommentHeader header) {
        chapters = new ArrayList<>();
        System.out.println(header.toString());
    }

    @Override
    public boolean onContentVectorKey(String content) {
        return content.matches(CHAPTER_KEY);
    }

    @Override
    public void onContentVectorValue(String key, String value) throws VorbisCommentReaderException {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Key: " + key + ", value: " + value);
        }
        String attribute = VorbisCommentChapter.getAttributeTypeFromKey(key);
        int id = VorbisCommentChapter.getIDFromKey(key);
        Chapter chapter = getChapterById(id);
        if (attribute == null) {
            if (getChapterById(id) == null) {
                // new chapter
                long start = VorbisCommentChapter.getStartTimeFromValue(value);
                chapter = new VorbisCommentChapter(id);
                chapter.setStart(start);
                chapters.add(chapter);
            } else {
                throw new VorbisCommentReaderException("Found chapter with duplicate ID (" + key + ", " + value + ")");
            }
        } else if (attribute.equals(CHAPTER_ATTRIBUTE_TITLE)) {
            if (chapter != null) {
                chapter.setTitle(value);
            }
        } else if (attribute.equals(CHAPTER_ATTRIBUTE_LINK)) {
            if (chapter != null) {
                chapter.setLink(value);
            }
        }
    }

    @Override
    public void onEndOfComment() {
        System.out.println("End of comment");
        for (Chapter c : chapters) {
            System.out.println(c.toString());
        }
    }

    @Override
    public void onError(VorbisCommentReaderException exception) {
        exception.printStackTrace();
    }

    private Chapter getChapterById(long id) {
        for (Chapter c : chapters) {
            if (((VorbisCommentChapter) c).getVorbisCommentId() == id) {
                return c;
            }
        }
        return null;
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

}
