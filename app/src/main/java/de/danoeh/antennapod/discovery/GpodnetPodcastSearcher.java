package de.danoeh.antennapod.discovery;

import de.danoeh.antennapod.core.gpoddernet.GpodnetService;
import de.danoeh.antennapod.core.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetPodcast;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;

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
