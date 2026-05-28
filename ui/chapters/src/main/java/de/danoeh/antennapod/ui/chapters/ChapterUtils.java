package de.danoeh.antennapod.ui.chapters;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import de.danoeh.antennapod.parser.media.MediaFormatDetector;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.parser.feed.PodcastIndexChapterParser;
import de.danoeh.antennapod.parser.media.id3.ChapterReader;
import de.danoeh.antennapod.parser.media.id3.ID3ReaderException;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.parser.media.vorbis.VorbisCommentChapterReader;
import de.danoeh.antennapod.parser.media.vorbis.VorbisCommentReaderException;
import de.danoeh.antennapod.parser.media.m4a.M4AChapterReader;
import okhttp3.CacheControl;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.input.CountingInputStream;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.SequenceInputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for getting chapter data from media files.
 */
public class ChapterUtils {

    private static final String TAG = "ChapterUtils";

    private ChapterUtils() {
    }

    public static void loadChapters(Playable playable, Context context, boolean forceRefresh) {
        if (playable.getChapters() != null && !forceRefresh) {
            // Already loaded
            return;
        }

        try {
            List<Chapter> chaptersFromDatabase = null;
            List<Chapter> chaptersFromPodcastIndex = null;
            if (playable instanceof FeedMedia) {
                FeedMedia feedMedia = (FeedMedia) playable;
                if (feedMedia.getItem() == null) {
                    feedMedia.setItem(DBReader.getFeedItem(feedMedia.getItemId()));
                }
                if (feedMedia.getItem().hasChapters()) {
                    chaptersFromDatabase = DBReader.loadChaptersOfFeedItem(feedMedia.getItem());
                }

                if (!TextUtils.isEmpty(feedMedia.getItem().getPodcastIndexChapterUrl())) {
                    chaptersFromPodcastIndex = ChapterUtils.loadChaptersFromUrl(
                            feedMedia.getItem().getPodcastIndexChapterUrl(), forceRefresh);
                }
            }

            List<Chapter> chaptersFromMediaFile = ChapterUtils.loadChaptersFromMediaFile(playable, context);
            List<Chapter> chaptersMergePhase1 = ChapterMerger.merge(chaptersFromDatabase, chaptersFromMediaFile);
            List<Chapter> chapters = ChapterMerger.merge(chaptersMergePhase1, chaptersFromPodcastIndex);
            if (chapters == null) {
                // Do not try loading again. There are no chapters or parsing failed.
                playable.setChapters(Collections.emptyList());
            } else {
                playable.setChapters(chapters);
            }
        } catch (InterruptedIOException e) {
            Log.d(TAG, "Chapter loading interrupted");
            playable.setChapters(null); // Allow later retry
        }
    }

    public static List<Chapter> loadChaptersFromMediaFile(Playable playable, Context context)
            throws InterruptedIOException {
        // Load the first few bytes to detect the format.
        // Then stitch the format back onto the stream and pass it to the chapter reader.
        // If we were unable to detect the format, we stitch and send it to the first reader,
        // then we try again with a fresh stream for the remaining two readers.
        // This reduces the number of times we have to open a new stream as much as possible.
        MediaFormatDetector.Format format = MediaFormatDetector.Format.UNKNOWN;
        MediaFormatDetector.Format hint = MediaFormatDetector.Format.UNKNOWN;
        try (CountingInputStream sniffStream = openStream(playable, context)) {
            MediaFormatDetector.Result detection = MediaFormatDetector.detect(sniffStream);
            format = detection.format;
            if (format == MediaFormatDetector.Format.UNKNOWN) {
                hint = detectHintFromMetadata(
                        ((FeedMedia) playable).getMimeType(), playable.getStreamUrl());
            }
            InputStream reconstructed = new SequenceInputStream(
                    new ByteArrayInputStream(detection.bytes), sniffStream);
            if (format != MediaFormatDetector.Format.UNKNOWN) {
                CountingInputStream input = new CountingInputStream(reconstructed);
                List<Chapter> chapters = readChaptersFromInputStream(input, format);
                hasLoadedChapters(chapters);
                return chapters;
            } else if (hint == MediaFormatDetector.Format.ID3
                    || hint == MediaFormatDetector.Format.UNKNOWN) {
                List<Chapter> chapters = readId3ChaptersFrom(new CountingInputStream(reconstructed));
                if (hasLoadedChapters(chapters)) {
                    return chapters;
                }
            } else if (hint == MediaFormatDetector.Format.OGG) {
                List<Chapter> chapters = readOggChaptersFromInputStream(
                        new CountingInputStream(reconstructed));
                if (hasLoadedChapters(chapters)) {
                    return chapters;
                }
            } else {
                List<Chapter> chapters = readM4AChaptersFromInputStream(
                        new CountingInputStream(reconstructed));
                if (hasLoadedChapters(chapters)) {
                    return chapters;
                }
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException | ID3ReaderException | VorbisCommentReaderException e) {
            Log.e(TAG, "Unable to load chapters: " + e.getMessage());
        }

        if (format == MediaFormatDetector.Format.UNKNOWN) {
            for (MediaFormatDetector.Format fallbackFormat : getFallbackOrder(hint)) {
                List<Chapter> chapters = tryFreshStreamParser(playable, context, fallbackFormat);
                if (hasLoadedChapters(chapters)) {
                    return chapters;
                }
            }
        }
        return null;
    }

    static MediaFormatDetector.Format detectHintFromMetadata(String mime, String url) {
        MediaFormatDetector.Format format = MediaFormatDetector.Format.UNKNOWN;
        if (mime != null) {
            format = switch (mime.trim().toLowerCase(Locale.US)) {
                case "audio/mpeg", "audio/mp3", "audio/x-mp3"
                        -> MediaFormatDetector.Format.ID3;
                case "audio/ogg", "application/ogg", "audio/opus", "application/opus"
                        -> MediaFormatDetector.Format.OGG;
                case "audio/mp4", "audio/x-m4a", "audio/m4a", "video/mp4", "audio/x-m4b", "audio/m4b"
                        -> MediaFormatDetector.Format.M4A;
                default -> format;
            };
        }

        if (format == MediaFormatDetector.Format.UNKNOWN && url != null) {
            String filename = URLUtil.guessFileName(url, null, mime);
            if (!TextUtils.isEmpty(filename)) {
                int dot = filename.lastIndexOf('.');
                if (dot != -1 && dot < filename.length() - 1) {
                    String ext = filename.substring(dot + 1).toLowerCase(Locale.US);
                    format = switch (ext) {
                        case "mp3" -> MediaFormatDetector.Format.ID3;
                        case "ogg", "opus" -> MediaFormatDetector.Format.OGG;
                        case "m4a", "mp4", "m4b" -> MediaFormatDetector.Format.M4A;
                        default -> format;
                    };
                }
            }
        }
        return format;
    }

    static MediaFormatDetector.Format[] getFallbackOrder(MediaFormatDetector.Format hint) {
        return switch (hint) {
            case OGG -> new MediaFormatDetector.Format[]{
                    MediaFormatDetector.Format.ID3, MediaFormatDetector.Format.M4A};
            case M4A -> new MediaFormatDetector.Format[]{
                    MediaFormatDetector.Format.ID3, MediaFormatDetector.Format.OGG};
            default -> new MediaFormatDetector.Format[]{
                    MediaFormatDetector.Format.OGG, MediaFormatDetector.Format.M4A};
        };
    }

    private static List<Chapter> readChaptersFromInputStream(
            CountingInputStream input, MediaFormatDetector.Format format)
            throws IOException, ID3ReaderException, VorbisCommentReaderException {
        return switch (format) {
            case ID3 -> readId3ChaptersFrom(input);
            case OGG -> readOggChaptersFromInputStream(input);
            case M4A -> readM4AChaptersFromInputStream(input);
            default -> Collections.emptyList();
        };
    }

    private static boolean hasLoadedChapters(List<Chapter> chapters) {
        if (chapters != null && !chapters.isEmpty()) {
            Log.i(TAG, "Chapters loaded");
            return true;
        }
        return false;
    }

    private static List<Chapter> tryFreshStreamParser(Playable playable, Context context,
                                                      MediaFormatDetector.Format format) throws InterruptedIOException {
        try (CountingInputStream in = openStream(playable, context)) {
            return readChaptersFromInputStream(in, format);
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException | ID3ReaderException | VorbisCommentReaderException e) {
            Log.e(TAG, "Unable to load chapters (" + format + "): " + e.getMessage());
            return null;
        }
    }

    private static CountingInputStream openStream(Playable playable, Context context) throws IOException {
        if (playable.localFileAvailable()) {
            if (playable.getLocalFileUrl() == null) {
                throw new IOException("No local url");
            }
            File source = new File(playable.getLocalFileUrl());
            if (!source.exists()) {
                throw new IOException("Local file does not exist");
            }
            return new CountingInputStream(new BufferedInputStream(new FileInputStream(source)));
        } else if (playable.getStreamUrl().startsWith(ContentResolver.SCHEME_CONTENT)) {
            Uri uri = Uri.parse(playable.getStreamUrl());
            return new CountingInputStream(new BufferedInputStream(context.getContentResolver().openInputStream(uri)));
        } else {
            Request request = new Request.Builder().url(playable.getStreamUrl()).build();
            Response response = AntennapodHttpClient.getHttpClient().newCall(request).execute();
            if (response.body() == null) {
                throw new IOException("Body is null");
            }
            return new CountingInputStream(new BufferedInputStream(response.body().byteStream()));
        }
    }

    public static List<Chapter> loadChaptersFromUrl(String url, boolean forceRefresh) throws InterruptedIOException {
        if (forceRefresh) {
            return loadChaptersFromUrl(url, CacheControl.FORCE_NETWORK);
        }
        List<Chapter> cachedChapters = loadChaptersFromUrl(url, CacheControl.FORCE_CACHE);
        if (cachedChapters == null || cachedChapters.size() <= 1) {
            // Some publishers use one dummy chapter before actual chapters are available
            return loadChaptersFromUrl(url, CacheControl.FORCE_NETWORK);
        }
        return cachedChapters;
    }

    private static List<Chapter> loadChaptersFromUrl(String url, CacheControl cacheControl)
            throws InterruptedIOException {
        Response response = null;
        try {
            Request request = new Request.Builder().url(url).cacheControl(cacheControl).build();
            response = AntennapodHttpClient.getHttpClient().newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                return PodcastIndexChapterParser.parse(response.body().string());
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.d(TAG, "Failed to load chapters from URL: " + url, e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }

    @NonNull
    private static List<Chapter> readId3ChaptersFrom(CountingInputStream in) throws IOException, ID3ReaderException {
        ChapterReader reader = new ChapterReader(in);
        reader.readInputStream();
        List<Chapter> chapters = reader.getChapters();
        return processChapters(chapters);
    }

    @NonNull
    private static List<Chapter> readOggChaptersFromInputStream(InputStream input) throws VorbisCommentReaderException {
        VorbisCommentChapterReader reader = new VorbisCommentChapterReader(new BufferedInputStream(input));
        reader.readInputStream();
        List<Chapter> chapters = reader.getChapters();
        return processChapters(chapters);
    }

    @NonNull
    private static List<Chapter> readM4AChaptersFromInputStream(InputStream input) {
        M4AChapterReader reader = new M4AChapterReader(new BufferedInputStream(input));
        reader.readInputStream();
        List<Chapter> chapters = reader.getChapters();
        return processChapters(chapters);
    }

    private static List<Chapter> processChapters(List<Chapter> chapters) {
        if (chapters == null) {
            return Collections.emptyList();
        }
        Collections.sort(chapters, new ChapterStartTimeComparator());
        enumerateEmptyChapterTitles(chapters);
        if (chaptersValid(chapters)) {
            return chapters;
        }
        Log.e(TAG, "Chapter data was invalid");
        return Collections.emptyList();
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

    public static class ChapterStartTimeComparator implements Comparator<Chapter> {
        @Override
        public int compare(Chapter lhs, Chapter rhs) {
            return Long.compare(lhs.getStart(), rhs.getStart());
        }
    }
}
