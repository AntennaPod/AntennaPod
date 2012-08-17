package de.danoeh.antennapod.activity;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.DownloadlistAdapter;
import de.danoeh.antennapod.asynctask.DownloadStatus;
import de.danoeh.antennapod.service.download.DownloadService;
import de.danoeh.antennapod.service.download.Downloader;
import de.danoeh.antennapod.storage.DownloadRequester;

/**
 * Shows all running downloads in a list. The list objects are DownloadStatus
 * objects created by a DownloadObserver.
 */
public class DownloadActivity extends SherlockListActivity implements
		ActionMode.Callback {

	private static final String TAG = "DownloadActivity";
	private static final int MENU_SHOW_LOG = 0;
	private static final int MENU_CANCEL_ALL_DOWNLOADS = 1;
	private DownloadlistAdapter dla;
	private DownloadRequester requester;

	private ActionMode mActionMode;
	private DownloadStatus selectedDownload;

	private DownloadService downloadService = null;
	boolean mIsBound;

	private AsyncTask<Void, Void, Void> contentRefresher;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (AppConfig.DEBUG)
			Log.d(TAG, "Creating Activity");
		requester = DownloadRequester.getInstance();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unbindService(mConnection);
		unregisterReceiver(contentChanged);
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(contentChanged, new IntentFilter(
				DownloadService.ACTION_DOWNLOADS_CONTENT_CHANGED));
		bindService(new Intent(this, DownloadService.class), mConnection, 0);
		startContentRefresher();
		if (dla != null) {
			dla.notifyDataSetChanged();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (AppConfig.DEBUG)
			Log.d(TAG, "Stopping Activity");
		stopContentRefresher();
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceDisconnected(ComponentName className) {
			downloadService = null;
			mIsBound = false;
			Log.i(TAG, "Closed connection with DownloadService.");
		}

		public void onServiceConnected(ComponentName name, IBinder service) {
			downloadService = ((DownloadService.LocalBinder) service)
					.getService();
			mIsBound = true;
			if (AppConfig.DEBUG)
				Log.d(TAG, "Connection to service established");
			dla = new DownloadlistAdapter(DownloadActivity.this, 0,
					downloadService.getDownloads());
			setListAdapter(dla);
			dla.notifyDataSetChanged();
		}
	};

	@SuppressLint("NewApi")
	private void startContentRefresher() {
		if (contentRefresher != null) {
			contentRefresher.cancel(true);
		}
		contentRefresher = new AsyncTask<Void, Void, Void>() {
			private final int WAITING_INTERVALL = 1000;

			@Override
			protected void onProgressUpdate(Void... values) {
				super.onProgressUpdate(values);
				if (dla != null) {
					if (AppConfig.DEBUG)
						Log.d(TAG, "Refreshing content automatically");
					dla.notifyDataSetChanged();
				}
			}

			@Override
			protected Void doInBackground(Void... params) {
				while (!isCancelled()) {
					try {
						Thread.sleep(WAITING_INTERVALL);
						publishProgress();
					} catch (InterruptedException e) {
						return null;
					}
				}
				return null;
			}
		};
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			contentRefresher.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			contentRefresher.execute();
		}
	}

	private void stopContentRefresher() {
		if (contentRefresher != null) {
			contentRefresher.cancel(true);
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view,
					int position, long id) {
				DownloadStatus selection = dla.getItem(position).getStatus();
				if (selection != null && mActionMode != null) {
					mActionMode.finish();
				}
				dla.setSelectedItemIndex(position);
				selectedDownload = selection;
				mActionMode = startActionMode(DownloadActivity.this);
				return true;
			}

		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_SHOW_LOG, Menu.NONE,
				R.string.show_download_log).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(Menu.NONE, MENU_CANCEL_ALL_DOWNLOADS, Menu.NONE,
				R.string.cancel_all_downloads_label).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		case MENU_SHOW_LOG:
			startActivity(new Intent(this, DownloadLogActivity.class));
			break;
		case MENU_CANCEL_ALL_DOWNLOADS:
			requester.cancelAllDownloads(this);
			break;
		}
		return true;
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		if (!selectedDownload.isDone()) {
			menu.add(Menu.NONE, R.id.cancel_download_item, Menu.NONE,
					R.string.cancel_download_label).setIcon(
					R.drawable.navigation_cancel);
		}
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		boolean handled = false;
		switch (item.getItemId()) {
		case R.id.cancel_download_item:
			requester.cancelDownload(this, selectedDownload.getFeedFile());
			handled = true;
			break;
		}
		mActionMode.finish();
		return handled;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		mActionMode = null;
		selectedDownload = null;
		dla.setSelectedItemIndex(DownloadlistAdapter.SELECTION_NONE);
	}

	private BroadcastReceiver contentChanged = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (dla != null) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Refreshing content");
				dla.notifyDataSetChanged();
			}
		}
	};

}
