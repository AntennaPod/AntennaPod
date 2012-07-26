package de.danoeh.antennapod.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.viewpagerindicator.TabPageIndicator;

import de.danoeh.antennapod.asynctask.OpmlExportWorker;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.fragment.FeedlistFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.fragment.UnreadItemlistFragment;
import de.danoeh.antennapod.service.DownloadService;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.StorageUtils;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;

public class MainActivity extends SherlockFragmentActivity {
	private static final String TAG = "MainActivity";

	private FeedManager manager;
	private ViewPager viewpager;
	private MainPagerAdapter pagerAdapter;
	private TabPageIndicator tabs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		StorageUtils.checkStorageAvailability(this);
		manager = FeedManager.getInstance();
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);
		pagerAdapter = new MainPagerAdapter(getSupportFragmentManager(), this);
		
		viewpager = (ViewPager) findViewById(R.id.viewpager);
		tabs = (TabPageIndicator) findViewById(R.id.tabs);
		
		viewpager.setAdapter(pagerAdapter);
		tabs.setViewPager(viewpager);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(contentUpdate);
	}

	@Override
	protected void onResume() {
		super.onResume();
		StorageUtils.checkStorageAvailability(this);
		updateProgressBarVisibility();
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadService.ACTION_DOWNLOAD_HANDLED);
		filter.addAction(DownloadRequester.ACTION_DOWNLOAD_QUEUED);
		registerReceiver(contentUpdate, filter);
	}

	private BroadcastReceiver contentUpdate = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (AppConfig.DEBUG) Log.d(TAG, "Received contentUpdate Intent.");
			updateProgressBarVisibility();
		}
	};

	private void updateProgressBarVisibility() {
		if (DownloadService.isRunning
				&& DownloadRequester.getInstance().isDownloadingFeeds()) {
			setSupportProgressBarIndeterminateVisibility(true);
		} else {
			setSupportProgressBarIndeterminateVisibility(false);
		}
		invalidateOptionsMenu();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
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
		case R.id.opml_import:
			startActivity(new Intent(this, OpmlImportActivity.class));
			return true;
		case R.id.opml_export:
			new OpmlExportWorker(this).executeAsync();
			return false;
		case R.id.search_item:
			onSearchRequested();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem refreshAll = menu.findItem(R.id.all_feed_refresh);
		if (DownloadService.isRunning
				&& DownloadRequester.getInstance().isDownloadingFeeds()) {
			refreshAll.setVisible(false);
		} else {
			refreshAll.setVisible(true);
		}
		
		boolean hasFeeds = !manager.getFeeds().isEmpty();
		menu.findItem(R.id.opml_export).setVisible(hasFeeds);
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.podfetcher, menu);
		return true;
	}

	public static class MainPagerAdapter extends FragmentStatePagerAdapter {
		private static final int NUM_ITEMS = 3;

		private static final int POS_FEEDLIST = 0;
		private static final int POS_NEW_ITEMS = 1;
		private static final int POS_QUEUE = 2;
		
		private Context context;

		public MainPagerAdapter(FragmentManager fm, Context context) {
			super(fm);
			this.context = context;
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case POS_FEEDLIST:
				return new FeedlistFragment();
			case POS_NEW_ITEMS:
				return new UnreadItemlistFragment();
			case POS_QUEUE:
				return new QueueFragment();
			default:
				return null;
			}
		}

		@Override
		public int getCount() {
			return NUM_ITEMS;
		}
		
		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case POS_FEEDLIST:
				return context.getString(R.string.feeds_label);
			case POS_NEW_ITEMS:
				return context.getString(R.string.new_label);
			case POS_QUEUE:
				return context.getString(R.string.queue_label);
			default:
				return null;
			}
		}

	}
}
