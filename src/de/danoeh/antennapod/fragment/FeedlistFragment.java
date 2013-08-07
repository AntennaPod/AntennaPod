package de.danoeh.antennapod.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.FeedItemlistActivity;
import de.danoeh.antennapod.adapter.FeedlistAdapter;
import de.danoeh.antennapod.asynctask.FeedRemover;
import de.danoeh.antennapod.dialog.ConfirmationDialog;
import de.danoeh.antennapod.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.util.menuhandler.FeedMenuHandler;

public class FeedlistFragment extends Fragment implements
		ActionMode.Callback, AdapterView.OnItemClickListener,
		AdapterView.OnItemLongClickListener {
	private static final String TAG = "FeedlistFragment";

	private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED
			| EventDistributor.DOWNLOAD_QUEUED
			| EventDistributor.FEED_LIST_UPDATE
			| EventDistributor.UNREAD_ITEMS_UPDATE;
	
	public static final String EXTRA_SELECTED_FEED = "extra.de.danoeh.antennapod.activity.selected_feed";

	private FeedManager manager;
	private FeedlistAdapter fla;

	private Feed selectedFeed;
	private ActionMode mActionMode;

	private GridView gridView;
	private ListView listView;
	private TextView txtvEmpty;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (AppConfig.DEBUG)
			Log.d(TAG, "Creating");
		manager = FeedManager.getInstance();
		fla = new FeedlistAdapter(getActivity());

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
		EventDistributor.getInstance().register(contentUpdate);
		fla.notifyDataSetChanged();
	}

	@Override
	public void onPause() {
		super.onPause();
		EventDistributor.getInstance().unregister(contentUpdate);
		if (mActionMode != null) {
			mActionMode.finish();
		}
	}

	private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {
		
		@Override
		public void update(EventDistributor eventDistributor, Integer arg) {
			if ((EVENTS & arg) != 0) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Received contentUpdate Intent.");
				fla.notifyDataSetChanged();
			}
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
		try {
			if (FeedMenuHandler.onOptionsItemClicked(getActivity(),
					item, selectedFeed)) {
				fla.notifyDataSetChanged();
			} else {
				switch (item.getItemId()) {
				case R.id.remove_item:
					final FeedRemover remover = new FeedRemover(
							getActivity(), selectedFeed) {
						@Override
						protected void onPostExecute(Void result) {
							super.onPostExecute(result);
							fla.notifyDataSetChanged();
						}
					};
					ConfirmationDialog conDialog = new ConfirmationDialog(
							getActivity(), R.string.remove_feed_label,
							R.string.feed_delete_confirmation_msg) {

						@Override
						public void onConfirmButtonPressed(
								DialogInterface dialog) {
							dialog.dismiss();
							remover.executeAsync();
						}
					};
					conDialog.createNewDialog().show();
					break;
				}
			}
		} catch (DownloadRequestException e) {
			e.printStackTrace();
			DownloadRequestErrorDialogCreator.newRequestErrorDialog(
					getActivity(), e.getMessage());
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
		Intent showFeed = new Intent(getActivity(), FeedItemlistActivity.class);
		showFeed.putExtra(EXTRA_SELECTED_FEED, selection.getId());

		getActivity().startActivity(showFeed);
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
			mActionMode = ((ActionBarActivity) getActivity()).startSupportActionMode(FeedlistFragment.this);

		}
		return true;
	}
}
