package de.danoeh.antennapod.discovery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AudiothekProgramSetSearcher implements PodcastSearcher {
    private static final String API_URL = "https://api.ardaudiothek.de/search?query=%s";


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

            String formattedUrl = String.format(API_URL, encodedQuery);

            OkHttpClient client = AntennapodHttpClient.getHttpClient();
            Request.Builder httpReq = new Request.Builder()
                    .addHeader("User-Agent", ClientConfig.USER_AGENT)
                    .url(formattedUrl);
            List<PodcastSearchResult> podcasts = new ArrayList<>();
            try {
                Response response = client.newCall(httpReq.build()).execute();

                if (response.isSuccessful()) {
                    String resultString = response.body().string();
                    JSONObject result = new JSONObject(resultString);
                    JSONArray j = result.getJSONArray("_embedded");

                    for (int i = 0; i < j.length(); i++) {
                        JSONObject podcastJson = j.getJSONObject(i);
                        PodcastSearchResult podcast = PodcastSearchResult.fromPodcastIndex(podcastJson);
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

    public Single<String> lookupUrl(String url) {
        return Single.just(url);
    }


    @Override
    public boolean urlNeedsLookup(String resultUrl) {
        return false;
    }

    @Override
    public String getName() {
        return "ARD Audiothek Programset Searcher";
    }
}
