package de.danoeh.antennapod.net.discovery;

import io.reactivex.rxjava3.core.Single;
import java.util.Collections;
import java.util.List;

public interface PodcastSearcher {
    Single<List<PodcastSearchResult>> search(String query);

    default Single<List<EpisodeSearchResult>> searchEpisodes(String query) {
        return Single.just(Collections.emptyList());
    }

    Single<String> lookupUrl(String resultUrl);

    boolean urlNeedsLookup(String resultUrl);

    String getName();
}
