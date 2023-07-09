package de.danoeh.antennapod.net.discovery.audiothek;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.net.discovery.PodcastSearchResult;

public class AudiothekSearchResultMapper {

    public static final String AUDIOTHEK_BASE_URI = "https://api.ardaudiothek.de/";

    public static List<PodcastSearchResult> extractPodcasts(JSONObject searchResponse) throws JSONException {
        List<PodcastSearchResult> podcasts = new ArrayList<>();
        JSONObject embeddedProgramSetJsonObject = searchResponse
                .getJSONObject("data")
                .getJSONObject("search")
                .getJSONObject("programSets");

        JSONObject embeddedEditorialCollectionJsonObject = searchResponse
                .getJSONObject("data")
                .getJSONObject("search")
                .getJSONObject("editorialCollections");
        JSONArray programSets = new JSONArray();
        JSONArray editorialCollections = new JSONArray();
        try {
            programSets = embeddedProgramSetJsonObject
                    .getJSONArray("nodes");
            editorialCollections = embeddedEditorialCollectionJsonObject.getJSONArray("nodes");
            programSets = appendResults(programSets, editorialCollections);
        } catch (JSONException jsonException) {
            Log.d("Audiothek Parser", "Error parsing search result");
        }

        for (int i = 0; i < programSets.length(); i++) {
            JSONObject podcastJson = programSets.getJSONObject(i);
            try {
                PodcastSearchResult podcast = getPodcastSearchResult(podcastJson);
                if (podcast.feedUrl != null) {
                    podcasts.add(podcast);
                }
            } catch (JSONException exception) {
                continue;
            }

        }

        return podcasts;
    }

    private static JSONArray appendResults(JSONArray programSets, JSONArray editorialCollections) {
        for (int i = 0; i < editorialCollections.length(); i++) {
            try {
                programSets.put(editorialCollections.get(0));
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
            }
        }

        return programSets;
    }

    protected static PodcastSearchResult getPodcastSearchResult(JSONObject json) throws JSONException {
        String title = json.optString("title", "");
        JSONObject links = json.getJSONObject("_links");
        String imageUrl = getImageUrl(json.getJSONObject("image"));
        String feedUrlRaw = links.getJSONObject("self").optString("href", null);
        String feedUrl = AUDIOTHEK_BASE_URI + feedUrlRaw.replace("{?order,offset,limit}", "");
        String author = json.getJSONObject("publicationService").getString("organizationName");
        return new PodcastSearchResult(title, imageUrl, feedUrl, author);
    }

    @NonNull
    private static String getImageUrl(JSONObject imageLinks) {
        String imageUrl = imageLinks.optString("url1X1", null);
        if (imageUrl == null) {
            imageUrl = imageLinks.optString("url", null);
        }

        imageUrl = imageUrl.replace("{width}", "64");
        return imageUrl;
    }
}