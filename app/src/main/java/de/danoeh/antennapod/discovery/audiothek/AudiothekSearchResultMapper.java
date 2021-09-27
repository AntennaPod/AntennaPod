package de.danoeh.antennapod.discovery.audiothek;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.discovery.PodcastSearchResult;

public class AudiothekSearchResultMapper {

    public static final String AUDIOTHEK_BASE_URI = "https://api.ardaudiothek.de/";

    public static List<PodcastSearchResult> extractPodcasts(JSONObject searchResponse) throws JSONException {
        List<PodcastSearchResult> podcasts = new ArrayList<>();
        JSONObject embeddedJsonObject = searchResponse
                .getJSONObject("_embedded")
                .getJSONObject("mt:programSetSearchResults")
                .getJSONObject("_embedded");
        JSONArray programSets = new JSONArray();
        try {
            programSets = embeddedJsonObject
                    .getJSONArray("mt:programSets");
        } catch (JSONException jsonException) {
            programSets.put(embeddedJsonObject.getJSONObject("mt:programSets"));
        }

        for (int i = 0; i < programSets.length(); i++) {
            JSONObject podcastJson = programSets.getJSONObject(i);
            PodcastSearchResult podcast = getPodcastSearchResult(podcastJson);
            if (podcast.feedUrl != null) {
                podcasts.add(podcast);
            }
        }

        return podcasts;
    }

    protected static PodcastSearchResult getPodcastSearchResult(JSONObject json) throws JSONException {
        String title = json.optString("title", "");
        JSONObject links = json.getJSONObject("_links");
        String imageUrl = links.getJSONObject("mt:squareImage").optString("href", null);
        imageUrl = imageUrl.replace("{ratio}", "1x1");
        imageUrl = imageUrl.replace("{width}", "64");
        String feedUrlRaw = links.getJSONObject("self").optString("href", null);
        String feedUrl = AUDIOTHEK_BASE_URI + feedUrlRaw.replace("{?order,offset,limit}", "");
        String author = json.getJSONObject("_embedded").getJSONObject("mt:publicationService")
                .getString("organizationName");
        return new PodcastSearchResult(title, imageUrl, feedUrl, author);
    }
}
