package de.danoeh.antennapod.fragment.gpodnet;

import android.os.Bundle;

import org.apache.commons.lang3.Validate;

import java.util.List;

import de.danoeh.antennapod.core.sync.gpoddernet.GpodnetService;
import de.danoeh.antennapod.core.sync.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.core.sync.gpoddernet.model.GpodnetPodcast;
import de.danoeh.antennapod.core.sync.gpoddernet.model.GpodnetTag;

/**
 * Shows all podcasts from gpodder.net that belong to a specific tag.
 * Use the newInstance method of this class to create a new TagFragment.
 */
public class TagFragment extends PodcastListFragment {

    private static final String TAG = "TagFragment";
    private static final int PODCAST_COUNT = 50;

    private GpodnetTag tag;

    public static TagFragment newInstance(GpodnetTag tag) {
        Validate.notNull(tag);
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
        Validate.isTrue(args != null && args.getParcelable("tag") != null, "args invalid");
        tag = args.getParcelable("tag");
    }

    @Override
    protected List<GpodnetPodcast> loadPodcastData(GpodnetService service) throws GpodnetServiceException {
        return service.getPodcastsForTag(tag, PODCAST_COUNT);
    }
}
