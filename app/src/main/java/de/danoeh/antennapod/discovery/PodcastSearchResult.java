package de.danoeh.antennapod.discovery;

import androidx.annotation.Nullable;
import de.danoeh.antennapod.core.sync.gpoddernet.model.GpodnetPodcast;
import de.mfietz.fyydlin.SearchHit;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PodcastSearchResult {

    /**
     * The name of the podcast
     */
    public final String title;

    /**
     * URL of the podcast image
     */
    @Nullable
    public final String imageUrl;
    /**
     * URL of the podcast feed
     */
    @Nullable
    public final String feedUrl;

    /**
     * artistName of the podcast feed
     */
    @Nullable
    public final String author;

    @Nullable
    public PodcastSearcher searcher;

    private PodcastSearchResult(String title, @Nullable String imageUrl, @Nullable String feedUrl, @Nullable String author) {
        this(title, imageUrl, feedUrl, author, null);
    }

    private PodcastSearchResult(String title, @Nullable String imageUrl, @Nullable String feedUrl, @Nullable String author, @Nullable PodcastSearcher searcher) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.feedUrl = feedUrl;
        this.author = author;
        this.searcher = searcher;
    }

    private PodcastSearchResult(String title, @Nullable String imageUrl, @Nullable String feedUrl) {
        this(title, imageUrl, feedUrl, "", null);
    }

    public static PodcastSearchResult dummy() {
        return new PodcastSearchResult("", "", "", "", null);
    }

    public PodcastSearcher getSearcher() {
        return searcher;
    }

    /**
     * Constructs a Podcast instance from a iTunes search result
     *
     * @param json object holding the podcast information
     * @throws JSONException
     */
    public static PodcastSearchResult fromItunes(JSONObject json, ItunesPodcastSearcher searcher) {
        String title = json.optString("collectionName", "");
        String imageUrl = json.optString("artworkUrl100", null);
        String feedUrl = json.optString("feedUrl", null);
        String author = json.optString("artistName", null);
        return new PodcastSearchResult(title, imageUrl, feedUrl, author, searcher);
    }

    /**
     * Constructs a Podcast instance from iTunes toplist entry
     *
     * @param json object holding the podcast information
     * @throws JSONException
     */
    public static PodcastSearchResult fromItunesToplist(JSONObject json, ItunesTopListLoader searcher) throws JSONException {
        String title = json.getJSONObject("title").getString("label");
        String imageUrl = null;
        JSONArray images =  json.getJSONArray("im:image");
        for(int i=0; imageUrl == null && i < images.length(); i++) {
            JSONObject image = images.getJSONObject(i);
            String height = image.getJSONObject("attributes").getString("height");
            if(Integer.parseInt(height) >= 100) {
                imageUrl = image.getString("label");
            }
        }
        String feedUrl = "https://itunes.apple.com/lookup?id=" +
                json.getJSONObject("id").getJSONObject("attributes").getString("im:id");

        String author = null;
        try {
            author = json.getJSONObject("im:artist").getString("label");
        } catch (Exception e) {
            // Some feeds have empty artist
        }
        return new PodcastSearchResult(title, imageUrl, feedUrl, author, searcher);
    }

    public static PodcastSearchResult fromFyyd(SearchHit searchHit, FyydPodcastSearcher searcher) {
        return new PodcastSearchResult(searchHit.getTitle(),
                                       searchHit.getThumbImageURL(),
                                       searchHit.getXmlUrl(),
                                       searchHit.getAuthor(),
                                       searcher);
    }

    public static PodcastSearchResult fromGpodder(GpodnetPodcast searchHit, GpodnetPodcastSearcher searcher) {
        return new PodcastSearchResult(searchHit.getTitle(),
                                       searchHit.getLogoUrl(),
                                       searchHit.getUrl(),
                                       searchHit.getAuthor(),
                                       searcher);
    }

    public static PodcastSearchResult fromPodcastIndex(JSONObject json, PodcastIndexPodcastSearcher searcher) {
        String title = json.optString("title", "");
        String imageUrl = json.optString("image", null);
        String feedUrl = json.optString("url", null);
        String author = json.optString("author", null);
        return new PodcastSearchResult(title, imageUrl, feedUrl, author, searcher);
    }
}
