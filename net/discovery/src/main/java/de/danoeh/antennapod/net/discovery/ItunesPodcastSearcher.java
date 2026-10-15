package de.danoeh.antennapod.net.discovery;

import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItunesPodcastSearcher implements PodcastSearcher {
    private static final String ITUNES_API_URL = "https://itunes.apple.com/search?media=podcast&term=%s";
    private static final String ITUNES_EPISODES_API_URL
            = "https://itunes.apple.com/search?media=podcast&entity=podcastEpisode&term=%s";
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
                        if (!podcastJson.has("feedUrl")) {
                            continue;
                        }
                        String title = podcastJson.optString("collectionName", "Unknown");
                        String imageUrl = podcastJson.optString("artworkUrl100", "");
                        String feedUrl = podcastJson.optString("feedUrl", "");
                        String author = podcastJson.optString("artistName", "Unknown");
                        podcasts.add(new PodcastSearchResult(title, imageUrl, feedUrl, author));
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
    public Single<List<EpisodeSearchResult>> searchEpisodes(String query) {
        return Single.create((SingleOnSubscribe<List<EpisodeSearchResult>>) subscriber -> {
            String encodedQuery;
            try {
                encodedQuery = URLEncoder.encode(query, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                encodedQuery = query;
            }

            String formattedUrl = String.format(ITUNES_EPISODES_API_URL, encodedQuery);

            OkHttpClient client = AntennapodHttpClient.getHttpClient();
            Request.Builder httpReq = new Request.Builder()
                    .url(formattedUrl);
            List<EpisodeSearchResult> episodes = new ArrayList<>();
            try {
                Response response = client.newCall(httpReq.build()).execute();

                if (response.isSuccessful()) {
                    String resultString = response.body().string();
                    JSONObject result = new JSONObject(resultString);
                    JSONArray j = result.getJSONArray("results");

                    for (int i = 0; i < j.length(); i++) {
                        JSONObject episodeJson = j.getJSONObject(i);
                        String feedUrl = episodeJson.optString("feedUrl", null);
                        String episodeUrl = episodeJson.optString("episodeUrl", null);
                        if (episodeUrl == null || episodeUrl.isEmpty()) {
                            episodeUrl = episodeJson.optString("previewUrl", null);
                        }
                        if (feedUrl == null || feedUrl.isEmpty()
                                || episodeUrl == null || episodeUrl.isEmpty()) {
                            continue;
                        }

                        String imageUrl = episodeJson.optString("artworkUrl600", null);
                        if (imageUrl == null || imageUrl.isEmpty()) {
                            imageUrl = episodeJson.optString("artworkUrl160", null);
                        }
                        if (imageUrl == null || imageUrl.isEmpty()) {
                            imageUrl = episodeJson.optString("artworkUrl60", null);
                        }
                        String episodeGuid = episodeJson.optString("episodeGuid", null);
                        long durationMs = episodeJson.optLong("trackTimeMillis", 0);
                        String description = episodeJson.optString("description", null);
                        if (description == null || description.isEmpty()) {
                            description = episodeJson.optString("shortDescription", null);
                        }
                        String author = episodeJson.optString("artistName", null);

                        Date pubDate = null;
                        String releaseDateStr = episodeJson.optString("releaseDate", null);
                        if (releaseDateStr != null && !releaseDateStr.isEmpty()) {
                            try {
                                SimpleDateFormat format =
                                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                                pubDate = format.parse(releaseDateStr);
                            } catch (Exception ignored) {
                                ignored.printStackTrace();
                            }
                        }

                        final String title = episodeJson.optString("trackName", "Unknown");
                        final String podcastTitle = episodeJson.optString("collectionName", "Unknown");

                        episodes.add(new EpisodeSearchResult(title, podcastTitle, imageUrl, feedUrl,
                                episodeUrl, episodeGuid, pubDate, durationMs, description, author));
                    }
                } else {
                    subscriber.onError(new IOException(response.toString()));
                }
            } catch (IOException | JSONException e) {
                subscriber.onError(e);
            }
            subscriber.onSuccess(episodes);
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
        return "Apple";
    }
}
