package de.danoeh.antennapod.discovery;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import java.util.List;

public interface PodcastSearcher {
    Single<List<PodcastSearchResult>> search(String query);

    Single<String> lookupUrl(String resultUrl);

    boolean urlNeedsLookup(String resultUrl);

    String getName();
}
