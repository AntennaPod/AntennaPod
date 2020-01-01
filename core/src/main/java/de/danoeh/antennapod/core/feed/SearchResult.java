package de.danoeh.antennapod.core.feed;

import de.danoeh.antennapod.core.storage.SearchLocation;

public class SearchResult {
    private final FeedComponent component;
    private SearchLocation location;

    public SearchResult(FeedComponent component, SearchLocation location) {
        super();
        this.component = component;
        this.location = location;
    }

    public FeedComponent getComponent() {
        return component;
    }

    public SearchLocation getLocation() {
        return location;
    }
}
