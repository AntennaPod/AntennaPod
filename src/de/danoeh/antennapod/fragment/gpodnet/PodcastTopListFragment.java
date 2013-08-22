package de.danoeh.antennapod.fragment.gpodnet;

import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.gpoddernet.GpodnetService;
import de.danoeh.antennapod.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.gpoddernet.model.GpodnetPodcast;

import java.util.List;

/**
 *
 */
public class PodcastTopListFragment extends PodcastListFragment {
    private static final String TAG = "PodcastTopListFragment";
    private static final int PODCAST_COUNT = 50;

    @Override
    protected void onPodcastSelected(GpodnetPodcast selection) {
        if (AppConfig.DEBUG) Log.d(TAG, "Selected: " + selection.getTitle());
    }

    @Override
    protected List<GpodnetPodcast> loadPodcastData(GpodnetService service) throws GpodnetServiceException {
        return service.getPodcastToplist(PODCAST_COUNT);
    }
}
