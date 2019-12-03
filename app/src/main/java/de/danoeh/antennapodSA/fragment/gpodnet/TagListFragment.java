package de.danoeh.antennapodSA.fragment.gpodnet;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.ListFragment;

import java.util.List;

import de.danoeh.antennapodSA.R;
import de.danoeh.antennapodSA.core.gpoddernet.GpodnetService;
import de.danoeh.antennapodSA.core.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapodSA.core.gpoddernet.model.GpodnetTag;
import de.danoeh.antennapodSA.activity.MainActivity;
import de.danoeh.antennapodSA.adapter.gpodnet.TagListAdapter;

public class TagListFragment extends ListFragment {

    private static final String TAG = "TagListFragment";
    private static final int COUNT = 50;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.gpodder_podcasts, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView sv = (SearchView) MenuItemCompat.getActionView(searchItem);
        sv.setQueryHint(getString(R.string.gpodnet_search_hint));
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Activity activity = getActivity();
                if (activity != null) {
                    sv.clearFocus();
                    ((MainActivity) activity).loadChildFragment(SearchListFragment.newInstance(s));
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setOnItemClickListener((parent, view1, position, id) -> {
            GpodnetTag tag = (GpodnetTag) getListAdapter().getItem(position);
            MainActivity activity = (MainActivity) getActivity();
            activity.loadChildFragment(TagFragment.newInstance(tag));
        });

        startLoadTask();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) getActivity()).getSupportActionBar().setTitle(R.string.add_feed_label);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelLoadTask();
    }

    private AsyncTask<Void, Void, List<GpodnetTag>> loadTask;

    private void cancelLoadTask() {
        if (loadTask != null && !loadTask.isCancelled()) {
            loadTask.cancel(true);
        }
    }

    private void startLoadTask() {
        cancelLoadTask();
        loadTask = new AsyncTask<Void, Void, List<GpodnetTag>>() {
            private Exception exception;

            @Override
            protected List<GpodnetTag> doInBackground(Void... params) {
                GpodnetService service = new GpodnetService();
                try {
                    return service.getTopTags(COUNT);
                } catch (GpodnetServiceException e) {
                    e.printStackTrace();
                    exception = e;
                    return null;
                } finally {
                    service.shutdown();
                }
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                setListShown(false);
            }

            @Override
            protected void onPostExecute(List<GpodnetTag> gpodnetTags) {
                super.onPostExecute(gpodnetTags);
                final Context context = getActivity();
                if (context != null) {
                    if (gpodnetTags != null) {
                        setListAdapter(new TagListAdapter(context, android.R.layout.simple_list_item_1, gpodnetTags));
                    } else if (exception != null) {
                        TextView txtvError = new TextView(getActivity());
                        txtvError.setText(exception.getMessage());
                        getListView().setEmptyView(txtvError);
                    }
                    setListShown(true);

                }
            }
        };
        loadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}

