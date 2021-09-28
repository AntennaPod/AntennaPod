package de.danoeh.antennapod.discovery.audiothek;

import androidx.annotation.NonNull;

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
        JSONObject embeddedProgramSetJsonObject = searchResponse
                .getJSONObject("_embedded")
                .getJSONObject("mt:programSetSearchResults")
                .getJSONObject("_embedded");

        JSONObject embeddedEditorialCollectionJsonObject = searchResponse
                .getJSONObject("_embedded")
                .getJSONObject("mt:editorialCollectionSearchResults")
                .getJSONObject("_embedded");
        JSONArray programSets = new JSONArray();
        JSONArray editorialCollections = new JSONArray();
        try {
            programSets = embeddedProgramSetJsonObject
                    .getJSONArray("mt:programSets");
            editorialCollections = embeddedEditorialCollectionJsonObject.getJSONArray("mt:editorialCollections");
            programSets = appendResults(programSets, editorialCollections);
        } catch (JSONException jsonException) {
            // if there is only a single result the response contains only a JSONObject and NOT JSONArray
            programSets.put(embeddedProgramSetJsonObject.getJSONObject("mt:programSets"));
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
        String imageUrl = links.getJSONObject("mt:squareImage").optString("href", null);
        imageUrl = imageUrl.replace("{ratio}", "1x1");
        imageUrl = imageUrl.replace("{width}", "64");
        String feedUrlRaw = links.getJSONObject("self").optString("href", null);
        String feedUrl = AUDIOTHEK_BASE_URI + feedUrlRaw.replace("{?order,offset,limit}", "");
        String author = getAuthor(json);
        return new PodcastSearchResult(title, imageUrl, feedUrl, author);
    }

    @NonNull
    private static String getAuthor(JSONObject json) throws JSONException {
        JSONObject embedded = json.optJSONObject("_embedded");
        if (embedded == null) {
            return "";
        }
        String author = embedded.getJSONObject("mt:publicationService")
                .getString("organizationName");
        return author;
    }
}
