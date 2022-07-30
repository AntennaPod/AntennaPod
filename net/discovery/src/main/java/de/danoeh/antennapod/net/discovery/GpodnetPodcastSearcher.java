package de.danoeh.antennapod.net.discovery;

import de.danoeh.antennapod.core.sync.SynchronizationCredentials;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.net.sync.gpoddernet.GpodnetService;
import de.danoeh.antennapod.net.sync.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.net.sync.gpoddernet.model.GpodnetPodcast;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;

public class GpodnetPodcastSearcher implements PodcastSearcher {
    public Single<List<PodcastSearchResult>> search(String query) {
        return Single.create((SingleOnSubscribe<List<PodcastSearchResult>>) subscriber -> {
            try {
                GpodnetService service = new GpodnetService(AntennapodHttpClient.getHttpClient(),
                        SynchronizationCredentials.getHosturl(), SynchronizationCredentials.getDeviceID(),
                        SynchronizationCredentials.getUsername(), SynchronizationCredentials.getPassword());
                List<GpodnetPodcast> gpodnetPodcasts = service.searchPodcasts(query, 0);
                List<PodcastSearchResult> results = new ArrayList<>();
                for (GpodnetPodcast podcast : gpodnetPodcasts) {
                    results.add(PodcastSearchResult.fromGpodder(podcast));
                }
                subscriber.onSuccess(results);
            } catch (GpodnetServiceException e) {
                e.printStackTrace();
                subscriber.onError(e);
            }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<String> lookupUrl(String url) {
        return Single.just(url);
    }

    @Override
    public boolean urlNeedsLookup(String url) {
        return false;
    }

    @Override
    public String getName() {
        return "Gpodder.net";
    }
}
