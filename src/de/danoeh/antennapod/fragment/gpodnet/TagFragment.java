package de.danoeh.antennapod.fragment.gpodnet;

import android.os.Bundle;

import org.apache.commons.lang3.Validate;

import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.gpoddernet.GpodnetService;
import de.danoeh.antennapod.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.gpoddernet.model.GpodnetPodcast;
import de.danoeh.antennapod.gpoddernet.model.GpodnetTag;

import java.util.List;

/**
 * Shows all podcasts from gpodder.net that belong to a specific tag.
 * Use the newInstance method of this class to create a new TagFragment.
 */
public class TagFragment extends PodcastListFragment {

    private static final String TAG = "TagFragment";
    private static final int PODCAST_COUNT = 50;

    private GpodnetTag tag;

    public static TagFragment newInstance(String tagName) {
        Validate.notNull(tagName);
        TagFragment fragment = new TagFragment();
        Bundle args = new Bundle();
        args.putString("tag", tagName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        Validate.isTrue(args != null && args.getString("tag") != null, "args invalid");

        tag = new GpodnetTag(args.getString("tag"));
        ((MainActivity) getActivity()).getMainActivtyActionBar().setTitle(tag.getName());
    }

    @Override
    protected List<GpodnetPodcast> loadPodcastData(GpodnetService service) throws GpodnetServiceException {
        return service.getPodcastsForTag(tag, PODCAST_COUNT);
    }
}
