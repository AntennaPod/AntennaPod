package de.danoeh.antennapod.core.util.nostr;

import org.json.JSONException;

import java.util.List;

public class PodcastEpisodeEvent extends NostrEvent {

    public PodcastEpisodeEvent(){

    }

    public List<String> getEpisodeInfo(String podcastEventJson) throws JSONException {
        NostrEvent podcastEvent = fromJson(podcastEventJson);
        List<List<String>> eventTags = podcastEvent.getTags();
        List<String> titleTag = eventTags.get(0);
        List<String> episodeUrlTag = eventTags.get(1);
        String title = titleTag.get(1);
        String episodeUrl = episodeUrlTag.get(1);
        String episodeNotes = podcastEvent.getContent();

        //The podcast data is stored in the parsing order above.
        return List.of(title, episodeUrl, episodeNotes);
    }

}
