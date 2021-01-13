package de.danoeh.antennapod.fragment.gpodnet;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.adapter.gpodnet.PodcastListAdapter;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.core.sync.gpoddernet.GpodnetService;
import de.danoeh.antennapod.core.sync.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.core.sync.gpoddernet.model.GpodnetPodcast;

/**
 * Displays a list of GPodnetPodcast-Objects in a GridView
 */
public abstract class PodcastListFragment extends Fragment {
    private static final String TAG = "PodcastListFragment";

    private GridView gridView;
    private ProgressBar progressBar;
    private TextView txtvError;
    private Button butRetry;

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
        AsyncTask<Void, Void, List<GpodnetPodcast>> loaderTask = new AsyncTask<Void, Void, List<GpodnetPodcast>>() {
            volatile Exception exception = null;

            @Override
            protected List<GpodnetPodcast> doInBackground(Void... params) {
                try {
                    GpodnetService service = new GpodnetService(AntennapodHttpClient.getHttpClient(),
                            GpodnetPreferences.getHostname());
                    return loadPodcastData(service);
                } catch (GpodnetServiceException e) {
                    exception = e;
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<GpodnetPodcast> gpodnetPodcasts) {
                super.onPostExecute(gpodnetPodcasts);
                final Context context = getActivity();
                if (context != null && gpodnetPodcasts != null && gpodnetPodcasts.size() > 0) {
                    PodcastListAdapter listAdapter = new PodcastListAdapter(context, 0, gpodnetPodcasts);
                    gridView.setAdapter(listAdapter);
                    listAdapter.notifyDataSetChanged();

                    progressBar.setVisibility(View.GONE);
                    gridView.setVisibility(View.VISIBLE);
                    txtvError.setVisibility(View.GONE);
                    butRetry.setVisibility(View.GONE);
                } else if (context != null && gpodnetPodcasts != null) {
                    gridView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    txtvError.setText(getString(R.string.search_status_no_results));
                    txtvError.setVisibility(View.VISIBLE);
                    butRetry.setVisibility(View.GONE);
                } else if (context != null) {
                    gridView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    txtvError.setText(getString(R.string.error_msg_prefix) + exception.getMessage());
                    txtvError.setVisibility(View.VISIBLE);
                    butRetry.setVisibility(View.VISIBLE);
                }
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                gridView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                txtvError.setVisibility(View.GONE);
                butRetry.setVisibility(View.GONE);
            }
        };

        loaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
