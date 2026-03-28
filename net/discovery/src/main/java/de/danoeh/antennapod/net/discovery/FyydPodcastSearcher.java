package de.danoeh.antennapod.net.discovery;

import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
import io.reactivex.rxjava3.schedulers.Schedulers;
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

public class FyydPodcastSearcher implements PodcastSearcher {
    private static final String FYYD_API_URL = "https://api.fyyd.de/0.2/search/podcast?title=%s&count=10";

    public Single<List<PodcastSearchResult>> search(String query) {
        return Single.create((SingleOnSubscribe<List<PodcastSearchResult>>) subscriber -> {
            String encodedQuery;
            try {
                encodedQuery = URLEncoder.encode(query, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                encodedQuery = query;
            }
            String formattedUrl = String.format(FYYD_API_URL, encodedQuery);

            OkHttpClient client = AntennapodHttpClient.getHttpClient();
            Request.Builder httpReq = new Request.Builder().url(formattedUrl);
            ArrayList<PodcastSearchResult> searchResults = new ArrayList<>();
            try (Response response = client.newCall(httpReq.build()).execute()) {
                if (!response.isSuccessful()) {
                    subscriber.onError(new IOException(response.toString()));
                    return;
                }
                if (response.body() == null) {
                    subscriber.onError(new IOException("Null response"));
                    return;
                }
                String resultString = response.body().string();
                JSONObject result = new JSONObject(resultString);
                JSONArray data = result.optJSONArray("data");
                if (data == null) {
                    subscriber.onError(new IOException("Null response"));
                    return;
                }

                for (int i = 0; i < data.length(); i++) {
                    JSONObject podcastJson = data.getJSONObject(i);
                    String title = podcastJson.optString("title", "Unknown");
                    String imageUrl = podcastJson.optString("thumbImageURL", "");
                    String feedUrl = podcastJson.optString("xmlURL", "");
                    String author = podcastJson.optString("author", "Unknown");
                    searchResults.add(new PodcastSearchResult(title, imageUrl, feedUrl, author));
                }
            } catch (IOException | JSONException e) {
                subscriber.onError(e);
                return;
            }
            subscriber.onSuccess(searchResults);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<String> lookupUrl(String url) {
        return Single.just(url);
    }

    @Override
    public boolean urlNeedsLookup(String url) {
        return false;
    }

    @Override
    public String getName() {
        return "fyyd";
    }
}
