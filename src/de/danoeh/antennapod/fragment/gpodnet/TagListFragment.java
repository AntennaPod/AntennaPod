package de.danoeh.antennapod.fragment.gpodnet;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import de.danoeh.antennapod.activity.gpoddernet.GpodnetTagActivity;
import de.danoeh.antennapod.gpoddernet.GpodnetService;
import de.danoeh.antennapod.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.gpoddernet.model.GpodnetTag;

import java.util.ArrayList;
import java.util.List;

public class TagListFragment extends ListFragment {
    private static final String TAG = "TagListFragment";
    private static final int COUNT = 50;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setRetainInstance(true);

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedTag = (String) getListAdapter().getItem(position);
                Intent intent = new Intent(getActivity(), GpodnetTagActivity.class);
                intent.putExtra(GpodnetTagActivity.ARG_TAGNAME, selectedTag);
                startActivity(intent);
            }
        });

        loadData();
    }

    private void loadData() {
        AsyncTask<Void, Void, List<GpodnetTag>> task = new AsyncTask<Void, Void, List<GpodnetTag>>() {
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
                        List<String> tagNames = new ArrayList<String>();
                        for (GpodnetTag tag : gpodnetTags) {
                            tagNames.add(tag.getName());
                        }
                        setListAdapter(new ArrayAdapter<String>(context, R.layout.simple_list_item_1, tagNames));
                        setListShown(true);
                    } else if (exception != null) {
                        TextView txtvError = new TextView(getActivity());
                        txtvError.setText(exception.getMessage());
                        getListView().setEmptyView(txtvError);
                    } else {
                        setListShown(true);
                    }
                }
            }
        };
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            task.execute();
        }
    }
}

