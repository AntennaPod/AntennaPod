package de.danoeh.antennapodSA.discovery;

import java.util.List;

import io.reactivex.Single;

public interface PodcastSearcher {
    Single<List<PodcastSearchResult>> search(String query);
}
