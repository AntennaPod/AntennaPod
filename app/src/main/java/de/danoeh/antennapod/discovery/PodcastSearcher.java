package de.danoeh.antennapod.discovery;

import io.reactivex.rxjava3.core.Single;
import java.util.List;

public interface PodcastSearcher {
    Single<List<PodcastSearchResult>> search(String query);

    Single<String> lookupUrl(String resultUrl);

    boolean urlNeedsLookup(String resultUrl);

    String getName();
}
