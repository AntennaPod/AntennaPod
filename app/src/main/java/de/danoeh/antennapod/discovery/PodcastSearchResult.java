package de.danoeh.antennapod.discovery;

import androidx.annotation.Nullable;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetPodcast;
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
    public final String summary;


    private PodcastSearchResult(String title, @Nullable String imageUrl, @Nullable String feedUrl, @Nullable String summary) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.feedUrl = feedUrl;
        this.summary = summary;
    }
    private PodcastSearchResult(String title, @Nullable String imageUrl, @Nullable String feedUrl) {
        this(title, imageUrl, feedUrl, "");
    }

    public static PodcastSearchResult dummy() {
        return new PodcastSearchResult("", "", "", "");
    }

    /**
     * Constructs a Podcast instance from a iTunes search result
     *
     * @param json object holding the podcast information
     * @throws JSONException
     */
    public static PodcastSearchResult fromItunes(JSONObject json) {
        String title = json.optString("collectionName", "");
        String imageUrl = json.optString("artworkUrl100", null);
        String feedUrl = json.optString("feedUrl", null);
        String summary = json.optString("artistName", null);
        return new PodcastSearchResult(title, imageUrl, feedUrl, summary);
    }

    /**
     * Constructs a Podcast instance from iTunes toplist entry
     *
     * @param json object holding the podcast information
     * @throws JSONException
     */
    public static PodcastSearchResult fromItunesToplist(JSONObject json) throws JSONException {
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

        String summary = null;
        try {
            summary = json.getJSONObject("summary").getString("label");
        } catch (Exception e) {
            // Some feeds have empty summary
        }
        return new PodcastSearchResult(title, imageUrl, feedUrl, summary);
    }

    public static PodcastSearchResult fromFyyd(SearchHit searchHit) {
        return new PodcastSearchResult(searchHit.getTitle(),
                                       searchHit.getThumbImageURL(),
                                       searchHit.getXmlUrl(),
                                       searchHit.getDescription());
    }

    public static PodcastSearchResult fromGpodder(GpodnetPodcast searchHit) {
        return new PodcastSearchResult(searchHit.getTitle(),
                                       searchHit.getLogoUrl(),
                                       searchHit.getUrl(),
                                       searchHit.getDescription());
    }
}
