package de.danoeh.antennapod.event;

import java.util.Map;
import java.util.Set;

public class EpisodeDownloadEvent {
    private final Map<String, Integer> map;

    public EpisodeDownloadEvent(Map<String, Integer> map) {
        this.map = map;
    }

    public Set<String> getUrls() {
        return map.keySet();
    }
}
