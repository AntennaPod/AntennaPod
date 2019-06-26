package de.danoeh.antennapod.discovery;

import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.mfietz.fyydlin.FyydClient;
import de.mfietz.fyydlin.FyydResponse;
import de.mfietz.fyydlin.SearchHit;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;

public class FyydPodcastSearcher implements PodcastSearcher {
    private final String query;
    private final FyydClient client = new FyydClient(AntennapodHttpClient.getHttpClient());

    public FyydPodcastSearcher(String query) {
        this.query = query;
    }

    public Disposable search(Consumer<? super List<PodcastSearchResult>> successHandler, Consumer<? super Throwable> errorHandler) {
        return client.searchPodcasts(query, 10)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    ArrayList<PodcastSearchResult> results = processSearchResult(result);
                    successHandler.accept(results);
                }, errorHandler);
    }

    private ArrayList<PodcastSearchResult> processSearchResult(FyydResponse response) {
        ArrayList<PodcastSearchResult> searchResults = new ArrayList<>();

        if (!response.getData().isEmpty()) {
            for (SearchHit searchHit : response.getData()) {
                PodcastSearchResult podcast = PodcastSearchResult.fromFyyd(searchHit);
                searchResults.add(podcast);
            }
        }

        return searchResults;
    }
}
