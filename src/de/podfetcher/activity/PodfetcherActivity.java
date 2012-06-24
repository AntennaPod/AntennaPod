package de.podfetcher.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.podfetcher.R;
import de.podfetcher.feed.FeedManager;
import de.podfetcher.fragment.FeedItemlistFragment;
import de.podfetcher.fragment.FeedlistFragment;
import de.podfetcher.fragment.QueueFragment;
import de.podfetcher.fragment.UnreadItemlistFragment;

public class PodfetcherActivity extends SherlockFragmentActivity {
	private static final String TAG = "PodfetcherActivity";

	private FeedManager manager;
	
	private FeedlistFragment feedlist;
	FeedItemlistFragment unreadList;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		manager = FeedManager.getInstance();
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
