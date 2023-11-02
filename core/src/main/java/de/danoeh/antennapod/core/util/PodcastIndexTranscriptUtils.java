package de.danoeh.antennapod.core.util;

import android.util.Log;
import android.util.Pair;

import java.io.IOException;

import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.parser.feed.PodcastIndexTranscriptParser;
import okhttp3.Request;
import okhttp3.Response;

public class PodcastIndexTranscriptUtils {

    private static final String TAG = "PodcastIndexTranscript";

    public static Transcript loadTranscriptFromUrl(String type, String url, boolean forceRefresh) {
        Response response = null;
        Transcript transcript = null;
        try {
            // TT TODO, what to do with caching
            Request request = new Request.Builder().url(url).build();
            response = AntennapodHttpClient.getHttpClient().newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                transcript = PodcastIndexTranscriptParser.parse(response.body().string(), type);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            // TT TODO : NetworkOnMainThreadException
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
                return transcript;
            }
        }
        return transcript;
    }

    public static Transcript loadTranscript(FeedMedia media) {
        Pair<String, String> urlType = null;
        urlType = media.getItem().getPodcastIndexTranscriptUrlPreferred();

        if (media.getItem().getPodcastIndexTranscriptText() != null) {
            return PodcastIndexTranscriptParser.parse(media.getItem().getPodcastIndexTranscriptText(), urlType.first);
        }

        if (urlType != null) {
            return PodcastIndexTranscriptUtils.loadTranscriptFromUrl(urlType.first, urlType.second, false);
        }
        return null;
    }
}
