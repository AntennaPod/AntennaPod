package de.danoeh.antennapod.fragment.gpodnet;

import android.support.annotation.NonNull;

import java.util.List;

import de.danoeh.antennapod.core.gpoddernet.GpodnetService;
import de.danoeh.antennapod.core.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetPodcast;

/**
 *
 */
public class PodcastTopListFragment extends PodcastListFragment {
    private static final String TAG = "PodcastTopListFragment";
    private static final int PODCAST_COUNT = 50;

    @Override
    protected List<GpodnetPodcast> loadPodcastData(@NonNull GpodnetService service) throws GpodnetServiceException {
        return service.getPodcastToplist(PODCAST_COUNT);
    }
}
