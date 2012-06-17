package de.podfetcher.activity;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.podfetcher.R;
import de.podfetcher.adapter.DownloadLogAdapter;
import de.podfetcher.feed.FeedManager;

public class DownloadLogActivity extends SherlockListActivity {
	private static final String TAG = "DownloadLogActivity";
	
	DownloadLogAdapter dla;
	FeedManager manager;
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		manager = FeedManager.getInstance();
		
		dla = new DownloadLogAdapter(this, 0, manager.getDownloadLog());
		setListAdapter(dla);
	}
	
	
	
	
	
	
}
