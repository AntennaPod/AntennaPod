package de.danoeh.antennapod.fragment.gpodnet;

import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.sync.gpoddernet.GpodnetService;
import de.danoeh.antennapod.core.sync.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.core.sync.gpoddernet.model.GpodnetPodcast;

/**
 * Displays suggestions from gpodder.net
 */
public class SuggestionListFragment extends PodcastListFragment {
    private static final int SUGGESTIONS_COUNT = 50;

    @Override
    protected List<GpodnetPodcast> loadPodcastData(GpodnetService service) throws GpodnetServiceException {
        if (GpodnetPreferences.loggedIn()) {
            service.authenticate(GpodnetPreferences.getUsername(), GpodnetPreferences.getPassword());
            return service.getSuggestions(SUGGESTIONS_COUNT);
        } else {
            return Collections.emptyList();
        }
    }
}
