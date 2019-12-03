package de.danoeh.antennapodSA.discovery;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CombinedSearcher implements PodcastSearcher {
    private static final String TAG = "CombinedSearcher";

    private final List<Pair<PodcastSearcher, Float>> searchProviders = new ArrayList<>();

    public CombinedSearcher(Context context) {
        addProvider(new FyydPodcastSearcher(), 1.f);
        addProvider(new ItunesPodcastSearcher(context), 1.f);
        //addProvider(new GpodnetPodcastSearcher(), 0.6f);
    }

    private void addProvider(PodcastSearcher provider, float priority) {
        searchProviders.add(new Pair<>(provider, priority));
    }

    public Single<List<PodcastSearchResult>> search(String query) {
        ArrayList<Disposable> disposables = new ArrayList<>();
        List<List<PodcastSearchResult>> singleResults = new ArrayList<>(Collections.nCopies(searchProviders.size(), null));
        CountDownLatch latch = new CountDownLatch(searchProviders.size());
        for (int i = 0; i < searchProviders.size(); i++) {
            Pair<PodcastSearcher, Float> searchProviderInfo = searchProviders.get(i);
            PodcastSearcher searcher = searchProviderInfo.first;
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
                        disposable.dispose();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private List<PodcastSearchResult> weightSearchResults(List<List<PodcastSearchResult>> singleResults) {
        HashMap<String, Float> resultRanking = new HashMap<>();
        HashMap<String, PodcastSearchResult> urlToResult = new HashMap<>();
        for (int i = 0; i < singleResults.size(); i++) {
            float providerPriority = searchProviders.get(i).second;
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
}
