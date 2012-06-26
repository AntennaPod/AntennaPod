package de.podfetcher.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.view.Window;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.podfetcher.R;
import de.podfetcher.feed.FeedManager;
import de.podfetcher.fragment.ItemlistFragment;
import de.podfetcher.fragment.FeedlistFragment;
import de.podfetcher.fragment.QueueFragment;
import de.podfetcher.fragment.UnreadItemlistFragment;
import de.podfetcher.service.DownloadService;
import de.podfetcher.storage.DownloadRequester;

public class PodfetcherActivity extends SherlockFragmentActivity {
	private static final String TAG = "PodfetcherActivity";

	private FeedManager manager;
	
	private FeedlistFragment feedlist;
	ItemlistFragment unreadList;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		manager = FeedManager.getInstance();
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);
		// Set up tabs
		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(false);

		Tab tab = actionBar
				.newTab()
				.setText(getText(R.string.feeds_label).toString())
				.setTabListener(
						new TabListener<FeedlistFragment>(this, getText(
								R.string.feeds_label).toString(),
								FeedlistFragment.class));

		actionBar.addTab(tab);
		
		tab = actionBar
				.newTab()
				.setText(getText(R.string.new_label).toString())
				.setTabListener(
						new TabListener<UnreadItemlistFragment>(this, getText(
								R.string.new_label).toString(),
								UnreadItemlistFragment.class));
		actionBar.addTab(tab);
		
		tab = actionBar
				.newTab()
				.setText(getText(R.string.queue_label).toString())
				.setTabListener(
						new TabListener<QueueFragment>(this, getText(
								R.string.queue_label).toString(),
								QueueFragment.class));
		actionBar.addTab(tab);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(contentUpdate);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateProgressBarVisibility();
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadService.ACTION_DOWNLOAD_HANDLED);
		filter.addAction(DownloadRequester.ACTION_DOWNLOAD_QUEUED);
		registerReceiver(contentUpdate, filter);
	}
	
	private BroadcastReceiver contentUpdate = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "Received contentUpdate Intent.");
			updateProgressBarVisibility();
		}
	};
	
	private void updateProgressBarVisibility() {
		if (DownloadService.isRunning && DownloadRequester.getInstance().isDownloadingFeeds()) {
			setSupportProgressBarIndeterminateVisibility(true);
		} else {
			setSupportProgressBarIndeterminateVisibility(false);
		}
		invalidateOptionsMenu();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch(item.getItemId()) {
	        case R.id.add_feed:
	            startActivity(new Intent(this, AddFeedActivity.class));
				return true;
			case R.id.all_feed_refresh:
				manager.refreshAllFeeds(this);
				return true;
            case R.id.show_downloads:
                startActivity(new Intent(this, DownloadActivity.class));
                return true;
            case R.id.show_preferences:
            	startActivity(new Intent(this, PreferenceActivity.class));
            	return true;
            case R.id.show_player:
            	startActivity(new Intent(this, MediaplayerActivity.class));
            	return true;
			default:
			    return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem refreshAll = menu.findItem(R.id.all_feed_refresh);
		if (DownloadService.isRunning && DownloadRequester.getInstance().isDownloadingFeeds()) {
			refreshAll.setVisible(false);
		} else {
			refreshAll.setVisible(true);
		}
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = new MenuInflater(this);
	    inflater.inflate(R.menu.podfetcher, menu);
	    return true;
	}

	/** TabListener for navigating between the main lists. */
	private class TabListener<T extends Fragment> implements
			ActionBar.TabListener {

		private final Activity activity;
		private final String tag;
		private final Class<T> fClass;
		private Fragment fragment;

		public TabListener(Activity activity, String tag, Class<T> fClass) {
			this.activity = activity;
			this.tag = tag;
			this.fClass = fClass;
		}

		@SuppressWarnings("unused")
		public TabListener(Activity activity, String tag, Fragment fragment,
				Class<T> fClass) {
			super();
			this.activity = activity;
			this.tag = tag;
			this.fragment = fragment;
			this.fClass = fClass;
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			if (fragment == null) {
				fragment = Fragment.instantiate(activity, fClass.getName());
				ft.replace(R.id.main_fragment, fragment);
			} else {
				ft.attach(fragment);
			}
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			if (fragment != null) {
				ft.detach(fragment);
			}
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// Do nothing
		}
	}
}
