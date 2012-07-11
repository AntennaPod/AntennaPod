package de.podfetcher.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import de.podfetcher.feed.FeedManager;

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

	private BroadcastReceiver unreadItemsUpdate = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			fila.notifyDataSetChanged();
		}

	};

}
