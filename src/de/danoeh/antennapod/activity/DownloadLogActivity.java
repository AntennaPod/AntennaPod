package de.danoeh.antennapod.activity;

import android.os.AsyncTask;
import android.os.Bundle;

import android.util.Log;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.adapter.DownloadLogAdapter;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.service.download.DownloadStatus;
import de.danoeh.antennapod.storage.DBReader;

import java.util.List;

/**
 * Displays completed and failed downloads in a list.
 */
public class DownloadLogActivity extends SherlockListActivity {
	private static final String TAG = "DownloadLogActivity";

    private List<DownloadStatus> downloadLog;
	private DownloadLogAdapter dla;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(UserPreferences.getTheme());
		super.onCreate(savedInstanceState);

		dla = new DownloadLogAdapter(this, itemAccess);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setListAdapter(dla);
        loadData();
	}

	@Override
	protected void onPause() {
		super.onPause();
		EventDistributor.getInstance().unregister(contentUpdate);
	}

	@Override
	protected void onResume() {
		super.onResume();
		EventDistributor.getInstance().register(contentUpdate);
		dla.notifyDataSetChanged();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		default:
			return false;
		}
		return true;
	}

    private void loadData() {
        AsyncTask<Void, Void, List<DownloadStatus>> loadTask = new AsyncTask<Void, Void, List<DownloadStatus>>() {
            @Override
            protected List<DownloadStatus> doInBackground(Void... voids) {
                return DBReader.getDownloadLog(DownloadLogActivity.this);
            }

            @Override
            protected void onPostExecute(List<DownloadStatus> downloadStatuses) {
                super.onPostExecute(downloadStatuses);
                if (downloadStatuses != null) {
                    downloadLog = downloadStatuses;
                    if (dla != null) {
                        dla.notifyDataSetChanged();
                    }
                } else {
                    Log.e(TAG, "Could not load download log");
                }
            }
        };
        loadTask.execute();
    }

    private DownloadLogAdapter.ItemAccess itemAccess = new DownloadLogAdapter.ItemAccess() {

        @Override
        public int getCount() {
            return (downloadLog != null) ? downloadLog.size() : 0;
        }

        @Override
        public DownloadStatus getItem(int position) {
            return (downloadLog != null) ? downloadLog.get(position) : null;
        }
    };

	private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {
		
		@Override
		public void update(EventDistributor eventDistributor, Integer arg) {
			if ((arg & EventDistributor.DOWNLOADLOG_UPDATE) != 0) {
				loadData();
			}
		}
	};

}
