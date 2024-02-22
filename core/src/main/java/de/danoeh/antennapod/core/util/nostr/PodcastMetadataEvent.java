package de.danoeh.antennapod.core.util.nostr;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;

public class PodcastMetadataEvent extends NostrEvent {

    public PodcastMetadataEvent() {

    }

    /**
     * Extracts the podcast metadata and populates a list with the metadata,
     * in the order: name, description, image.
     * @param infoEventJson The JSON containing creator metadata.
     * @return {@link List} An ordered list of strings.
     * @throws JSONException If an error occurs during parsing of the Nostr event JSON.
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
