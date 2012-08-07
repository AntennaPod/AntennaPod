package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.viewpagerindicator.TabPageIndicator;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.SCListAdapter;
import de.danoeh.antennapod.feed.SimpleChapter;
import de.danoeh.antennapod.fragment.CoverFragment;
import de.danoeh.antennapod.fragment.ItemDescriptionFragment;
import de.danoeh.antennapod.service.PlaybackService;

/** Activity for playing audio files. */
public class AudioplayerActivity extends MediaplayerActivity {

	final String TAG = "AudioplayerActivity";

	private CoverFragment coverFragment;
	private ItemDescriptionFragment descriptionFragment;
	ViewPager viewpager;
	TabPageIndicator tabs;
	MediaPlayerPagerAdapter pagerAdapter;
	TextView txtvStatus;

	@Override
	protected void onAwaitingVideoSurface() {
		startActivity(new Intent(this, VideoplayerActivity.class));
	}

	@Override
	protected void postStatusMsg(int resId) {
		txtvStatus.setText(resId);

	}

	@Override
	protected void clearStatusMsg() {
		txtvStatus.setText("");

	}

	@Override
	protected void setupGUI() {
		super.setupGUI();
		txtvStatus = (TextView) findViewById(R.id.txtvStatus);
		viewpager = (ViewPager) findViewById(R.id.viewpager);
		tabs = (TabPageIndicator) findViewById(R.id.tabs);

		int tabcount = 2;
		if (media != null && media.getItem().getSimpleChapters() != null) {
			tabcount = 3;
		}
		pagerAdapter = new MediaPlayerPagerAdapter(getSupportFragmentManager(),
				tabcount, this);
		viewpager.setAdapter(pagerAdapter);
		tabs.setViewPager(viewpager);
	}

	@Override
	protected void onPositionObserverUpdate() {
		super.onPositionObserverUpdate();
		pagerAdapter.notifyMediaPositionChanged();
	}

	@Override
	protected void loadMediaInfo() {
		super.loadMediaInfo();
		if (!mediaInfoLoaded && media != null) {
			pagerAdapter.notifyDataSetChanged();

		}
	}

	public static class MediaPlayerPagerAdapter extends
			FragmentStatePagerAdapter {
		private int numItems;
		private AudioplayerActivity activity;

		private SherlockListFragment sCChapterFragment;

		private static final int POS_COVER = 0;
		private static final int POS_DESCR = 1;
		private static final int POS_CHAPTERS = 2;

		public MediaPlayerPagerAdapter(FragmentManager fm, int numItems,
				AudioplayerActivity activity) {
			super(fm);
			this.numItems = numItems;
			this.activity = activity;
		}

		@Override
		public Fragment getItem(int position) {
			if (activity.media != null) {
				switch (position) {
				case POS_COVER:
					activity.coverFragment = CoverFragment
							.newInstance(activity.media.getItem());
					return activity.coverFragment;
				case POS_DESCR:
					activity.descriptionFragment = ItemDescriptionFragment
							.newInstance(activity.media.getItem());
					return activity.descriptionFragment;
				case POS_CHAPTERS:
					sCChapterFragment = new SherlockListFragment() {

						@Override
						public void onListItemClick(ListView l, View v,
								int position, long id) {
							super.onListItemClick(l, v, position, id);
							SimpleChapter chapter = (SimpleChapter) this
									.getListAdapter().getItem(position);
							if (activity.playbackService != null) {
								activity.playbackService.seekToChapter(chapter);
							}
						}

					};

					sCChapterFragment.setListAdapter(new SCListAdapter(
							activity, 0, activity.media.getItem()
									.getSimpleChapters()));

					return sCChapterFragment;
				default:
					return CoverFragment.newInstance(null);
				}
			} else {
				return CoverFragment.newInstance(null);
			}
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case POS_COVER:
				return activity.getString(R.string.cover_label);
			case POS_DESCR:
				return activity.getString(R.string.shownotes_label);
			case POS_CHAPTERS:
				return activity.getString(R.string.chapters_label);
			default:
				return super.getPageTitle(position);
			}
		}

		@Override
		public int getCount() {
			return numItems;
		}

		@Override
		public int getItemPosition(Object object) {
			return POSITION_UNCHANGED;
		}

		public void notifyMediaPositionChanged() {
			if (sCChapterFragment != null) {
				ArrayAdapter<SimpleChapter> adapter = (ArrayAdapter<SimpleChapter>) sCChapterFragment
						.getListAdapter();
				adapter.notifyDataSetChanged();
			}
		}

	}

	@Override
	protected void onReloadNotification(int notificationCode) {
		if (notificationCode == PlaybackService.EXTRA_CODE_VIDEO) {
			if (AppConfig.DEBUG)
				Log.d(TAG,
						"ReloadNotification received, switching to Videoplayer now");
			startActivity(new Intent(this, VideoplayerActivity.class));

		}
	}

	@Override
	protected void onBufferStart() {
		postStatusMsg(R.string.player_buffering_msg);
	}

	@Override
	protected void onBufferEnd() {
		clearStatusMsg();
	}

}
