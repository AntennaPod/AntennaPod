package de.podfetcher.activity;

import de.podfetcher.R;
import de.podfetcher.storage.DownloadRequester;
import de.podfetcher.adapter.DownloadlistAdapter;
import de.podfetcher.asynctask.DownloadObserver;
import de.podfetcher.asynctask.DownloadStatus;
import de.podfetcher.feed.FeedMedia;
import de.podfetcher.feed.FeedFile;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;

/** Shows all running downloads in a list */
public class DownloadActivity extends SherlockListActivity implements
		ActionMode.Callback {

	private static final String TAG = "DownloadActivity";
	private static final int MENU_SHOW_LOG = 0;
	private static final int MENU_CANCEL_ALL_DOWNLOADS = 1;
	private DownloadlistAdapter dla;
	private DownloadRequester requester;

	private ActionMode mActionMode;
	private DownloadStatus selectedDownload;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requester = DownloadRequester.getInstance();
		observer.execute(requester.getDownloads().toArray(
				new FeedFile[requester.getDownloads().size()]));

	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "Stopping Activity");
		observer.cancel(true);
	}

	private final DownloadObserver observer = new DownloadObserver(this) {
		@Override
		protected void onProgressUpdate(DownloadStatus... values) {

			dla = new DownloadlistAdapter(getContext(), 0, getStatusList());
			setListAdapter(dla);
			dla.notifyDataSetChanged();

		}
	};

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view,
					int position, long id) {
				DownloadStatus selection = dla.getItem(position);
				if (selection != null && mActionMode != null) {
					mActionMode.finish();
				}
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
			requester.cancelDownload(this, selectedDownload.getFeedFile()
					.getDownloadId());
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
	}
}
