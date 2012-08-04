package de.danoeh.antennapod.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog.Builder;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.VideoView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;
import com.viewpagerindicator.TabPageIndicator;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.SCListAdapter;
import de.danoeh.antennapod.feed.SimpleChapter;
import de.danoeh.antennapod.fragment.CoverFragment;
import de.danoeh.antennapod.fragment.ItemDescriptionFragment;
import de.danoeh.antennapod.service.PlaybackService;
import de.danoeh.antennapod.service.PlayerStatus;

public class AudioplayerActivity extends MediaplayerActivity implements
		SurfaceHolder.Callback {

	final String TAG = "AudioplayerActivity";

	/** True if video controls are currently visible. */
	private boolean videoControlsShowing = true;
	private VideoControlsHider videoControlsToggler;

	// Widgets
	private CoverFragment coverFragment;
	private ItemDescriptionFragment descriptionFragment;
	ViewPager viewpager;
	TabPageIndicator tabs;
	MediaPlayerPagerAdapter pagerAdapter;
	VideoView videoview;
	TextView txtvStatus;
	LinearLayout videoOverlay;

	@Override
	protected void onPause() {
		super.onPause();
		if (PlaybackService.isRunning && playbackService != null
				&& playbackService.isPlayingVideo()) {
			playbackService.stop();
		}
		if (videoControlsToggler != null) {
			videoControlsToggler.cancel(true);
		}
		finish();
	}

	@Override
	protected void onAwaitingVideoSurface() {
		// TODO Auto-generated method stub

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

	View.OnTouchListener onVideoviewTouched = new View.OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				if (videoControlsToggler != null) {
					videoControlsToggler.cancel(true);
				}
				toggleVideoControlsVisibility();
				if (videoControlsShowing) {
					setupVideoControlsToggler();
				}

				return true;
			} else {
				return false;
			}
		}
	};

	@SuppressLint("NewApi")
	void setupVideoControlsToggler() {
		if (videoControlsToggler != null) {
			videoControlsToggler.cancel(true);
		}
		videoControlsToggler = new VideoControlsHider();
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			videoControlsToggler
					.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			videoControlsToggler.execute();
		}
	}

	private void toggleVideoControlsVisibility() {
		if (videoControlsShowing) {
			getSupportActionBar().hide();
			videoOverlay.setVisibility(View.GONE);
		} else {
			getSupportActionBar().show();
			videoOverlay.setVisibility(View.VISIBLE);
		}
		videoControlsShowing = !videoControlsShowing;
	}

	private boolean holderCreated;

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		holder.setFixedSize(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		holderCreated = true;
		if (AppConfig.DEBUG)
			Log.d(TAG, "Videoview holder created");
		if (status == PlayerStatus.AWAITING_VIDEO_SURFACE) {
			if (playbackService != null) {
				playbackService.setVideoSurface(holder);
			} else {
				Log.e(TAG,
						"Could'nt attach surface to mediaplayer - reference to service was null");
			}
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		holderCreated = false;
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

	// ---------------------- ASYNC TASKS

	/** Hides the videocontrols after a certain period of time. */
	public class VideoControlsHider extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onCancelled() {
			videoControlsToggler = null;
		}

		@Override
		protected void onPostExecute(Void result) {
			videoControlsToggler = null;
		}

		private static final int WAITING_INTERVALL = 5000;
		private static final String TAG = "VideoControlsToggler";

		@Override
		protected void onProgressUpdate(Void... values) {
			if (videoControlsShowing) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Hiding video controls");
				getSupportActionBar().hide();
				videoOverlay.setVisibility(View.GONE);
				videoControlsShowing = false;
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				Thread.sleep(WAITING_INTERVALL);
			} catch (InterruptedException e) {
				return null;
			}
			publishProgress();
			return null;
		}

	}
}
