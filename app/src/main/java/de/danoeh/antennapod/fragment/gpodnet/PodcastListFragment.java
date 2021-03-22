package de.danoeh.antennapod.fragment.gpodnet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.adapter.gpodnet.PodcastListAdapter;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.core.sync.gpoddernet.GpodnetService;
import de.danoeh.antennapod.core.sync.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.core.sync.gpoddernet.model.GpodnetPodcast;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.util.List;

/**
 * Displays a list of GPodnetPodcast-Objects in a GridView
 */
public abstract class PodcastListFragment extends Fragment {
    private static final String TAG = "PodcastListFragment";

    private GridView gridView;
    private ProgressBar progressBar;
    private TextView txtvError;
    private Button butRetry;
    private Disposable disposable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.gpodnet_podcast_list, container, false);
        gridView = root.findViewById(R.id.gridView);
        progressBar = root.findViewById(R.id.progressBar);
        txtvError = root.findViewById(R.id.txtvError);
        butRetry = root.findViewById(R.id.butRetry);

        gridView.setOnItemClickListener((parent, view, position, id) ->
                onPodcastSelected((GpodnetPodcast) gridView.getAdapter().getItem(position)));
        butRetry.setOnClickListener(v -> loadData());

        loadData();
        return root;
    }

    private void onPodcastSelected(GpodnetPodcast selection) {
        Log.d(TAG, "Selected podcast: " + selection.toString());
        Intent intent = new Intent(getActivity(), OnlineFeedViewActivity.class);
        intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, selection.getUrl());
        intent.putExtra(MainActivity.EXTRA_STARTED_FROM_SEARCH, true);
        startActivity(intent);
    }

    protected abstract List<GpodnetPodcast> loadPodcastData(GpodnetService service) throws GpodnetServiceException;

    final void loadData() {
        if (disposable != null) {
            disposable.dispose();
        }
        gridView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        txtvError.setVisibility(View.GONE);
        butRetry.setVisibility(View.GONE);
        disposable = Observable.fromCallable(
                () -> {
                    GpodnetService service = new GpodnetService(AntennapodHttpClient.getHttpClient(),
                            GpodnetPreferences.getHosturl());
                    return loadPodcastData(service);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        podcasts -> {
                            progressBar.setVisibility(View.GONE);
                            butRetry.setVisibility(View.GONE);

                            if (podcasts.size() > 0) {
                                PodcastListAdapter listAdapter = new PodcastListAdapter(getContext(), 0, podcasts);
                                gridView.setAdapter(listAdapter);
                                listAdapter.notifyDataSetChanged();
                                gridView.setVisibility(View.VISIBLE);
                                txtvError.setVisibility(View.GONE);
                            } else {
                                gridView.setVisibility(View.GONE);
                                txtvError.setText(getString(R.string.search_status_no_results));
                                txtvError.setVisibility(View.VISIBLE);
                            }
                        }, error -> {
                            gridView.setVisibility(View.GONE);
                            progressBar.setVisibility(View.GONE);
                            txtvError.setText(getString(R.string.error_msg_prefix) + error.getMessage());
                            txtvError.setVisibility(View.VISIBLE);
                            butRetry.setVisibility(View.VISIBLE);
                            Log.e(TAG, Log.getStackTraceString(error));
                        });
    }
}
