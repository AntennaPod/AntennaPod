package de.danoeh.antennapod.core.util;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.core.feed.ChapterMerger;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.comparator.ChapterStartTimeComparator;
import de.danoeh.antennapod.parser.feed.PodcastIndexChapterParser;
import de.danoeh.antennapod.parser.media.id3.ChapterReader;
import de.danoeh.antennapod.parser.media.id3.ID3ReaderException;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.parser.media.vorbis.VorbisCommentChapterReader;
import de.danoeh.antennapod.parser.media.vorbis.VorbisCommentReaderException;
import okhttp3.CacheControl;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.input.CountingInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

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

    public static void loadChapters(Playable playable, Context context) {
        if (playable.getChapters() != null) {
            // Already loaded
            return;
        }

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
                        feedMedia.getItem().getPodcastIndexChapterUrl());
            }

        }

        List<Chapter> chaptersFromMediaFile = ChapterUtils.loadChaptersFromMediaFile(playable, context);
        List<Chapter> chaptersMergePhase1 = ChapterMerger.merge(chaptersFromDatabase, chaptersFromMediaFile);
        List<Chapter> chapters = ChapterMerger.merge(chaptersMergePhase1, chaptersFromPodcastIndex);
        if (chapters == null) {
            // Do not try loading again. There are no chapters.
            playable.setChapters(Collections.emptyList());
        } else {
            playable.setChapters(chapters);
        }
    }

    public static List<Chapter> loadChaptersFromMediaFile(Playable playable, Context context) {
        try (CountingInputStream in = openStream(playable, context)) {
            List<Chapter> chapters = readId3ChaptersFrom(in);
            if (!chapters.isEmpty()) {
                Log.i(TAG, "Chapters loaded");
                return chapters;
            }
        } catch (IOException | ID3ReaderException e) {
            Log.e(TAG, "Unable to load ID3 chapters: " + e.getMessage());
        }

        try (CountingInputStream in = openStream(playable, context)) {
            List<Chapter> chapters = readOggChaptersFromInputStream(in);
            if (!chapters.isEmpty()) {
                Log.i(TAG, "Chapters loaded");
                return chapters;
            }
        } catch (IOException | VorbisCommentReaderException e) {
            Log.e(TAG, "Unable to load vorbis chapters: " + e.getMessage());
        }
        return null;
    }

    private static CountingInputStream openStream(Playable playable, Context context) throws IOException {
        if (playable.localFileAvailable()) {
            if (playable.getLocalMediaUrl() == null) {
                throw new IOException("No local url");
            }
            File source = new File(playable.getLocalMediaUrl());
            if (!source.exists()) {
                throw new IOException("Local file does not exist");
            }
            return new CountingInputStream(new FileInputStream(source));
        } else if (playable.getStreamUrl().startsWith(ContentResolver.SCHEME_CONTENT)) {
            Uri uri = Uri.parse(playable.getStreamUrl());
            return new CountingInputStream(context.getContentResolver().openInputStream(uri));
        } else {
            Request request = new Request.Builder().url(playable.getStreamUrl()).build();
            Response response = AntennapodHttpClient.getHttpClient().newCall(request).execute();
            if (response.body() == null) {
                throw new IOException("Body is null");
            }
            return new CountingInputStream(response.body().byteStream());
        }
    }

    public static List<Chapter> loadChaptersFromUrl(String url) {
        try {
            Request request = new Request.Builder().url(url).cacheControl(CacheControl.FORCE_CACHE).build();
            Response response = AntennapodHttpClient.getHttpClient().newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                List<Chapter> chapters = PodcastIndexChapterParser.parse(response.body().string());
                if (chapters != null && !chapters.isEmpty()) {
                    return chapters;
                }
            }
            request = new Request.Builder().url(url).build();
            response = AntennapodHttpClient.getHttpClient().newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                return PodcastIndexChapterParser.parse(response.body().string());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @NonNull
    private static List<Chapter> readId3ChaptersFrom(CountingInputStream in) throws IOException, ID3ReaderException {
        ChapterReader reader = new ChapterReader(in);
        reader.readInputStream();
        List<Chapter> chapters = reader.getChapters();
        Collections.sort(chapters, new ChapterStartTimeComparator());
        enumerateEmptyChapterTitles(chapters);
        if (!chaptersValid(chapters)) {
            Log.e(TAG, "Chapter data was invalid");
            return Collections.emptyList();
        }
        return chapters;
    }

    @NonNull
    private static List<Chapter> readOggChaptersFromInputStream(InputStream input) throws VorbisCommentReaderException {
        VorbisCommentChapterReader reader = new VorbisCommentChapterReader(input);
        reader.readInputStream();
        List<Chapter> chapters = reader.getChapters();
        if (chapters == null) {
            return Collections.emptyList();
        }
        Collections.sort(chapters, new ChapterStartTimeComparator());
        enumerateEmptyChapterTitles(chapters);
        if (chaptersValid(chapters)) {
            return chapters;
        }
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
}
