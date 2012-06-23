package de.podfetcher.activity;


import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.podfetcher.R;
import de.podfetcher.asynctask.FeedRemover;
import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedManager;
import de.podfetcher.fragment.FeedItemlistFragment;
import de.podfetcher.fragment.FeedlistFragment;
import de.podfetcher.util.FeedMenuHandler;

/** Displays a List of FeedItems */
public class FeedItemlistActivity extends SherlockFragmentActivity {
	private static final String TAG = "FeedItemlistActivity";

	private FeedManager manager;

	/** The feed which the activity displays */
	private Feed feed;
	private FeedItemlistFragment filf;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.feeditemlist_activity);
		manager = FeedManager.getInstance();
		long feedId = getIntent().getLongExtra(FeedlistFragment.EXTRA_SELECTED_FEED, -1);
		if(feedId == -1) Log.e(TAG, "Received invalid feed selection.");

		feed = manager.getFeed(feedId);

		setTitle(feed.getTitle());
		
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fT = fragmentManager.beginTransaction();
		filf = new FeedItemlistFragment(feed.getItems());
		fT.add(R.id.feeditemlistFragment, filf);
		fT.commit();
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return FeedMenuHandler.onCreateOptionsMenu(new MenuInflater(this), menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return FeedMenuHandler.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (FeedMenuHandler.onOptionsItemClicked(this, item, feed)) {
			filf.getListAdapter().notifyDataSetChanged();
		} else {
			switch(item.getItemId()) {
			case R.id.remove_item:
				FeedRemover remover = new FeedRemover(this) {
					@Override
					protected void onPostExecute(Void result) {
						super.onPostExecute(result);
						finish();
					}
				};
				remover.execute(feed);
				break;
			}
		}
		return true;
	}
	
}
