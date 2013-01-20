package de.danoeh.antennapod.fragment;

import android.os.Bundle;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.FeedManager;

public class QueueFragment extends ItemlistFragment {
	private static final String TAG = "QueueFragment";

	public QueueFragment() {
		super(FeedManager.getInstance().getQueue(), true);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		menu.add(Menu.NONE, R.id.clear_queue_item, Menu.NONE, getActivity()
				.getString(R.string.clear_queue_label));
		menu.add(Menu.NONE, R.id.download_all_item, Menu.NONE, getActivity()
				.getString(R.string.download_all));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.clear_queue_item:
			manager.clearQueue(getActivity());
			break;
		case R.id.download_all_item:
			manager.downloadAllItemsInQueue(getActivity());
			fila.notifyDataSetChanged();
			break;
		default:
			return false;
		}
		return true;
	}

}
