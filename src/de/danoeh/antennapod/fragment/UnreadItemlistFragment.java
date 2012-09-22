package de.danoeh.antennapod.fragment;

import android.os.Bundle;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.FeedManager;

/** Contains all unread items. */
public class UnreadItemlistFragment extends ItemlistFragment {

	public UnreadItemlistFragment() {
		super(FeedManager.getInstance().getUnreadItems(), true);

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		menu.add(Menu.NONE, R.id.mark_all_read_item, Menu.NONE, getActivity()
				.getString(R.string.mark_all_read_label));
		menu.add(Menu.NONE, R.id.enqueue_all_item, Menu.NONE, getActivity()
				.getString(R.string.enqueue_all_new));
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
		case R.id.enqueue_all_item:
			manager.enqueueAllNewItems(getActivity());
			break;
		default:
			return false;
		}
		return true;
	}

}
