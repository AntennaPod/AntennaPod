package de.danoeh.antennapod.core.util;

import java.io.IOException;
import java.util.List;

import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.parser.feed.PodcastIndexChapterParser;
import de.danoeh.antennapod.parser.feed.PodcastIndexTranscriptParser;
import okhttp3.CacheControl;
import okhttp3.Request;
import okhttp3.Response;

public class PodcastIndexTranscriptUtils {
    private static final String TAG = "PodcastIndexTranscriptUtils";
    public static List<Transcript> loadTranscriptFromURL(String url, String type, boolean forceRefresh) {
        Response response = null;
        try {
            // TT TODO, what to do with caching
            Request request = new Request.Builder().url(url).cacheControl(CacheControl.FORCE_CACHE).build();
            response = AntennapodHttpClient.getHttpClient().newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                return PodcastIndexTranscriptParser.parse(response.body().string(), type);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }

    public static void loadTranscriptFromURL(String podcastIndexTranscriptUrl, boolean b) {
    }
}
