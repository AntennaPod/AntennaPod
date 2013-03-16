package de.danoeh.antennapod.activity;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.fragment.ExternalPlayerFragment;
import de.danoeh.antennapod.fragment.FeedlistFragment;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.service.PlaybackService;
import de.danoeh.antennapod.service.download.DownloadService;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.StorageUtils;

/** The activity that is shown when the user launches the app. */
public class MainActivity extends SherlockFragmentActivity {
	private static final String TAG = "MainActivity";

	private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED
			| EventDistributor.DOWNLOAD_QUEUED;

	private FeedManager manager;
	private ViewPager viewpager;
	private TabsAdapter pagerAdapter;
	private ExternalPlayerFragment externalPlayerFragment;

	private static boolean appLaunched = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(UserPreferences.getTheme());
		super.onCreate(savedInstanceState);
		StorageUtils.checkStorageAvailability(this);
		manager = FeedManager.getInstance();
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);

		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		viewpager = (ViewPager) findViewById(R.id.viewpager);
		pagerAdapter = new TabsAdapter(this, viewpager);

		viewpager.setAdapter(pagerAdapter);

		Tab feedsTab = getSupportActionBar().newTab();
		feedsTab.setText(R.string.podcasts_label);
		Tab episodesTab = getSupportActionBar().newTab();
		episodesTab.setText(R.string.episodes_label);

		pagerAdapter.addTab(feedsTab, FeedlistFragment.class, null);
		pagerAdapter.addTab(episodesTab, EpisodesFragment.class, null);

		FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction();
		externalPlayerFragment = new ExternalPlayerFragment();
		transaction.replace(R.id.playerFragment, externalPlayerFragment);
		transaction.commit();

		// executed on application start
		if (!appLaunched && getIntent().getAction() != null
				&& getIntent().getAction().equals(Intent.ACTION_MAIN)) {
			appLaunched = true;
			if (manager.getUnreadItemsSize(true) > 0) {
				// select 'episodes' tab
				getSupportActionBar().setSelectedNavigationItem(1);
			}
		}
		if (savedInstanceState != null) {
			getSupportActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt("tab", 0));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", getSupportActionBar()
				.getSelectedNavigationIndex());
	}

	@Override
	protected void onPause() {
		super.onPause();
		EventDistributor.getInstance().unregister(contentUpdate);
	}

	@Override
	protected void onResume() {
		super.onResume();
		StorageUtils.checkStorageAvailability(this);
		updateProgressBarVisibility();
		EventDistributor.getInstance().register(contentUpdate);

	}

	private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {

		@Override
		public void update(EventDistributor eventDistributor, Integer arg) {
			if ((EVENTS & arg) != 0) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Received contentUpdate Intent.");
				updateProgressBarVisibility();
			}
		}
	};

	private void updateProgressBarVisibility() {
		if (DownloadService.isRunning
				&& DownloadRequester.getInstance().isDownloadingFeeds()) {
			setSupportProgressBarIndeterminateVisibility(true);
		} else {
			setSupportProgressBarIndeterminateVisibility(false);
		}
		supportInvalidateOptionsMenu();
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
			startActivity(PlaybackService.getPlayerActivityIntent(this));
			return true;
		case R.id.search_item:
			onSearchRequested();
			return true;
		case R.id.show_playback_history:
			startActivity(new Intent(this, PlaybackHistoryActivity.class));
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

		boolean hasFeeds = manager.getFeedsSize() > 0;
		menu.findItem(R.id.all_feed_refresh).setVisible(hasFeeds);
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	public static class TabsAdapter extends FragmentPagerAdapter implements
			ActionBar.TabListener, ViewPager.OnPageChangeListener {
		private final Context mContext;
		private final ActionBar mActionBar;
		private final ViewPager mViewPager;
		private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

		static final class TabInfo {
			private final Class<?> clss;
			private final Bundle args;

			TabInfo(Class<?> _class, Bundle _args) {
				clss = _class;
				args = _args;
			}
		}

		public TabsAdapter(MainActivity activity, ViewPager pager) {
			super(activity.getSupportFragmentManager());
			mContext = activity;
			mActionBar = activity.getSupportActionBar();
			mViewPager = pager;
			mViewPager.setAdapter(this);
			mViewPager.setOnPageChangeListener(this);
		}

		public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
			TabInfo info = new TabInfo(clss, args);
			tab.setTag(info);
			tab.setTabListener(this);
			mTabs.add(info);
			mActionBar.addTab(tab);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mTabs.size();
		}

		@Override
		public Fragment getItem(int position) {
			TabInfo info = mTabs.get(position);
			return Fragment.instantiate(mContext, info.clss.getName(),
					info.args);
		}

		@Override
		public void onPageScrolled(int position, float positionOffset,
				int positionOffsetPixels) {
		}

		@Override
		public void onPageSelected(int position) {
			mActionBar.setSelectedNavigationItem(position);
		}

		@Override
		public void onPageScrollStateChanged(int state) {
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			Object tag = tab.getTag();
			for (int i = 0; i < mTabs.size(); i++) {
				if (mTabs.get(i) == tag) {
					mViewPager.setCurrentItem(i);
				}
			}
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {

		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}

}
