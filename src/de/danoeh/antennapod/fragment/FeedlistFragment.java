package de.danoeh.antennapod.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.FeedItemlistActivity;
import de.danoeh.antennapod.adapter.FeedlistAdapter;
import de.danoeh.antennapod.asynctask.FeedRemover;
import de.danoeh.antennapod.dialog.ConfirmationDialog;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.service.download.DownloadService;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.menuhandler.FeedMenuHandler;

public class FeedlistFragment extends SherlockFragment implements
		ActionMode.Callback, AdapterView.OnItemClickListener,
		AdapterView.OnItemLongClickListener {
	private static final String TAG = "FeedlistFragment";
	public static final String EXTRA_SELECTED_FEED = "extra.de.danoeh.antennapod.activity.selected_feed";

	private FeedManager manager;
	private FeedlistAdapter fla;
	private SherlockFragmentActivity pActivity;

	private Feed selectedFeed;
	private ActionMode mActionMode;

	private GridView gridView;
	private ListView listView;
	private TextView txtvEmpty;

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
		if (AppConfig.DEBUG)
			Log.d(TAG, "Creating");
		manager = FeedManager.getInstance();
		fla = new FeedlistAdapter(pActivity, 0, manager.getFeeds());

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View result = inflater.inflate(R.layout.feedlist, container, false);
		listView = (ListView) result.findViewById(android.R.id.list);
		gridView = (GridView) result.findViewById(R.id.grid);
		txtvEmpty = (TextView) result.findViewById(android.R.id.empty);

		return result;

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (listView != null) {
			listView.setOnItemClickListener(this);
			listView.setOnItemLongClickListener(this);
			listView.setAdapter(fla);
			listView.setEmptyView(txtvEmpty);
			if (AppConfig.DEBUG)
				Log.d(TAG, "Using ListView");
		} else {
			gridView.setOnItemClickListener(this);
			gridView.setOnItemLongClickListener(this);
			gridView.setAdapter(fla);
			gridView.setEmptyView(txtvEmpty);
			if (AppConfig.DEBUG)
				Log.d(TAG, "Using GridView");
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (AppConfig.DEBUG)
			Log.d(TAG, "Resuming");
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadRequester.ACTION_DOWNLOAD_QUEUED);
		filter.addAction(FeedManager.ACTION_UNREAD_ITEMS_UPDATE);
		filter.addAction(FeedManager.ACITON_FEED_LIST_UPDATE);
		filter.addAction(DownloadService.ACTION_DOWNLOAD_HANDLED);
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
		public void onReceive(Context context, final Intent intent) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Received contentUpdate Intent.");
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					fla.notifyDataSetChanged();
				}
			});
		}
	};

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

	@SuppressLint("NewApi")
	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		if (FeedMenuHandler.onOptionsItemClicked(getSherlockActivity(), item,
				selectedFeed)) {
			fla.notifyDataSetChanged();
		} else {
			switch (item.getItemId()) {
			case R.id.remove_item:
				final FeedRemover remover = new FeedRemover(getSherlockActivity(), selectedFeed) {
					@Override
					protected void onPostExecute(Void result) {
						super.onPostExecute(result);
						fla.notifyDataSetChanged();
					}
				};
				ConfirmationDialog conDialog = new ConfirmationDialog(getActivity(), R.string.remove_feed_label, R.string.feed_delete_confirmation_msg){

					@Override
					public void onConfirmButtonPressed(DialogInterface dialog) {		
						dialog.dismiss();
						remover.executeAsync();
					}};
				conDialog.createNewDialog().show();
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
		fla.setSelectedItemIndex(FeedlistAdapter.SELECTION_NONE);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position,
			long id) {
		Feed selection = fla.getItem(position);
		Intent showFeed = new Intent(pActivity, FeedItemlistActivity.class);
		showFeed.putExtra(EXTRA_SELECTED_FEED, selection.getId());

		pActivity.startActivity(showFeed);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		Feed selection = fla.getItem(position);
		if (AppConfig.DEBUG)
			Log.d(TAG, "Selected Feed with title " + selection.getTitle());
		if (selection != null) {
			if (mActionMode != null) {
				mActionMode.finish();
			}
			fla.setSelectedItemIndex(position);
			selectedFeed = selection;
			mActionMode = getSherlockActivity().startActionMode(
					FeedlistFragment.this);

		}
		return true;
	}
}
