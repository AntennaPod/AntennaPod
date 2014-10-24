package de.danoeh.antennapod.fragment.gpodnet;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.widget.*;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.DefaultOnlineFeedViewActivity;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.adapter.gpodnet.PodcastListAdapter;
import de.danoeh.antennapod.core.gpoddernet.GpodnetService;
import de.danoeh.antennapod.core.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetPodcast;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.menuhandler.NavDrawerActivity;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (!MenuItemUtils.isActivityDrawerOpen((NavDrawerActivity) getActivity())) {
            final android.support.v7.widget.SearchView sv = new android.support.v7.widget.SearchView(getActivity());
            MenuItemUtils.addSearchItem(menu, sv);
            sv.setQueryHint(getString(R.string.gpodnet_search_hint));
            sv.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    sv.clearFocus();
                    ((MainActivity) getActivity()).loadChildFragment(SearchListFragment.newInstance(s));
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    return false;
                }
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.gpodnet_podcast_list, container, false);

        gridView = (GridView) root.findViewById(R.id.gridView);
        progressBar = (ProgressBar) root.findViewById(R.id.progressBar);
        txtvError = (TextView) root.findViewById(R.id.txtvError);
        butRetry = (Button) root.findViewById(R.id.butRetry);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onPodcastSelected((GpodnetPodcast) gridView.getAdapter().getItem(position));
            }
        });
        butRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadData();
            }
        });

        loadData();
        return root;
    }

    protected void onPodcastSelected(GpodnetPodcast selection) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Selected podcast: " + selection.toString());
        Intent intent = new Intent(getActivity(), DefaultOnlineFeedViewActivity.class);
        intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, selection.getUrl());
        intent.putExtra(DefaultOnlineFeedViewActivity.ARG_TITLE, getString(R.string.gpodnet_main_label));
        startActivity(intent);
    }

    protected abstract List<GpodnetPodcast> loadPodcastData(GpodnetService service) throws GpodnetServiceException;

    protected final void loadData() {
        AsyncTask<Void, Void, List<GpodnetPodcast>> loaderTask = new AsyncTask<Void, Void, List<GpodnetPodcast>>() {
            volatile Exception exception = null;

            @Override
            protected List<GpodnetPodcast> doInBackground(Void... params) {
                GpodnetService service = null;
                try {
                    service = new GpodnetService();
                    return loadPodcastData(service);
                } catch (GpodnetServiceException e) {
                    exception = e;
                    e.printStackTrace();
                    return null;
                } finally {
                    if (service != null) {
                        service.shutdown();
                    }
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

        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
            loaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            loaderTask.execute();
        }
    }
}
