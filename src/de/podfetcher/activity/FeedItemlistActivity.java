package de.podfetcher.activity;


import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import de.podfetcher.R;
import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedManager;
import de.podfetcher.fragment.FeedItemlistFragment;
import de.podfetcher.fragment.FeedlistFragment;

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
	/*
	public void onButActionClicked(View v) {
		Log.d(TAG, "Button clicked");
	}*/
}
