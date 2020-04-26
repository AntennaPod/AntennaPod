package de.danoeh.antennapod.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.adapter.FeedDiscoverAdapter;
import de.danoeh.antennapod.discovery.ItunesTopListLoader;
import de.danoeh.antennapod.discovery.PodcastSearchResult;
import io.reactivex.disposables.Disposable;

import java.util.ArrayList;
import java.util.List;


public class QuickFeedDiscoveryFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "FeedDiscoveryFragment";
    private static final int NUM_SUGGESTIONS = 12;

    private ProgressBar progressBar;
    private Disposable disposable;
    private FeedDiscoverAdapter adapter;
    private GridView discoverGridLayout;
    private TextView errorTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.quick_feed_discovery, container, false);
        View discoverMore = root.findViewById(R.id.discover_more);
        discoverMore.setOnClickListener(v ->
                ((MainActivity) getActivity()).loadChildFragment(new DiscoveryFragment()));

        discoverGridLayout = root.findViewById(R.id.discover_grid);
        progressBar = root.findViewById(R.id.discover_progress_bar);
        errorTextView = root.findViewById(R.id.discover_error);

        adapter = new FeedDiscoverAdapter((MainActivity) getActivity());
        discoverGridLayout.setAdapter(adapter);
        discoverGridLayout.setOnItemClickListener(this);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        if (screenWidthDp > 600) {
            discoverGridLayout.setNumColumns(6);
        } else {
            discoverGridLayout.setNumColumns(4);
        }

        // Fill with dummy elements to have a fixed height and
        // prevent the UI elements below from jumping on slow connections
        List<PodcastSearchResult> dummies = new ArrayList<>();
        for (int i = 0; i < NUM_SUGGESTIONS; i++) {
            dummies.add(PodcastSearchResult.dummy());
        }

        adapter.updateData(dummies);
        loadToplist();
        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void loadToplist() {
        progressBar.setVisibility(View.VISIBLE);
        discoverGridLayout.setVisibility(View.INVISIBLE);
        errorTextView.setVisibility(View.GONE);

        ItunesTopListLoader loader = new ItunesTopListLoader(getContext());
        disposable = loader.loadToplist(NUM_SUGGESTIONS)
                .subscribe(podcasts -> {
                    errorTextView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    discoverGridLayout.setVisibility(View.VISIBLE);
                    adapter.updateData(podcasts);
                }, error -> {
                    Log.e(TAG, Log.getStackTraceString(error));
                    errorTextView.setText(error.getLocalizedMessage());
                    errorTextView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    discoverGridLayout.setVisibility(View.INVISIBLE);
                });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
        PodcastSearchResult podcast = adapter.getItem(position);
        if (podcast.feedUrl == null) {
            return;
        }
        Intent intent = new Intent(getActivity(), OnlineFeedViewActivity.class);
        intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, podcast.feedUrl);
        startActivity(intent);
    }
}
