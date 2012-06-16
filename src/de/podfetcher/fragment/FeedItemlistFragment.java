package de.podfetcher.fragment;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;

import de.podfetcher.activity.ItemviewActivity;
import de.podfetcher.adapter.FeedItemlistAdapter;
import de.podfetcher.feed.FeedItem;
import de.podfetcher.feed.FeedManager;

public class FeedItemlistFragment extends SherlockListFragment {
	private static final String TAG = "FeedItemlistFragment";
	public static final String EXTRA_SELECTED_FEEDITEM = "extra.de.podfetcher.activity.selected_feeditem";
	
	private FeedItemlistAdapter fila;
	private FeedManager manager;

	/** The feed which the activity displays */
	private ArrayList<FeedItem> items;
	
	public FeedItemlistFragment(ArrayList<FeedItem> items) {
		super();
		this.items = items;
		manager = FeedManager.getInstance();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		fila = new FeedItemlistAdapter(getActivity(), 0, items);
		setListAdapter(fila);

		
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		FeedItem selection = fila.getItem(position);
		Intent showItem = new Intent(getActivity(), ItemviewActivity.class);
		showItem.putExtra(FeedlistFragment.EXTRA_SELECTED_FEED, selection.getFeed().getId());
		showItem.putExtra(EXTRA_SELECTED_FEEDITEM, selection.getId());

		startActivity(showItem);
	}
	
	public void onButActionClicked(View v) {
		Log.d(TAG, "Button clicked");
	}
}
