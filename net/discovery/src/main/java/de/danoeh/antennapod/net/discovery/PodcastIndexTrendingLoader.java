package de.danoeh.antennapod.net.discovery;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public abstract class PodcastIndexTrendingLoader {
    private static final String API_URL = "https://api.podcastindex.org/api/1.0/podcasts/trending?max=%d&lang=%s";
    private static final int NUM_LOADED = 25;

    public static List<PodcastSearchResult> loadTrending(String country, int limit, List<Feed> subscribed)
            throws JSONException, IOException {
        String formattedUrl = String.format(Locale.ROOT, API_URL, NUM_LOADED, "de");

        List<PodcastSearchResult> podcasts = new ArrayList<>();
        OkHttpClient client = AntennapodHttpClient.getHttpClient();
        Request.Builder builder = PodcastIndexApi.buildAuthenticatedRequest(formattedUrl);
        builder.cacheControl(new CacheControl.Builder().maxStale(1, TimeUnit.DAYS).build());
        Response response = client.newCall(builder.build()).execute();

        if (!response.isSuccessful()) {
            throw new IOException(response.toString());
        }

        String resultString = response.body().string();
        JSONObject result = new JSONObject(resultString);
        JSONArray j = result.getJSONArray("feeds");

        for (int i = 0; i < j.length(); i++) {
            JSONObject podcastJson = j.getJSONObject(i);
            PodcastSearchResult podcast = PodcastSearchResult.fromPodcastIndex(podcastJson);
            if (podcast.feedUrl == null) {
                continue;
            }
            boolean alreadySubscribed = false;
            for (Feed f : subscribed) {
                if (f.getState() != Feed.STATE_SUBSCRIBED) {
                    continue;
                }
                if (f.getDownloadUrl().equals(podcast.feedUrl) || f.getTitle().equals(podcast.title)) {
                    alreadySubscribed = true;
                    break;
                }
            }
            if (!alreadySubscribed) {
                podcasts.add(podcast);
                if (podcasts.size() == limit) {
                    return podcasts;
                }
            }
        }
        return podcasts;
    }
}
