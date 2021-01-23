package de.danoeh.antennapod.discovery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PodcastIndexPodcastSearcher implements PodcastSearcher {
    private static final String PODCASTINDEX_API_URL = "https://api.podcastindex.org/api/1.0/search/byterm?q=%s";

    public PodcastIndexPodcastSearcher() {
    }

    @Override
    public Single<List<PodcastSearchResult>> search(String query) {
        return Single.create((SingleOnSubscribe<List<PodcastSearchResult>>) subscriber -> {

            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.clear();
            Date now = new Date();
            calendar.setTime(now);
            long secondsSinceEpoch = calendar.getTimeInMillis() / 1000L;
            String apiHeaderTime = String.valueOf(secondsSinceEpoch);
            String data4Hash = BuildConfig.PODCASTINDEX_API_KEY + BuildConfig.PODCASTINDEX_API_SECRET + apiHeaderTime;
            String hashString = sha1(data4Hash);

            String encodedQuery;
            try {
                encodedQuery = URLEncoder.encode(query, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // this won't ever be thrown
                encodedQuery = query;
            }

            String formattedUrl = String.format(PODCASTINDEX_API_URL, encodedQuery);

            OkHttpClient client = AntennapodHttpClient.getHttpClient();
            Request.Builder httpReq = new Request.Builder()
                    .addHeader("X-Auth-Date", apiHeaderTime)
                    .addHeader("X-Auth-Key", BuildConfig.PODCASTINDEX_API_KEY)
                    .addHeader("Authorization", hashString)
                    .addHeader("User-Agent", ClientConfig.USER_AGENT)
                    .url(formattedUrl);
            List<PodcastSearchResult> podcasts = new ArrayList<>();
            try {
                Response response = client.newCall(httpReq.build()).execute();

                if (response.isSuccessful()) {
                    String resultString = response.body().string();
                    JSONObject result = new JSONObject(resultString);
                    JSONArray j = result.getJSONArray("feeds");

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
        return "Podcastindex.org";
    }

    private static String sha1(String clearString) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(clearString.getBytes("UTF-8"));
            return toHex(messageDigest.digest());
        } catch (Exception ignored) {
            ignored.printStackTrace();
            return null;
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();
        for (byte b : bytes) {
            buffer.append(String.format(Locale.getDefault(), "%02x", b));
        }
        return buffer.toString();
    }
}
