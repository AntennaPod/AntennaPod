package de.danoeh.antennapod.core.util;

import java.io.IOException;
import java.util.List;

import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.model.feed.FeedMedia;
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
        List<Transcript> transcripts = null;
        try {
            // TT TODO, what to do with cachingx
            Request request = new Request.Builder().url(url).build();
            response = AntennapodHttpClient.getHttpClient().newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                transcripts = PodcastIndexTranscriptParser.parse(response.body().string(), type);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
                return transcripts;
            }
        }
        return transcripts;
    }

    public static List<Transcript> loadTranscript(FeedMedia media) {
        String type = null;
        String url = null;
        if (media.getItem().getPodcastIndexTranscriptUrl("application/jsonxx") != null) {
            type = "application/json";
            url = media.getItem().getPodcastIndexTranscriptUrl("application/json");
        } else if (media.getItem().getPodcastIndexTranscriptUrl("application/srt") != null) {
            type = "application/srt";
            url = media.getItem().getPodcastIndexTranscriptUrl(type);
        }
        // TODO: Store the transcript somewhere? in the DB of file system?
        if (url != null) {
            return PodcastIndexTranscriptUtils.loadTranscriptFromURL(url, type, false);
        }
        return null;
    }
    public static void loadTranscriptFromURL(String podcastIndexTranscriptUrl, boolean b) {
    }
}
