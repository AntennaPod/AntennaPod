package de.danoeh.antennapod.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.viewpagerindicator.TabPageIndicator;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.fragment.MiroGuideChannellistFragment;
import de.danoeh.antennapod.preferences.UserPreferences;

/**
 * Shows channels of a category sorted by different criteria in lists. The
 * activity uses MiroGuideChannelListFragments for these lists. If the user
 * selects a channel, the MiroGuideChannelViewActivity is started.
 */
public class MiroGuideCategoryActivity extends SherlockFragmentActivity {
	private static final String TAG = "MiroGuideCategoryActivity";

	public static final String EXTRA_CATEGORY = "category";

	private ViewPager viewpager;
	private CategoryPagerAdapter pagerAdapter;
	private TabPageIndicator tabs;

	private String category;

	@Override
	protected void onCreate(Bundle arg0) {
		setTheme(UserPreferences.getTheme());
		super.onCreate(arg0);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.miroguide_category);

		viewpager = (ViewPager) findViewById(R.id.viewpager);
		tabs = (TabPageIndicator) findViewById(R.id.tabs);

		category = getIntent().getStringExtra(EXTRA_CATEGORY);
		if (category != null) {
			getSupportActionBar().setTitle(category);
			pagerAdapter = new CategoryPagerAdapter(getSupportFragmentManager());
			viewpager.setAdapter(pagerAdapter);
			tabs.setViewPager(viewpager);
		} else {
			Log.e(TAG, "Activity was started with invalid arguments");
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return false;
		}
	}

	public class CategoryPagerAdapter extends FragmentStatePagerAdapter {

		public CategoryPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		private static final int NUM_ITEMS = 2;
		private static final int POS_RATING = 0;
		private static final int POS_POPULAR = 1;

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case POS_RATING:
				return MiroGuideChannellistFragment.newInstance("category",
						category, "rating");
			case POS_POPULAR:
				return MiroGuideChannellistFragment.newInstance("category",
						category, "popular");
			default:
				return null;
			}
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case POS_RATING:
				return getString(R.string.best_rating_label);
			case POS_POPULAR:
				return getString(R.string.popular_label);
			default:
				return null;
			}
		}

		@Override
		public int getCount() {
			return NUM_ITEMS;
		}
	}
}
