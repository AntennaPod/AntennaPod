package de.danoeh.antennapod.discovery.searchresultmapper;

import org.json.JSONException;
import org.json.JSONObject;

import de.danoeh.antennapod.discovery.PodcastSearchResult;

public class AudiothekSearchResultMapper {

    public static final String AUDIOTHEK_BASE_URI = "https://api.ardaudiothek.de/";

    public static PodcastSearchResult getPodcastSearchResult(JSONObject json) throws JSONException {
        String title = json.optString("title", "");
        JSONObject links = json.getJSONObject("_links");
        String imageUrl = links.getJSONObject("mt:squareImage").optString("href", null);
        String feedUrlRaw = links.getJSONObject("self").optString("href", null);
        String feedUrl = AUDIOTHEK_BASE_URI + feedUrlRaw.replace("{?order,offset,limit}", "");
        String author = json.getJSONObject("_embedded").getJSONObject("mt:publicationService")
                .getString("organizationName");
        return new PodcastSearchResult(title, imageUrl, feedUrl, author);
    }
}
