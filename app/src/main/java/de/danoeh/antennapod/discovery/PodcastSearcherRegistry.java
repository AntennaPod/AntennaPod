package de.danoeh.antennapod.discovery;


import io.reactivex.Single;

import java.util.ArrayList;
import java.util.List;

public class PodcastSearcherRegistry {
    private static List<SearcherInfo> searchProviders;

    private PodcastSearcherRegistry() {
    }

    public static List<SearcherInfo> getSearchProviders() {
        if (searchProviders == null) {
            searchProviders = new ArrayList<>();
            searchProviders.add(new SearcherInfo(new CombinedSearcher(), 1.0f));
            searchProviders.add(new SearcherInfo(new GpodnetPodcastSearcher(), 0.0f));
            searchProviders.add(new SearcherInfo(new FyydPodcastSearcher(), 1.0f));
            searchProviders.add(new SearcherInfo(new ItunesPodcastSearcher(), 1.0f));
            searchProviders.add(new SearcherInfo(new PodcastIndexPodcastSearcher(), 1.0f));
        }
        return searchProviders;
    }

    public static Single<String> lookupUrl(String url) {
        for (PodcastSearcherRegistry.SearcherInfo searchProviderInfo : getSearchProviders()) {
            if (searchProviderInfo.searcher.getClass() != CombinedSearcher.class
                    && searchProviderInfo.searcher.urlNeedsLookup(url)) {
                return searchProviderInfo.searcher.lookupUrl(url);
            }
        }
        return Single.just(url);
    }

    public static boolean urlNeedsLookup(String url) {
        for (PodcastSearcherRegistry.SearcherInfo searchProviderInfo : getSearchProviders()) {
            if (searchProviderInfo.searcher.getClass() != CombinedSearcher.class
                    && searchProviderInfo.searcher.urlNeedsLookup(url)) {
                return true;
            }
        }
        return false;
    }

    public static class SearcherInfo {
        public final PodcastSearcher searcher;
        public final float weight;

        public SearcherInfo(PodcastSearcher searcher, float weight) {
            this.searcher = searcher;
            this.weight = weight;
        }
    }
}
