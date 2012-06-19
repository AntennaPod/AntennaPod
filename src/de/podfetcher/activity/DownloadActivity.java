package de.podfetcher.activity;


import de.podfetcher.R;
import de.podfetcher.storage.DownloadRequester;
import de.podfetcher.adapter.DownloadlistAdapter;
import de.podfetcher.asynctask.DownloadObserver;
import de.podfetcher.asynctask.DownloadStatus;
import de.podfetcher.feed.FeedMedia;
import de.podfetcher.feed.FeedFile;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class DownloadActivity extends SherlockListActivity {
    private static final String TAG = "DownloadActivity"; 
    private static final int MENU_SHOW_LOG = 0;
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
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_SHOW_LOG, Menu.NONE, R.string.show_download_log)
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SHOW_LOG:
			startActivity(new Intent(this, DownloadLogActivity.class));
		}
		return true;
	}
}
