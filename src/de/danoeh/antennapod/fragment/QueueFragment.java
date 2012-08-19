package de.danoeh.antennapod.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.R;

public class QueueFragment extends ItemlistFragment {
	private static final String TAG = "QueueFragment";
	
	public QueueFragment() {
		super(FeedManager.getInstance().getQueue(), true);
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		super.onCreateActionMode(mode, menu);
		menu.add(Menu.NONE, R.id.move_up_item, Menu.NONE,
				R.string.move_up_label);
		menu.add(Menu.NONE, R.id.move_down_item, Menu.NONE,
				R.string.move_down_label);
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		boolean handled = false;
		switch (item.getItemId()) {
		case R.id.move_up_item:
			manager.moveQueueItem(getActivity(), selectedItem, -1);
			handled = true;
			break;
		case R.id.move_down_item:
			manager.moveQueueItem(getActivity(), selectedItem, 1);
			handled = true;
			break;
		default:
			handled = super.onActionItemClicked(mode, item);
		}
		fila.notifyDataSetChanged();
		mode.finish();
		return handled;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		menu.add(Menu.NONE, R.id.clear_queue_item, Menu.NONE, getActivity()
				.getString(R.string.clear_queue_label));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.clear_queue_item:
			manager.clearQueue(getActivity());
			break;
		default:
			return false;
		}
		return true;
	}

}
