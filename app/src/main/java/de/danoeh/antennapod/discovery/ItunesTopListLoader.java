package de.danoeh.antennapod.discovery;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
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

import static android.content.Context.MODE_PRIVATE;

public class ItunesTopListLoader {
    private static final String TAG = "ITunesTopListLoader";
    private final Context context;
    public static final String PREF_KEY_COUNTRY_CODE = "country_code";
    public static final String PREFS = "CountryRegionPrefs";

    public ItunesTopListLoader(Context context) {

        this.context = context;
    }

    public Single<List<PodcastSearchResult>> loadToplist() {
        String defaultCountry= Locale.getDefault().getCountry();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        String country_code = prefs.getString(PREF_KEY_COUNTRY_CODE, defaultCountry);
        return this.loadToplist(country_code, 25);
    }
    public Single<List<PodcastSearchResult>> loadToplist(String country, int limit) {
        return Single.create((SingleOnSubscribe<List<PodcastSearchResult>>) emitter -> {
            OkHttpClient client = AntennapodHttpClient.getHttpClient();
            String feedString;
            try {
                feedString = getTopListFeed(client, country, limit);
            } catch (IOException e) {
                feedString = getTopListFeed(client, "us", limit);
            }
            List<PodcastSearchResult> podcasts = parseFeed(feedString);
            emitter.onSuccess(podcasts);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private String getTopListFeed(OkHttpClient client, String country, int limit) throws IOException {
        String url = "https://itunes.apple.com/%s/rss/toppodcasts/limit=" + limit + "/explicit=true/json";
        Log.d(TAG, "Feed URL " + String.format(url, country));
        Request.Builder httpReq = new Request.Builder()
                .cacheControl(new CacheControl.Builder().maxStale(1, TimeUnit.DAYS).build())
                .url(String.format(url, country));

        try (Response response = client.newCall(httpReq.build()).execute()) {
            if (response.isSuccessful()) {
                return response.body().string();
            }
            String prefix = context.getString(R.string.error_msg_prefix);
            throw new IOException(prefix + response);
        }
    }

    private List<PodcastSearchResult> parseFeed(String jsonString) throws JSONException {
        JSONObject result = new JSONObject(jsonString);
        JSONObject feed = result.getJSONObject("feed");
        JSONArray entries = feed.getJSONArray("entry");

        List<PodcastSearchResult> results = new ArrayList<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject json = entries.getJSONObject(i);
            results.add(PodcastSearchResult.fromItunesToplist(json));
        }

        return results;
    }

}
