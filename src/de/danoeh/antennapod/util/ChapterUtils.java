package de.danoeh.antennapod.util;

import android.util.Log;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.feed.Chapter;
import de.danoeh.antennapod.feed.MP4Chapter;
import de.danoeh.antennapod.util.comparator.ChapterStartTimeComparator;
import de.danoeh.antennapod.util.id3reader.ChapterReader;
import de.danoeh.antennapod.util.id3reader.ID3ReaderException;
import de.danoeh.antennapod.util.playback.Playable;
import de.danoeh.antennapod.util.vorbiscommentreader.VorbisCommentChapterReader;
import de.danoeh.antennapod.util.vorbiscommentreader.VorbisCommentReaderException;
import wseemann.media.FFmpegChapter;
import wseemann.media.FFmpegMediaMetadataRetriever;

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

    private static void readMP4ChaptersFromFileUrl(Playable p) {
        if (!FFmpegMediaMetadataRetriever.LIB_AVAILABLE) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "FFmpegMediaMetadataRetriever not available on this architecture");
            return;
        }
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Trying to read mp4 chapters from file " + p.getEpisodeTitle());

        FFmpegMediaMetadataRetriever retriever = new FFmpegMediaMetadataRetriever();
        retriever.setDataSource(p.getLocalMediaUrl());
        FFmpegChapter[] res = retriever.getChapters();
        retriever.release();
        if (res != null) {
            List<Chapter> chapters = new ArrayList<Chapter>();
            for (FFmpegChapter fFmpegChapter : res) {
                chapters.add(new MP4Chapter(fFmpegChapter));
            }
            Collections.sort(chapters, new ChapterStartTimeComparator());
            processChapters(chapters, p);
            p.setChapters(chapters);
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, "No mp4 chapters found in " + p.getEpisodeTitle());
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
            if (media.getChapters() == null && isMP4File(media)) {
                ChapterUtils.readMP4ChaptersFromFileUrl(media);
            }
        } else {
            Log.e(TAG, "Could not load chapters from file url: local file not available");
        }
    }

    private static boolean isMP4File(Playable media) {
        String ext = FilenameUtils.getExtension(media.getLocalMediaUrl());
        return StringUtils.equals(ext, "m4a") || StringUtils.equals(ext, "mp4")
                || StringUtils.equals(ext, "aac") || StringUtils.equals(ext, "m4p");
    }
}
