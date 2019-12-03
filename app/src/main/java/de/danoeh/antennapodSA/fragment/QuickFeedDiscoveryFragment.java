package de.danoeh.antennapodSA.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapodSA.R;
import de.danoeh.antennapodSA.activity.MainActivity;
import de.danoeh.antennapodSA.activity.OnlineFeedViewActivity;
import de.danoeh.antennapodSA.adapter.FeedDiscoverAdapter;
import de.danoeh.antennapodSA.discovery.ItunesTopListLoader;
import de.danoeh.antennapodSA.discovery.PodcastSearchResult;
import io.reactivex.disposables.Disposable;


public class QuickFeedDiscoveryFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "FeedDiscoveryFragment";

    private ProgressBar progressBar;
    private Disposable disposable;
    private FeedDiscoverAdapter adapter;
    private GridView subscriptionGridLayout;
    private TextView errorTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.quick_feed_discovery, container, false);
        View discoverMore = root.findViewById(R.id.discover_more);
        discoverMore.setOnClickListener(v ->
                ((MainActivity) getActivity()).loadChildFragment(new ItunesSearchFragment()));

        subscriptionGridLayout = root.findViewById(R.id.discover_grid);
        progressBar = root.findViewById(R.id.discover_progress_bar);
        errorTextView = root.findViewById(R.id.discover_error);

        adapter = new FeedDiscoverAdapter((MainActivity) getActivity());
        subscriptionGridLayout.setAdapter(adapter);
        subscriptionGridLayout.setOnItemClickListener(this);

        // Fill with dummy elements to have a fixed height and
        // prevent the UI elements below from jumping on slow connections
        List<PodcastSearchResult> dummies = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
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
        subscriptionGridLayout.setVisibility(View.INVISIBLE);
        errorTextView.setVisibility(View.GONE);

        ItunesTopListLoader loader = new ItunesTopListLoader(getContext());
        disposable = loader.loadToplist(8)
                .subscribe(podcasts -> {
                    errorTextView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    subscriptionGridLayout.setVisibility(View.VISIBLE);
                    adapter.updateData(podcasts);
                }, error -> {
                    Log.e(TAG, Log.getStackTraceString(error));
                    errorTextView.setText(error.getLocalizedMessage());
                    errorTextView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    subscriptionGridLayout.setVisibility(View.INVISIBLE);
                });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
        PodcastSearchResult podcast = adapter.getItem(position);
        if (podcast.feedUrl == null) {
            return;
        }
        view.setAlpha(0.5f);
        ItunesTopListLoader loader = new ItunesTopListLoader(getContext());
        disposable = loader.getFeedUrl(podcast)
                .subscribe(feedUrl -> {
                    view.setAlpha(1f);
                    Intent intent = new Intent(getActivity(), OnlineFeedViewActivity.class);
                    intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, feedUrl);
                    intent.putExtra(OnlineFeedViewActivity.ARG_TITLE, getString(R.string.add_feed_label));
                    startActivity(intent);
                }, error -> {
                    Log.e(TAG, Log.getStackTraceString(error));
                    view.setAlpha(1f);
                    String prefix = getString(R.string.error_msg_prefix);
                    new AlertDialog.Builder(getActivity())
                            .setMessage(prefix + " " + error.getMessage())
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
    }
}
