package de.danoeh.antennapod.discovery.searchresultmapper;

import org.json.JSONObject;

import de.danoeh.antennapod.discovery.PodcastSearchResult;

public class AudiothekSearchResultMapper {
    public static PodcastSearchResult getPodcastSearchResult(JSONObject raw) {
        return new PodcastSearchResult("", null, null, null);
    }
}
