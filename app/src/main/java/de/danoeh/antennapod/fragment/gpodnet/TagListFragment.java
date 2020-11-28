package de.danoeh.antennapod.fragment.gpodnet;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.fragment.app.ListFragment;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.gpodnet.TagListAdapter;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.core.sync.gpoddernet.GpodnetService;
import de.danoeh.antennapod.core.sync.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.core.sync.gpoddernet.model.GpodnetTag;

import java.util.List;

public class TagListFragment extends ListFragment {
    private static final int COUNT = 50;

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
                GpodnetService service = new GpodnetService(AntennapodHttpClient.getHttpClient(),
                        GpodnetPreferences.getHostname());
                try {
                    return service.getTopTags(COUNT);
                } catch (GpodnetServiceException e) {
                    e.printStackTrace();
                    exception = e;
                    return null;
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

