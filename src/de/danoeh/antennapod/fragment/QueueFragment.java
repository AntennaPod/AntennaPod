package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.AbstractFeedItemlistAdapter;
import de.danoeh.antennapod.adapter.ExternalFeedItemlistAdapter;
import de.danoeh.antennapod.adapter.FeedItemlistAdapter;
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

	protected AbstractFeedItemlistAdapter createListAdapter() {
		return new ExternalFeedItemlistAdapter(getActivity(), 0, items,
				adapterCallback);
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
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		View headerView = getLayoutInflater(savedInstanceState).inflate(R.layout.feeditemlist_header, null);
		TextView headerTitle = (TextView) headerView.findViewById(R.id.txtvHeaderTitle);
		headerTitle.setText(R.string.queue_label);
		headerView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
			}
		});
		getListView().addHeaderView(headerView);
		super.onViewCreated(view, savedInstanceState);
	}

}
