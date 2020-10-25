package de.danoeh.antennapod.core.util;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import android.util.Log;

import java.net.URLConnection;
import de.danoeh.antennapod.core.ClientConfig;
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
import org.apache.commons.io.input.CountingInputStream;

/**
 * Utility class for getting chapter data from media files.
 */
public class ChapterUtils {

    private static final String TAG = "ChapterUtils";

    private ChapterUtils() {
    }

    public static int getCurrentChapterIndex(Playable media, int position) {
        if (media == null || media.getChapters() == null || media.getChapters().size() == 0) {
            return -1;
        }
        List<Chapter> chapters = media.getChapters();
        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).getStart() > position) {
                return i - 1;
            }
        }
        return chapters.size() - 1;
    }

    public static List<Chapter> loadChaptersFromStreamUrl(Playable media, Context context) {
        List<Chapter> chapters = ChapterUtils.readID3ChaptersFromPlayableStreamUrl(media, context);
        if (chapters == null) {
            chapters = ChapterUtils.readOggChaptersFromPlayableStreamUrl(media, context);
        }
        return chapters;
    }

    public static List<Chapter> loadChaptersFromFileUrl(Playable media) {
        if (!media.localFileAvailable()) {
            Log.e(TAG, "Could not load chapters from file url: local file not available");
            return null;
        }
        List<Chapter> chapters = ChapterUtils.readID3ChaptersFromPlayableFileUrl(media);
        if (chapters == null) {
            chapters = ChapterUtils.readOggChaptersFromPlayableFileUrl(media);
        }
        return chapters;
    }

    /**
     * Uses the download URL of a media object of a feeditem to read its ID3
     * chapters.
     */
    private static List<Chapter> readID3ChaptersFromPlayableStreamUrl(Playable p, Context context) {
        if (p == null || p.getStreamUrl() == null) {
            Log.e(TAG, "Unable to read ID3 chapters: media or download URL was null");
            return null;
        }
        Log.d(TAG, "Reading id3 chapters from item " + p.getEpisodeTitle());
        CountingInputStream in = null;
        try {
            if (p.getStreamUrl().startsWith(ContentResolver.SCHEME_CONTENT)) {
                Uri uri = Uri.parse(p.getStreamUrl());
                in = new CountingInputStream(context.getContentResolver().openInputStream(uri));
            } else {
                URL url = new URL(p.getStreamUrl());
                URLConnection urlConnection = url.openConnection();
                urlConnection.setRequestProperty("User-Agent", ClientConfig.USER_AGENT);
                in = new CountingInputStream(urlConnection.getInputStream());
            }
            List<Chapter> chapters = readChaptersFrom(in);
            if (!chapters.isEmpty()) {
                return chapters;
            }
            Log.i(TAG, "Chapters loaded");
        } catch (IOException | ID3ReaderException | IllegalArgumentException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            IOUtils.closeQuietly(in);
        }
        return null;
    }

    /**
     * Uses the file URL of a media object of a feeditem to read its ID3
     * chapters.
     */
    private static List<Chapter> readID3ChaptersFromPlayableFileUrl(Playable p) {
        if (p == null || !p.localFileAvailable() || p.getLocalMediaUrl() == null) {
            return null;
        }
        Log.d(TAG, "Reading id3 chapters from item " + p.getEpisodeTitle());
        File source = new File(p.getLocalMediaUrl());
        if (!source.exists()) {
            Log.e(TAG, "Unable to read id3 chapters: Source doesn't exist");
            return null;
        }

        CountingInputStream in = null;
        try {
            in = new CountingInputStream(new BufferedInputStream(new FileInputStream(source)));
            List<Chapter> chapters = readChaptersFrom(in);
            if (!chapters.isEmpty()) {
                return chapters;
            }
            Log.i(TAG, "Chapters loaded");
        } catch (IOException | ID3ReaderException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            IOUtils.closeQuietly(in);
        }
        return null;
    }

    @NonNull
    private static List<Chapter> readChaptersFrom(CountingInputStream in) throws IOException, ID3ReaderException {
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

    private static List<Chapter> readOggChaptersFromPlayableStreamUrl(Playable media, Context context) {
        if (media == null || !media.streamAvailable()) {
            return null;
        }
        InputStream input = null;
        try {
            if (media.getStreamUrl().startsWith(ContentResolver.SCHEME_CONTENT)) {
                Uri uri = Uri.parse(media.getStreamUrl());
                input = context.getContentResolver().openInputStream(uri);
            } else {
                URL url = new URL(media.getStreamUrl());
                URLConnection urlConnection = url.openConnection();
                urlConnection.setRequestProperty("User-Agent", ClientConfig.USER_AGENT);
                input = urlConnection.getInputStream();
            }
            if (input != null) {
                return readOggChaptersFromInputStream(media, input);
            }
        } catch (IOException | IllegalArgumentException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            IOUtils.closeQuietly(input);
        }
        return null;
    }

    private static List<Chapter> readOggChaptersFromPlayableFileUrl(Playable media) {
        if (media == null || media.getLocalMediaUrl() == null) {
            return null;
        }
        File source = new File(media.getLocalMediaUrl());
        if (source.exists()) {
            InputStream input = null;
            try {
                input = new BufferedInputStream(new FileInputStream(source));
                return readOggChaptersFromInputStream(media, input);
            } catch (FileNotFoundException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            } finally {
                IOUtils.closeQuietly(input);
            }
        }
        return null;
    }

    private static List<Chapter> readOggChaptersFromInputStream(Playable p, InputStream input) {
        Log.d(TAG, "Trying to read chapters from item with title " + p.getEpisodeTitle());
        try {
            VorbisCommentChapterReader reader = new VorbisCommentChapterReader();
            reader.readInputStream(input);
            List<Chapter> chapters = reader.getChapters();
            if (chapters == null) {
                Log.i(TAG, "ChapterReader could not find any Ogg vorbis chapters");
                return null;
            }
            Collections.sort(chapters, new ChapterStartTimeComparator());
            enumerateEmptyChapterTitles(chapters);
            if (chaptersValid(chapters)) {
                Log.i(TAG, "Chapters loaded");
                return chapters;
            } else {
                Log.e(TAG, "Chapter data was invalid");
            }
        } catch (VorbisCommentReaderException e) {
            e.printStackTrace();
        }
        return null;
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
