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
import de.danoeh.antennapod.adapter.ChapterListAdapter;
import de.danoeh.antennapod.feed.Chapter;
import de.danoeh.antennapod.feed.FeedMedia;
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
		pagerAdapter = new MediaPlayerPagerAdapter(getSupportFragmentManager());
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
		final FeedMedia media = controller.getMedia();
		if (media != null) {
			if (media.getItem().getChapters() != null
					&& pagerAdapter.getCount() < MediaPlayerPagerAdapter.NUM_ITEMS_WITH_CHAPTERS) {
				pagerAdapter
						.setNumItems(MediaPlayerPagerAdapter.NUM_ITEMS_WITH_CHAPTERS);
			}
			pagerAdapter.notifyDataSetChanged();
		}
	}

	public class MediaPlayerPagerAdapter extends FragmentStatePagerAdapter {
		private int numItems;

		private SherlockListFragment sCChapterFragment;

		private static final int POS_COVER = 0;
		private static final int POS_DESCR = 1;
		private static final int POS_CHAPTERS = 2;

		public static final int NUM_ITEMS_WITH_CHAPTERS = 3;
		public static final int NUM_ITEMS_WITHOUT_CHAPTERS = 2;

		public MediaPlayerPagerAdapter(FragmentManager fm) {
			super(fm);
			numItems = NUM_ITEMS_WITHOUT_CHAPTERS;
			FeedMedia media = AudioplayerActivity.this.controller.getMedia();
			if (media != null && media.getItem().getChapters() != null) {
				numItems = NUM_ITEMS_WITH_CHAPTERS;
			}
		}

		@Override
		public Fragment getItem(int position) {
			FeedMedia media = controller.getMedia();
			if (media != null) {
				switch (position) {
				case POS_COVER:
					AudioplayerActivity.this.coverFragment = CoverFragment
							.newInstance(media.getItem());
					return AudioplayerActivity.this.coverFragment;
				case POS_DESCR:
					AudioplayerActivity.this.descriptionFragment = ItemDescriptionFragment
							.newInstance(media.getItem());
					return AudioplayerActivity.this.descriptionFragment;
				case POS_CHAPTERS:
					sCChapterFragment = new SherlockListFragment() {

						@Override
						public void onListItemClick(ListView l, View v,
								int position, long id) {
							super.onListItemClick(l, v, position, id);
							Chapter chapter = (Chapter) this.getListAdapter()
									.getItem(position);
							controller.seekToChapter(chapter);
						}

					};

					sCChapterFragment.setListAdapter(new ChapterListAdapter(
							AudioplayerActivity.this, 0, media.getItem()
									.getChapters(), media));

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
				return AudioplayerActivity.this.getString(R.string.cover_label);
			case POS_DESCR:
				return AudioplayerActivity.this
						.getString(R.string.shownotes_label);
			case POS_CHAPTERS:
				return AudioplayerActivity.this
						.getString(R.string.chapters_label);
			default:
				return super.getPageTitle(position);
			}
		}

		@Override
		public int getCount() {
			return numItems;
		}

		public void setNumItems(int numItems) {
			this.numItems = numItems;
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
