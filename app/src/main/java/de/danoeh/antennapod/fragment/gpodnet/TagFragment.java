package de.danoeh.antennapod.fragment.gpodnet;

import android.os.Bundle;
import androidx.annotation.NonNull;

import java.util.List;

import de.danoeh.antennapod.net.sync.gpoddernet.GpodnetService;
import de.danoeh.antennapod.net.sync.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.net.sync.gpoddernet.model.GpodnetPodcast;
import de.danoeh.antennapod.net.sync.gpoddernet.model.GpodnetTag;

/**
 * Shows all podcasts from gpodder.net that belong to a specific tag.
 * Use the newInstance method of this class to create a new TagFragment.
 */
public class TagFragment extends PodcastListFragment {
    private static final int PODCAST_COUNT = 50;

    private GpodnetTag tag;

    public static TagFragment newInstance(@NonNull GpodnetTag tag) {
        TagFragment fragment = new TagFragment();
        Bundle args = new Bundle();
        args.putParcelable("tag", tag);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null || args.getParcelable("tag") == null) {
            throw new IllegalArgumentException("Arguments not given");
        }
        tag = args.getParcelable("tag");
    }

    @Override
    protected List<GpodnetPodcast> loadPodcastData(GpodnetService service) throws GpodnetServiceException {
        return service.getPodcastsForTag(tag, PODCAST_COUNT);
    }
}
