package de.podfetcher.activity;


import de.podfetcher.storage.DownloadRequester;
import de.podfetcher.adapter.DownloadlistAdapter;
import de.podfetcher.service.DownloadObserver;
import de.podfetcher.service.DownloadStatus;
import de.podfetcher.feed.FeedMedia;
import de.podfetcher.feed.FeedFile;
import com.actionbarsherlock.app.SherlockListActivity;

import android.os.Bundle;
import android.util.Log;

public class DownloadActivity extends SherlockListActivity {
    private static final String TAG = "DownloadActivity"; 

    private DownloadlistAdapter dla;
    private DownloadRequester requester;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requester = DownloadRequester.getInstance();
        observer.execute(requester.getDownloads().toArray(
                    new FeedFile[requester.getDownloads().size()]));

    }
    
    @Override
    protected void onStop()  {
    	super.onStop();
    	Log.d(TAG, "Stopping Activity");
    	observer.cancel(true);
    }

    private final DownloadObserver observer = new DownloadObserver(this) {
        @Override
        protected void onProgressUpdate(DownloadStatus... values) {
            if (dla != null) {
                dla.notifyDataSetChanged();
            } else {
                dla = new DownloadlistAdapter(getContext(), 0, getStatusList());
                setListAdapter(dla);
                dla.notifyDataSetChanged();
            }
        }
    };
}
