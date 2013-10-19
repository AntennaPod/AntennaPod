package de.danoeh.antennapod.fragment.gpodnet;

import android.os.Bundle;
import de.danoeh.antennapod.gpoddernet.GpodnetService;
import de.danoeh.antennapod.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.gpoddernet.model.GpodnetPodcast;

import java.util.List;

/**
 * Created by daniel on 23.08.13.
 */
public class SearchListFragment extends PodcastListFragment {
    private static final String ARG_QUERY = "query";

    private String query;

    public static SearchListFragment newInstance(String query) {
        SearchListFragment fragment = new SearchListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey(ARG_QUERY)) {
            this.query = getArguments().getString(ARG_QUERY);
        } else {
            this.query = "";
        }
    }

    @Override
    protected List<GpodnetPodcast> loadPodcastData(GpodnetService service) throws GpodnetServiceException {
        return service.searchPodcasts(query, 0);
    }

    public void changeQuery(String query) {
        if (query == null) {
            throw new NullPointerException();
        }
        this.query = query;
        loadData();
    }
}
