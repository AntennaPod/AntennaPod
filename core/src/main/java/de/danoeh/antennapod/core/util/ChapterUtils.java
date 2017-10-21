package de.danoeh.antennapod.core.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.util.comparator.ChapterStartTimeComparator;
import de.danoeh.antennapod.core.util.id3reader.ChapterReader;
import de.danoeh.antennapod.core.util.id3reader.ID3ReaderException;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.vorbiscommentreader.VorbisCommentChapterReader;
import de.danoeh.antennapod.core.util.vorbiscommentreader.VorbisCommentReaderException;

/**
 * Utility class for getting chapter data from media files.
 */
public class ChapterUtils {

    private static final String TAG = "ChapterUtils";

    private ChapterUtils() {
    }

    @Nullable
    public static Chapter getCurrentChapter(Playable media) {
        if (media.getChapters() == null) {
            return null;
        }
        List<Chapter> chapters = media.getChapters();
        if (chapters == null) {
            return null;
        }
        Chapter current = chapters.get(0);
        for (Chapter sc : chapters) {
            if (sc.getStart() > media.getPosition()) {
                break;
            } else {
                current = sc;
            }
        }
        return current;
    }

    public static void loadChaptersFromStreamUrl(Playable media) {
        ChapterUtils.readID3ChaptersFromPlayableStreamUrl(media);
        if (media.getChapters() == null) {
            ChapterUtils.readOggChaptersFromPlayableStreamUrl(media);
        }
    }

    public static void loadChaptersFromFileUrl(Playable media) {
        if (!media.localFileAvailable()) {
            Log.e(TAG, "Could not load chapters from file url: local file not available");
            return;
        }
        ChapterUtils.readID3ChaptersFromPlayableFileUrl(media);
        if (media.getChapters() == null) {
            ChapterUtils.readOggChaptersFromPlayableFileUrl(media);
        }
    }

    /**
     * Uses the download URL of a media object of a feeditem to read its ID3
     * chapters.
     */
    private static void readID3ChaptersFromPlayableStreamUrl(Playable p) {
        if (p == null || p.getStreamUrl() == null) {
            Log.e(TAG, "Unable to read ID3 chapters: media or download URL was null");
            return;
        }
        Log.d(TAG, "Reading id3 chapters from item " + p.getEpisodeTitle());
        InputStream in = null;
        try {
            URL url = new URL(p.getStreamUrl());

            in = url.openStream();
            List<Chapter> chapters = readChaptersFrom(in);
            if(!chapters.isEmpty()) {
                p.setChapters(chapters);
            }
            Log.i(TAG, "Chapters loaded");
        } catch (IOException | ID3ReaderException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Uses the file URL of a media object of a feeditem to read its ID3
     * chapters.
     */
    private static void readID3ChaptersFromPlayableFileUrl(Playable p) {
        if (p == null || !p.localFileAvailable() || p.getLocalMediaUrl() == null) {
            return;
        }
        Log.d(TAG, "Reading id3 chapters from item " + p.getEpisodeTitle());
        File source = new File(p.getLocalMediaUrl());
        if (!source.exists()) {
            Log.e(TAG, "Unable to read id3 chapters: Source doesn't exist");
            return;
        }

        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(source));
            List<Chapter> chapters = readChaptersFrom(in);
            if (!chapters.isEmpty()) {
                p.setChapters(chapters);
            }
            Log.i(TAG, "Chapters loaded");
        } catch (IOException | ID3ReaderException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    @NonNull
    private static List<Chapter> readChaptersFrom(InputStream in) throws IOException, ID3ReaderException {
        ChapterReader reader = new ChapterReader();
        reader.readInputStream(in);
        List<Chapter> chapters = reader.getChapters();

        if (chapters == null) {
            Log.i(TAG, "ChapterReader could not find any ID3 chapters");
            return Collections.emptyList();
        }
        Collections.sort(chapters, new ChapterStartTimeComparator());
        enumerateEmptyChapterTitles(chapters);
        if (!chaptersValid(chapters)) {
            Log.e(TAG, "Chapter data was invalid");
            return Collections.emptyList();
        }
        return chapters;
    }

    private static void readOggChaptersFromPlayableStreamUrl(Playable media) {
        if (media == null || !media.streamAvailable()) {
            return;
        }
        InputStream input = null;
        try {
            URL url = new URL(media.getStreamUrl());
            input = url.openStream();
            if (input != null) {
                readOggChaptersFromInputStream(media, input);
            }
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    private static void readOggChaptersFromPlayableFileUrl(Playable media) {
        if (media == null || media.getLocalMediaUrl() == null) {
            return;
        }
        File source = new File(media.getLocalMediaUrl());
        if (source.exists()) {
            InputStream input = null;
            try {
                input = new BufferedInputStream(new FileInputStream(source));
                readOggChaptersFromInputStream(media, input);
            } catch (FileNotFoundException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            } finally {
                IOUtils.closeQuietly(input);
            }
        }
    }

    private static void readOggChaptersFromInputStream(Playable p, InputStream input) {
        Log.d(TAG, "Trying to read chapters from item with title " + p.getEpisodeTitle());
        try {
            VorbisCommentChapterReader reader = new VorbisCommentChapterReader();
            reader.readInputStream(input);
            List<Chapter> chapters = reader.getChapters();
            if (chapters == null) {
                Log.i(TAG, "ChapterReader could not find any Ogg vorbis chapters");
                return;
            }
            Collections.sort(chapters, new ChapterStartTimeComparator());
            enumerateEmptyChapterTitles(chapters);
            if (chaptersValid(chapters)) {
                p.setChapters(chapters);
                Log.i(TAG, "Chapters loaded");
            } else {
                Log.e(TAG, "Chapter data was invalid");
            }
        } catch (VorbisCommentReaderException e) {
            e.printStackTrace();
        }
    }

    /**
     * Makes sure that chapter does a title and an item attribute.
     */
    private static void enumerateEmptyChapterTitles(List<Chapter> chapters) {
        for (int i = 0; i < chapters.size(); i++) {
            Chapter c = chapters.get(i);
            if (c.getTitle() == null) {
                c.setTitle(Integer.toString(i));
            }
        }
    }

    private static boolean chaptersValid(List<Chapter> chapters) {
        if (chapters.isEmpty()) {
            return false;
        }
        for (Chapter c : chapters) {
            if (c.getStart() < 0) {
                return false;
            }
        }
        return true;
    }

}
