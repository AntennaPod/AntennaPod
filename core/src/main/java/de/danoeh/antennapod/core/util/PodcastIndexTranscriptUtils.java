package de.danoeh.antennapod.core.util;

import android.util.Log;

import java.io.IOException;

import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.parser.feed.PodcastIndexTranscriptParser;
import okhttp3.Request;
import okhttp3.Response;

public class PodcastIndexTranscriptUtils {

    private static final String TAG = "PodcastIndexTranscript";

    public static Transcript loadTranscriptFromUrl(String url, String type, boolean forceRefresh) {
        Response response = null;
        Transcript transcript = null;
        try {
            // TT TODO, what to do with cachingx
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
        String type = null;
        String url = null;
        if (media.getItem().getPodcastIndexTranscriptUrls("application/json") != null) {
            type = "application/json";
            url = media.getItem().getPodcastIndexTranscriptUrls("application/json");

        } else if (media.getItem().getPodcastIndexTranscriptUrls("application/srt") != null) {
            // TT TODO: how about application/x-subrip
            type = "application/srt";
            url = media.getItem().getPodcastIndexTranscriptUrls(type);
        }

        if (media.getItem().getPodcastIndexTranscriptText() != null) {
            return PodcastIndexTranscriptParser.parse(media.getItem().getPodcastIndexTranscriptText(), type);
        }

        // TODO: Store the transcript somewhere? in the DB of file system?
        if (url != null) {
            Log.d(TAG, "Loading Transcript URL " + url);
            return PodcastIndexTranscriptUtils.loadTranscriptFromUrl(url, type, false);
        }
        return null;
    }
}
