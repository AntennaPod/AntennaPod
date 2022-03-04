package de.danoeh.antennapod.parser.media.vorbis;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.parser.media.BuildConfig;

public class VorbisCommentChapterReader extends VorbisCommentReader {
    private static final String TAG = "VorbisCommentChptrReadr";

    private static final String CHAPTER_KEY = "chapter\\d\\d\\d.*";
    private static final String CHAPTER_ATTRIBUTE_TITLE = "name";
    private static final String CHAPTER_ATTRIBUTE_LINK = "url";
    private static final int CHAPTERXXX_LENGTH = "chapterxxx".length();

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
        String attribute = getAttributeTypeFromKey(key);
        int id = getIdFromKey(key);
        Chapter chapter = getChapterById(id);
        if (attribute == null) {
            if (getChapterById(id) == null) {
                // new chapter
                long start = getStartTimeFromValue(value);
                chapter = new Chapter();
                chapter.setChapterId("" + id);
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
            if (("" + id).equals(c.getChapterId())) {
                return c;
            }
        }
        return null;
    }

    public static long getStartTimeFromValue(String value)
            throws VorbisCommentReaderException {
        String[] parts = value.split(":");
        if (parts.length >= 3) {
            try {
                long hours = TimeUnit.MILLISECONDS.convert(
                        Long.parseLong(parts[0]), TimeUnit.HOURS);
                long minutes = TimeUnit.MILLISECONDS.convert(
                        Long.parseLong(parts[1]), TimeUnit.MINUTES);
                if (parts[2].contains("-->")) {
                    parts[2] = parts[2].substring(0, parts[2].indexOf("-->"));
                }
                long seconds = TimeUnit.MILLISECONDS.convert(
                        ((long) Float.parseFloat(parts[2])), TimeUnit.SECONDS);
                return hours + minutes + seconds;
            } catch (NumberFormatException e) {
                throw new VorbisCommentReaderException(e);
            }
        } else {
            throw new VorbisCommentReaderException("Invalid time string");
        }
    }

    /**
     * Return the id of a vorbiscomment chapter from a string like CHAPTERxxx*
     *
     * @return the id of the chapter key or -1 if the id couldn't be read.
     * @throws VorbisCommentReaderException
     * */
    private static int getIdFromKey(String key) throws VorbisCommentReaderException {
        if (key.length() >= CHAPTERXXX_LENGTH) { // >= CHAPTERxxx
            try {
                String strId = key.substring(8, 10);
                return Integer.parseInt(strId);
            } catch (NumberFormatException e) {
                throw new VorbisCommentReaderException(e);
            }
        }
        throw new VorbisCommentReaderException("key is too short (" + key + ")");
    }

    /**
     * Get the string that comes after 'CHAPTERxxx', for example 'name' or
     * 'url'.
     */
    private static String getAttributeTypeFromKey(String key) {
        if (key.length() > CHAPTERXXX_LENGTH) {
            return key.substring(CHAPTERXXX_LENGTH);
        }
        return null;
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

}
