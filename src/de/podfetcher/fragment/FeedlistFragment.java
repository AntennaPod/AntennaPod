package de.podfetcher.fragment;

import de.podfetcher.R;
import de.podfetcher.feed.*;
import de.podfetcher.activity.*;
import de.podfetcher.adapter.FeedlistAdapter;
import de.podfetcher.asynctask.FeedRemover;
import de.podfetcher.storage.DownloadRequester;
import de.podfetcher.service.DownloadService;
import de.podfetcher.util.FeedMenuHandler;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import android.util.Log;

public class FeedlistFragment extends SherlockListFragment {
	private static final String TAG = "FeedlistFragment";
	public static final String EXTRA_SELECTED_FEED = "extra.de.podfetcher.activity.selected_feed";

	private FeedManager manager;
	private FeedlistAdapter fla;
	private SherlockFragmentActivity pActivity;

	private Feed selectedFeed;
	private ActionMode mActionMode;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		pActivity = (SherlockFragmentActivity) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		pActivity = null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "Creating");
		manager = FeedManager.getInstance();
		fla = new FeedlistAdapter(pActivity, 0, manager.getFeeds());
		setListAdapter(fla);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.feedlist, container, false);

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				Feed selection = fla.getItem(position);
				Log.d(TAG, "Selected Feed with title " + selection.getTitle());
				if (selection != null) {
					if (mActionMode != null) {
						mActionMode.finish();
					}
					selectedFeed = selection;
					mActionMode = getSherlockActivity().startActionMode(
							mActionModeCallback);

				}
				return true;
			}

		});
	}

	@Override
	public void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadService.ACTION_DOWNLOAD_HANDLED);
		filter.addAction(DownloadService.ACTION_FEED_SYNC_COMPLETED);
		filter.addAction(DownloadRequester.ACTION_DOWNLOAD_QUEUED);

		pActivity.registerReceiver(contentUpdate, filter);
		fla.notifyDataSetChanged();
	}

	@Override
	public void onPause() {
		super.onPause();
		pActivity.unregisterReceiver(contentUpdate);
		if (mActionMode != null) {
			mActionMode.finish();
		}
	}

	private BroadcastReceiver contentUpdate = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "Received contentUpdate Intent.");
			fla.notifyDataSetChanged();
		}
	};

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Feed selection = fla.getItem(position);
		Intent showFeed = new Intent(pActivity, FeedItemlistActivity.class);
		showFeed.putExtra(EXTRA_SELECTED_FEED, selection.getId());

		pActivity.startActivity(showFeed);
	}

	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			FeedMenuHandler.onCreateOptionsMenu(mode.getMenuInflater(), menu);
			mode.setTitle(selectedFeed.getTitle());
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return FeedMenuHandler.onPrepareOptionsMenu(menu, selectedFeed);
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if (FeedMenuHandler.onOptionsItemClicked(getSherlockActivity(),
					item, selectedFeed)) {
				fla.notifyDataSetChanged();
			} else {
				switch (item.getItemId()) {
				case R.id.remove_item:
					FeedRemover remover = new FeedRemover(getSherlockActivity()) {
						@Override
						protected void onPostExecute(Void result) {
							super.onPostExecute(result);
							fla.notifyDataSetChanged();
						}
					};
					remover.execute(selectedFeed);
					break;
				}
			}
			mode.finish();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
			selectedFeed = null;

		}
	};
}
