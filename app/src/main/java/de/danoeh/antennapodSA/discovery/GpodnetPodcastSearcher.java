package de.danoeh.antennapodSA.discovery;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapodSA.core.gpoddernet.GpodnetService;
import de.danoeh.antennapodSA.core.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapodSA.core.gpoddernet.model.GpodnetPodcast;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class GpodnetPodcastSearcher implements PodcastSearcher {
    public Single<List<PodcastSearchResult>> search(String query) {
        return Single.create((SingleOnSubscribe<List<PodcastSearchResult>>) subscriber -> {
            GpodnetService service = null;
            try {
                service = new GpodnetService();
                List<GpodnetPodcast> gpodnetPodcasts = service.searchPodcasts(query, 0);
                List<PodcastSearchResult> results = new ArrayList<>();
                for (GpodnetPodcast podcast : gpodnetPodcasts) {
                    results.add(PodcastSearchResult.fromGpodder(podcast));
                }
                subscriber.onSuccess(results);
            } catch (GpodnetServiceException e) {
                e.printStackTrace();
                subscriber.onError(e);
            } finally {
                if (service != null) {
                    service.shutdown();
                }
            }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }
}
