package de.danoeh.antennapod.core.util;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.core.BuildConfig;
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

    /**
     * Uses the download URL of a media object of a feeditem to read its ID3
     * chapters.
     */
    public static void readID3ChaptersFromPlayableStreamUrl(Playable p) {
        if (p != null && p.getStreamUrl() != null) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Reading id3 chapters from item " + p.getEpisodeTitle());
            InputStream in = null;
            try {
                URL url = new URL(p.getStreamUrl());
                ChapterReader reader = new ChapterReader();

                in = url.openStream();
                reader.readInputStream(in);
                List<Chapter> chapters = reader.getChapters();

                if (chapters != null) {
                    Collections
                            .sort(chapters, new ChapterStartTimeComparator());
                    processChapters(chapters, p);
                    if (chaptersValid(chapters)) {
                        p.setChapters(chapters);
                        Log.i(TAG, "Chapters loaded");
                    } else {
                        Log.e(TAG, "Chapter data was invalid");
                    }
                } else {
                    Log.i(TAG, "ChapterReader could not find any ID3 chapters");
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ID3ReaderException e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            Log.e(TAG,
                    "Unable to read ID3 chapters: media or download URL was null");
        }
    }

    /**
     * Uses the file URL of a media object of a feeditem to read its ID3
     * chapters.
     */
    public static void readID3ChaptersFromPlayableFileUrl(Playable p) {
        if (p != null && p.localFileAvailable() && p.getLocalMediaUrl() != null) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Reading id3 chapters from item " + p.getEpisodeTitle());
            File source = new File(p.getLocalMediaUrl());
            if (source.exists()) {
                ChapterReader reader = new ChapterReader();
                InputStream in = null;

                try {
                    in = new BufferedInputStream(new FileInputStream(source));
                    reader.readInputStream(in);
                    List<Chapter> chapters = reader.getChapters();

                    if (chapters != null) {
                        Collections.sort(chapters,
                                new ChapterStartTimeComparator());
                        processChapters(chapters, p);
                        if (chaptersValid(chapters)) {
                            p.setChapters(chapters);
                            Log.i(TAG, "Chapters loaded");
                        } else {
                            Log.e(TAG, "Chapter data was invalid");
                        }
                    } else {
                        Log.i(TAG,
                                "ChapterReader could not find any ID3 chapters");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ID3ReaderException e) {
                    e.printStackTrace();
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                Log.e(TAG, "Unable to read id3 chapters: Source doesn't exist");
            }
        }
    }

    public static void readOggChaptersFromPlayableStreamUrl(Playable media) {
        if (media != null && media.streamAvailable()) {
            InputStream input = null;
            try {
                URL url = new URL(media.getStreamUrl());
                input = url.openStream();
                if (input != null) {
                    readOggChaptersFromInputStream(media, input);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(input);
            }
        }
    }

    public static void readOggChaptersFromPlayableFileUrl(Playable media) {
        if (media != null && media.getLocalMediaUrl() != null) {
            File source = new File(media.getLocalMediaUrl());
            if (source.exists()) {
                InputStream input = null;
                try {
                    input = new BufferedInputStream(new FileInputStream(source));
                    readOggChaptersFromInputStream(media, input);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    IOUtils.closeQuietly(input);
                }
            }
        }
    }

    private static void readOggChaptersFromInputStream(Playable p,
                                                       InputStream input) {
        if (BuildConfig.DEBUG)
            Log.d(TAG,
                    "Trying to read chapters from item with title "
                            + p.getEpisodeTitle());
        try {
            VorbisCommentChapterReader reader = new VorbisCommentChapterReader();
            reader.readInputStream(input);
            List<Chapter> chapters = reader.getChapters();
            if (chapters != null) {
                Collections.sort(chapters, new ChapterStartTimeComparator());
                processChapters(chapters, p);
                if (chaptersValid(chapters)) {
                    p.setChapters(chapters);
                    Log.i(TAG, "Chapters loaded");
                } else {
                    Log.e(TAG, "Chapter data was invalid");
                }
            } else {
                Log.i(TAG,
                        "ChapterReader could not find any Ogg vorbis chapters");
            }
        } catch (VorbisCommentReaderException e) {
            e.printStackTrace();
        }
    }

    /**
     * Makes sure that chapter does a title and an item attribute.
     */
    private static void processChapters(List<Chapter> chapters, Playable p) {
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
            if (c.getTitle() == null) {
                return false;
            }
            if (c.getStart() < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calls getCurrentChapter with current position.
     */
    public static Chapter getCurrentChapter(Playable media) {
        if (media.getChapters() != null) {
            List<Chapter> chapters = media.getChapters();
            Chapter current = null;
            if (chapters != null) {
                current = chapters.get(0);
                for (Chapter sc : chapters) {
                    if (sc.getStart() > media.getPosition()) {
                        break;
                    } else {
                        current = sc;
                    }
                }
            }
            return current;
        } else {
            return null;
        }
    }

    public static void loadChaptersFromStreamUrl(Playable media) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Starting chapterLoader thread");
        ChapterUtils.readID3ChaptersFromPlayableStreamUrl(media);
        if (media.getChapters() == null) {
            ChapterUtils.readOggChaptersFromPlayableStreamUrl(media);
        }

        if (BuildConfig.DEBUG)
            Log.d(TAG, "ChapterLoaderThread has finished");
    }

    public static void loadChaptersFromFileUrl(Playable media) {
        if (media.localFileAvailable()) {
            ChapterUtils.readID3ChaptersFromPlayableFileUrl(media);
            if (media.getChapters() == null) {
                ChapterUtils.readOggChaptersFromPlayableFileUrl(media);
            }
        } else {
            Log.e(TAG, "Could not load chapters from file url: local file not available");
        }
    }
}
