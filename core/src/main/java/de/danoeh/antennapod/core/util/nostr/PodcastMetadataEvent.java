package de.danoeh.antennapod.core.util.nostr;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;

class PodcastMetadataEvent extends NostrEvent {

    private PodcastMetadataEvent() {

    }

    /**
     * Extracts the podcast metadata and populates a list with the metadata,
     * in the order: name, description, image.
     * @param infoEventJson
     * @return List<String>
     * @throws JSONException
     */
    public List<String> getPodcastInfo(String infoEventJson) throws JSONException {
        NostrEvent infoEvent = fromJson(infoEventJson);

        String infoEventContent = infoEvent.getContent();
        JSONObject infoJson = new JSONObject(infoEventContent);
        String podcastName = infoJson.getString("name");
        String podcastDescription = infoJson.getString("about");
        String imageUrl = infoJson.getString("picture");

        //The list is populated in parsing order
        return List.of(podcastName, podcastDescription, imageUrl);
    }
}
