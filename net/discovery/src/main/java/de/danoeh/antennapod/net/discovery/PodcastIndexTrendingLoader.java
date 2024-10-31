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
    public static final String PREF_KEY_LANGUAGE = "language";
    public static final String PREF_KEY_HIDDEN_DISCOVERY_COUNTRY = "hidden_discovery_country";
    public static final String PREF_KEY_NEEDS_CONFIRM = "needs_confirm";
    public static final String PREFS = "CountryRegionPrefs";
    public static final String LANGUAGE_UNSET = "99";
    private static final int NUM_LOADED = 25;

    public static List<PodcastSearchResult> loadTrending(String language, String categories,
                                         int limit, List<Feed> subscribed) throws JSONException, IOException {
        String loadLanguage = language;
        if (LANGUAGE_UNSET.equals(language)) {
            loadLanguage = Locale.getDefault().getCountry();
        }
        try {
            return doLoad(loadLanguage, categories, limit, subscribed);
        } catch (IOException e) {
            if (LANGUAGE_UNSET.equals(language)) {
                return doLoad("en", categories, limit, subscribed);
            } else {
                throw e;
            }
        }
    }

    private static List<PodcastSearchResult> doLoad(String language, String categories,
                                         int limit, List<Feed> subscribed) throws JSONException, IOException {
        String formattedUrl = "https://api.podcastindex.org/api/1.0/podcasts/trending"
                + "?max=" + NUM_LOADED
                + "&lang=" + language;
        if (categories != null) {
            formattedUrl += "&cat=" + categories;
        }

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
