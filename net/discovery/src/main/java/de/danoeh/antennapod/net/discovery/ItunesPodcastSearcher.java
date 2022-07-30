package de.danoeh.antennapod.net.discovery;

import de.danoeh.antennapod.core.feed.FeedUrlNotFoundException;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItunesPodcastSearcher implements PodcastSearcher {
    private static final String ITUNES_API_URL = "https://itunes.apple.com/search?media=podcast&term=%s";
    private static final String PATTERN_BY_ID = ".*/podcasts\\.apple\\.com/.*/podcast/.*/id(\\d+).*";

    public ItunesPodcastSearcher() {
    }

    @Override
    public Single<List<PodcastSearchResult>> search(String query) {
        return Single.create((SingleOnSubscribe<List<PodcastSearchResult>>) subscriber -> {
            String encodedQuery;
            try {
                encodedQuery = URLEncoder.encode(query, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // this won't ever be thrown
                encodedQuery = query;
            }

            String formattedUrl = String.format(ITUNES_API_URL, encodedQuery);

            OkHttpClient client = AntennapodHttpClient.getHttpClient();
            Request.Builder httpReq = new Request.Builder()
                    .url(formattedUrl);
            List<PodcastSearchResult> podcasts = new ArrayList<>();
            try {
                Response response = client.newCall(httpReq.build()).execute();

                if (response.isSuccessful()) {
                    String resultString = response.body().string();
                    JSONObject result = new JSONObject(resultString);
                    JSONArray j = result.getJSONArray("results");

                    for (int i = 0; i < j.length(); i++) {
                        JSONObject podcastJson = j.getJSONObject(i);
                        PodcastSearchResult podcast = PodcastSearchResult.fromItunes(podcastJson);
                        if (podcast.feedUrl != null) {
                            podcasts.add(podcast);
                        }
                    }
                } else {
                    subscriber.onError(new IOException(response.toString()));
                }
            } catch (IOException | JSONException e) {
                subscriber.onError(e);
            }
            subscriber.onSuccess(podcasts);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<String> lookupUrl(String url) {
        Pattern pattern = Pattern.compile(PATTERN_BY_ID);
        Matcher matcher = pattern.matcher(url);
        final String lookupUrl = matcher.find() ? ("https://itunes.apple.com/lookup?id=" + matcher.group(1)) : url;
        return Single.create(emitter -> {
            OkHttpClient client = AntennapodHttpClient.getHttpClient();
            Request.Builder httpReq = new Request.Builder().url(lookupUrl);
            try {
                Response response = client.newCall(httpReq.build()).execute();
                if (response.isSuccessful()) {
                    String resultString = response.body().string();
                    JSONObject result = new JSONObject(resultString);
                    JSONObject results = result.getJSONArray("results").getJSONObject(0);
                    String feedUrlName = "feedUrl";
                    if (!results.has(feedUrlName)) {
                        String artistName = results.getString("artistName");
                        String trackName = results.getString("trackName");
                        emitter.onError(new FeedUrlNotFoundException(artistName, trackName));
                        return;
                    }
                    String feedUrl = results.getString(feedUrlName);
                    emitter.onSuccess(feedUrl);
                } else {
                    emitter.onError(new IOException(response.toString()));
                }
            } catch (IOException | JSONException e) {
                emitter.onError(e);
            }
        });
    }

    @Override
    public boolean urlNeedsLookup(String url) {
        return url.contains("itunes.apple.com") || url.matches(PATTERN_BY_ID);
    }

    @Override
    public String getName() {
        return "iTunes";
    }
}
