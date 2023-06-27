package de.danoeh.antennapod.net.discovery;

import android.content.Context;
import android.util.Log;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.model.feed.Feed;
import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ItunesTopListLoader {
    private static final String TAG = "ITunesTopListLoader";
    private final Context context;
    public static final String PREF_KEY_COUNTRY_CODE = "country_code";
    public static final String PREF_KEY_HIDDEN_DISCOVERY_COUNTRY = "hidden_discovery_country";
    public static final String PREF_KEY_NEEDS_CONFIRM = "needs_confirm";
    public static final String PREFS = "CountryRegionPrefs";
    public static final String COUNTRY_CODE_UNSET = "99";
    private static final int NUM_LOADED = 25;

    public ItunesTopListLoader(Context context) {
        this.context = context;
    }

    public List<PodcastSearchResult> loadToplist(String country, int limit, List<Feed> subscribed)
            throws JSONException, IOException {
        OkHttpClient client = AntennapodHttpClient.getHttpClient();
        String feedString;
        String loadCountry = country;
        if (COUNTRY_CODE_UNSET.equals(country)) {
            loadCountry = Locale.getDefault().getCountry();
        }
        try {
            feedString = getTopListFeed(client, loadCountry);
        } catch (IOException e) {
            if (COUNTRY_CODE_UNSET.equals(country)) {
                feedString = getTopListFeed(client, "US");
            } else {
                throw e;
            }
        }
        return removeSubscribed(parseFeed(feedString), subscribed, limit);
    }

    private static List<PodcastSearchResult> removeSubscribed(
            List<PodcastSearchResult> suggestedPodcasts, List<Feed> subscribedFeeds, int limit) {
        Set<String> subscribedPodcastsSet = new HashSet<>();
        for (Feed subscribedFeed : subscribedFeeds) {
            if (subscribedFeed.getTitle() != null && subscribedFeed.getAuthor() != null) {
                subscribedPodcastsSet.add(subscribedFeed.getTitle().trim() + " - " + subscribedFeed.getAuthor().trim());
            }
        }
        List<PodcastSearchResult> suggestedNotSubscribed = new ArrayList<>();
        for (PodcastSearchResult suggested : suggestedPodcasts) {
            if (!subscribedPodcastsSet.contains(suggested.title.trim())) {
                suggestedNotSubscribed.add(suggested);
            }
            if (suggestedNotSubscribed.size() == limit) {
                return suggestedNotSubscribed;
            }
        }
        return suggestedNotSubscribed;
    }

    private String getTopListFeed(OkHttpClient client, String country) throws IOException {
        String url = "https://itunes.apple.com/%s/rss/toppodcasts/limit=" + NUM_LOADED + "/explicit=true/json";
        Log.d(TAG, "Feed URL " + String.format(url, country));
        Request.Builder httpReq = new Request.Builder()
                .cacheControl(new CacheControl.Builder().maxStale(1, TimeUnit.DAYS).build())
                .url(String.format(url, country));

        try (Response response = client.newCall(httpReq.build()).execute()) {
            if (response.isSuccessful()) {
                return response.body().string();
            }
            if (response.code() == 400) {
                throw new IOException("iTunes does not have data for the selected country.");
            }
            String prefix = context.getString(R.string.error_msg_prefix);
            throw new IOException(prefix + response);
        }
    }

    private List<PodcastSearchResult> parseFeed(String jsonString) throws JSONException {
        JSONObject result = new JSONObject(jsonString);
        JSONObject feed;
        JSONArray entries;
        try {
            feed = result.getJSONObject("feed");
            entries = feed.getJSONArray("entry");
        } catch (JSONException e) {
            return new ArrayList<>();
        }

        List<PodcastSearchResult> results = new ArrayList<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject json = entries.getJSONObject(i);
            results.add(PodcastSearchResult.fromItunesToplist(json));
        }

        return results;
    }

}
