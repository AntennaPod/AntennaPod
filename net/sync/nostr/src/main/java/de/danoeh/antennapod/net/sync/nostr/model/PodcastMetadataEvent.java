package de.danoeh.antennapod.net.sync.nostr.model;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;

public class PodcastMetadataEvent extends NostrEvent {

    public PodcastMetadataEvent() {

    }

    /**
     * Extracts the podcast metadata and populates a list with the metadata,
     * in the order: name, description, image.
     * @return {@link List} An ordered list of strings.
     * @throws JSONException If an error occurs during parsing of the Nostr event JSON.
     */
    public List<String> getPodcastInfo() throws JSONException {

        String infoEventContent = this.getContent();
        JSONObject infoJson = new JSONObject(infoEventContent);
        String podcastName = infoJson.getString("name");
        String podcastDescription = infoJson.getString("about");
        String imageUrl = infoJson.getString("picture");

        //The list is populated in parsing order
        return List.of(podcastName, podcastDescription, imageUrl);
    }
}
