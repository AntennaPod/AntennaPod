package de.danoeh.antennapod.net.discovery;

import android.text.TextUtils;
import android.util.Log;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class CombinedSearcher implements PodcastSearcher {
    private static final String TAG = "CombinedSearcher";

    public CombinedSearcher() {
    }

    public Single<List<PodcastSearchResult>> search(String query) {
        ArrayList<Disposable> disposables = new ArrayList<>();
        List<List<PodcastSearchResult>> singleResults = new ArrayList<>(
                Collections.nCopies(PodcastSearcherRegistry.getSearchProviders().size(), null));
        CountDownLatch latch = new CountDownLatch(PodcastSearcherRegistry.getSearchProviders().size());
        for (int i = 0; i < PodcastSearcherRegistry.getSearchProviders().size(); i++) {
            PodcastSearcherRegistry.SearcherInfo searchProviderInfo
                    = PodcastSearcherRegistry.getSearchProviders().get(i);
            PodcastSearcher searcher = searchProviderInfo.searcher;
            if (searchProviderInfo.weight <= 0.00001f || searcher.getClass() == CombinedSearcher.class) {
                latch.countDown();
                continue;
            }
            final int index = i;
            disposables.add(searcher.search(query).subscribe(e -> {
                        singleResults.set(index, e);
                        latch.countDown();
                    }, throwable -> {
                        Log.d(TAG, Log.getStackTraceString(throwable));
                        latch.countDown();
                    }
            ));
        }

        return Single.create((SingleOnSubscribe<List<PodcastSearchResult>>) subscriber -> {
            latch.await();
            List<PodcastSearchResult> results = weightSearchResults(singleResults);
            subscriber.onSuccess(results);
        })
                .doOnDispose(() -> {
                    for (Disposable disposable : disposables) {
                        if (disposable != null) {
                            disposable.dispose();
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private List<PodcastSearchResult> weightSearchResults(List<List<PodcastSearchResult>> singleResults) {
        HashMap<String, Float> resultRanking = new HashMap<>();
        HashMap<String, PodcastSearchResult> urlToResult = new HashMap<>();
        for (int i = 0; i < singleResults.size(); i++) {
            float providerPriority = PodcastSearcherRegistry.getSearchProviders().get(i).weight;
            List<PodcastSearchResult> providerResults = singleResults.get(i);
            if (providerResults == null) {
                continue;
            }
            for (int position = 0; position < providerResults.size(); position++) {
                PodcastSearchResult result = providerResults.get(position);
                urlToResult.put(result.feedUrl, result);

                float ranking = 0;
                if (resultRanking.containsKey(result.feedUrl)) {
                    ranking = resultRanking.get(result.feedUrl);
                }
                ranking += 1.f / (position + 1.f);
                resultRanking.put(result.feedUrl, ranking * providerPriority);
            }
        }
        List<Map.Entry<String, Float>> sortedResults = new ArrayList<>(resultRanking.entrySet());
        Collections.sort(sortedResults, (o1, o2) -> Double.compare(o2.getValue(), o1.getValue()));

        List<PodcastSearchResult> results = new ArrayList<>();
        for (Map.Entry<String, Float> res : sortedResults) {
            results.add(urlToResult.get(res.getKey()));
        }
        return results;
    }

    @Override
    public Single<String> lookupUrl(String url) {
        return PodcastSearcherRegistry.lookupUrl(url);
    }

    @Override
    public boolean urlNeedsLookup(String url) {
        return PodcastSearcherRegistry.urlNeedsLookup(url);
    }

    @Override
    public String getName() {
        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < PodcastSearcherRegistry.getSearchProviders().size(); i++) {
            PodcastSearcherRegistry.SearcherInfo searchProviderInfo
                    = PodcastSearcherRegistry.getSearchProviders().get(i);
            PodcastSearcher searcher = searchProviderInfo.searcher;
            if (searchProviderInfo.weight > 0.00001f && searcher.getClass() != CombinedSearcher.class) {
                names.add(searcher.getName());
            }
        }
        return TextUtils.join(", ", names);
    }
}
