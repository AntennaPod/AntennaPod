package de.danoeh.antennapod.fragment;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.R;

/** Contains all unread items. */
public class UnreadItemlistFragment extends ItemlistFragment {

	public UnreadItemlistFragment() {
		super(FeedManager.getInstance().getUnreadItems(), true);

	}

	@Override
	public void onPause() {
		super.onPause();
		try {
			getActivity().unregisterReceiver(unreadItemsUpdate);
		} catch (IllegalArgumentException e) {

		}
	}

	@Override
	public void onResume() {
		super.onResume();
		getActivity().registerReceiver(unreadItemsUpdate,
				new IntentFilter(FeedManager.ACTION_UNREAD_ITEMS_UPDATE));
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	private BroadcastReceiver unreadItemsUpdate = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			fila.notifyDataSetChanged();
		}

	};

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		menu.add(Menu.NONE, R.id.mark_all_read_item, Menu.NONE, getActivity()
				.getString(R.string.mark_all_read_label));
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.mark_all_read_item:
			manager.markAllItemsRead(getActivity());
			break;
		default:
			return false;
		}
		return true;
	}

}
