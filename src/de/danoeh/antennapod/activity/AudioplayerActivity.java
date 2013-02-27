package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Window;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.ChapterListAdapter;
import de.danoeh.antennapod.asynctask.ImageLoader;
import de.danoeh.antennapod.feed.Chapter;
import de.danoeh.antennapod.feed.SimpleChapter;
import de.danoeh.antennapod.fragment.CoverFragment;
import de.danoeh.antennapod.fragment.ItemDescriptionFragment;
import de.danoeh.antennapod.service.PlaybackService;
import de.danoeh.antennapod.util.playback.Playable;

/** Activity for playing audio files. */
public class AudioplayerActivity extends MediaplayerActivity {
	private static final int POS_COVER = 0;
	private static final int POS_DESCR = 1;
	private static final int POS_CHAPTERS = 2;
	private static final int NUM_CONTENT_FRAGMENTS = 3;

	final String TAG = "AudioplayerActivity";

	private Fragment[] detachedFragments;

	private CoverFragment coverFragment;
	private ItemDescriptionFragment descriptionFragment;
	private SherlockListFragment chapterFragment;

	private Fragment currentlyShownFragment;
	private int currentlyShownPosition = -1;

	private TextView txtvTitle;
	private TextView txtvFeed;
	private ImageButton butNavLeft;
	private ImageButton butNavRight;

	private void resetFragmentView() {
		currentlyShownFragment = null;
		coverFragment = null;
		descriptionFragment = null;
		chapterFragment = null;
		currentlyShownPosition = -1;
		detachedFragments = new Fragment[NUM_CONTENT_FRAGMENTS];
	}

	@Override
	protected void onStop() {
		super.onStop();
		resetFragmentView();

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		detachedFragments = new Fragment[NUM_CONTENT_FRAGMENTS];
	}

	@Override
	protected void onAwaitingVideoSurface() {
		startActivity(new Intent(this, VideoplayerActivity.class));
	}

	@Override
	protected void postStatusMsg(int resId) {
		setSupportProgressBarIndeterminateVisibility(resId == R.string.player_preparing_msg
				|| resId == R.string.player_seeking_msg
				|| resId == R.string.player_buffering_msg);
	}

	@Override
	protected void clearStatusMsg() {
		setSupportProgressBarIndeterminateVisibility(false);
	}

	/**
	 * Changes the currently displayed fragment.
	 * 
	 * @param Must
	 *            be POS_COVER, POS_DESCR, or POS_CHAPTERS
	 * */
	private void switchToFragment(int pos) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Switching contentView to position " + pos);
		if (currentlyShownPosition != pos) {
			Playable media = controller.getMedia();
			if (media != null) {
				FragmentTransaction ft = getSupportFragmentManager()
						.beginTransaction();
				if (currentlyShownFragment != null) {
					detachedFragments[currentlyShownPosition] = currentlyShownFragment;
					ft.detach(currentlyShownFragment);
				}
				switch (pos) {
				case POS_COVER:
					if (coverFragment == null) {
						Log.i(TAG, "Using new coverfragment");
						coverFragment = CoverFragment.newInstance(media);
					}
					currentlyShownFragment = coverFragment;
					break;
				case POS_DESCR:
					if (descriptionFragment == null) {
						descriptionFragment = ItemDescriptionFragment
								.newInstance(media);
					}
					currentlyShownFragment = descriptionFragment;
					break;
				case POS_CHAPTERS:
					if (chapterFragment == null) {
						chapterFragment = new SherlockListFragment() {

							@Override
							public void onListItemClick(ListView l, View v,
									int position, long id) {
								super.onListItemClick(l, v, position, id);
								Chapter chapter = (Chapter) this
										.getListAdapter().getItem(position);
								controller.seekToChapter(chapter);
							}

						};
						chapterFragment.setListAdapter(new ChapterListAdapter(
								AudioplayerActivity.this, 0, media
										.getChapters(), media));
					}
					currentlyShownFragment = chapterFragment;
					break;
				}
				if (currentlyShownFragment != null) {
					currentlyShownPosition = pos;
					if (detachedFragments[pos] != null) {
						if (AppConfig.DEBUG)
							Log.d(TAG, "Reattaching fragment at position "
									+ pos);
						ft.attach(detachedFragments[pos]);
					} else {
						ft.add(R.id.contentView, currentlyShownFragment);
					}
					ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
					ft.commit();
					updateNavButtonDrawable();
				}
			}
		}
	}

	private void updateNavButtonDrawable() {
		TypedArray drawables = obtainStyledAttributes(new int[] {
				R.attr.navigation_shownotes, R.attr.navigation_chapters });
		final Playable media = controller.getMedia();
		if (butNavLeft != null && butNavRight != null && media != null) {
			switch (currentlyShownPosition) {
			case POS_COVER:
				butNavLeft.setScaleType(ScaleType.CENTER);
				butNavLeft.setImageDrawable(drawables.getDrawable(0));
				butNavRight.setImageDrawable(drawables.getDrawable(1));
				break;
			case POS_DESCR:
				butNavLeft.setScaleType(ScaleType.CENTER_CROP);
				butNavLeft.post(new Runnable() {

					@Override
					public void run() {
						ImageLoader.getInstance().loadThumbnailBitmap(
								media.getImageFileUrl(), butNavLeft);
					}
				});
				butNavRight.setImageDrawable(drawables.getDrawable(1));
				break;
			case POS_CHAPTERS:
				butNavLeft.setScaleType(ScaleType.CENTER_CROP);
				butNavLeft.post(new Runnable() {

					@Override
					public void run() {
						ImageLoader.getInstance().loadThumbnailBitmap(
								media.getImageFileUrl(),
								butNavLeft);
					}
				});
				butNavRight.setImageDrawable(drawables.getDrawable(0));
				break;
			}
		}
	}

	@Override
	protected void setupGUI() {
		super.setupGUI();
		resetFragmentView();
		txtvTitle = (TextView) findViewById(R.id.txtvTitle);
		txtvFeed = (TextView) findViewById(R.id.txtvFeed);
		butNavLeft = (ImageButton) findViewById(R.id.butNavLeft);
		butNavRight = (ImageButton) findViewById(R.id.butNavRight);

		butNavLeft.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (currentlyShownFragment == null
						|| currentlyShownPosition == POS_DESCR) {
					switchToFragment(POS_COVER);
				} else if (currentlyShownPosition == POS_COVER) {
					switchToFragment(POS_DESCR);
				} else if (currentlyShownPosition == POS_CHAPTERS) {
					switchToFragment(POS_COVER);
				}
			}
		});

		butNavRight.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (currentlyShownPosition == POS_CHAPTERS) {
					switchToFragment(POS_DESCR);
				} else {
					switchToFragment(POS_CHAPTERS);
				}
			}
		});
	}

	@Override
	protected void onPositionObserverUpdate() {
		super.onPositionObserverUpdate();
		notifyMediaPositionChanged();
	}

	@Override
	protected void loadMediaInfo() {
		super.loadMediaInfo();
		final Playable media = controller.getMedia();
		if (media != null) {
			txtvTitle.setText(media.getEpisodeTitle());
			txtvFeed.setText(media.getFeedTitle());
			if (media.getChapters() != null) {
				butNavRight.setVisibility(View.VISIBLE);
			} else {
				butNavRight.setVisibility(View.GONE);
			}

		}
		if (currentlyShownPosition == -1) {
			switchToFragment(POS_COVER);
		}
		if (currentlyShownFragment instanceof AudioplayerContentFragment) {
			((AudioplayerContentFragment) currentlyShownFragment)
					.onDataSetChanged(media);
		}

	}

	public void notifyMediaPositionChanged() {
		if (chapterFragment != null) {
			ArrayAdapter<SimpleChapter> adapter = (ArrayAdapter<SimpleChapter>) chapterFragment
					.getListAdapter();
			adapter.notifyDataSetChanged();
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

	public interface AudioplayerContentFragment {
		public void onDataSetChanged(Playable media);
	}

}
