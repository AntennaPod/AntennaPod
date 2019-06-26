package de.danoeh.antennapod.discovery;

import android.content.Context;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
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

public class ItunesPodcastSearcher implements PodcastSearcher {
    private static final String ITUNES_API_URL = "https://itunes.apple.com/search?media=podcast&term=%s";
    private final Context context;
    private final String query;

    public ItunesPodcastSearcher(Context context, String query) {
        this.context = context;
        this.query = query;
    }

    public Disposable search(Consumer<? super List<PodcastSearchResult>> successHandler, Consumer<? super Throwable> errorHandler) {
        return Single.create((SingleOnSubscribe<List<PodcastSearchResult>>) subscriber -> {
            String encodedQuery = null;
            try {
                encodedQuery = URLEncoder.encode(query, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // this won't ever be thrown
            }
            if (encodedQuery == null) {
                encodedQuery = query; // failsafe
            }

            String formattedUrl = String.format(ITUNES_API_URL, encodedQuery);

            OkHttpClient client = AntennapodHttpClient.getHttpClient();
            Request.Builder httpReq = new Request.Builder()
                    .url(formattedUrl)
                    .header("User-Agent", ClientConfig.USER_AGENT);
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
                        podcasts.add(podcast);
                    }
                } else {
                    String prefix = context.getString(R.string.error_msg_prefix);
                    subscriber.onError(new IOException(prefix + response));
                }
            } catch (IOException | JSONException e) {
                subscriber.onError(e);
            }
            subscriber.onSuccess(podcasts);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(successHandler, errorHandler);
    }
}
