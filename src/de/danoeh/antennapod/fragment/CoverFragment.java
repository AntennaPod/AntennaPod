package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragment;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.AudioplayerActivity.AudioplayerContentFragment;
import de.danoeh.antennapod.asynctask.FeedImageLoader;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.feed.FeedMedia;

/** Displays the cover and the title of a FeedItem. */
public class CoverFragment extends SherlockFragment implements
		AudioplayerContentFragment {
	private static final String TAG = "CoverFragment";
	private static final String ARG_FEED_ID = "arg.feedId";
	private static final String ARG_FEEDITEM_ID = "arg.feedItem";

	private FeedMedia media;

	private ImageView imgvCover;

	private boolean viewCreated = false;

	public static CoverFragment newInstance(FeedItem item) {
		CoverFragment f = new CoverFragment();
		if (item != null) {
			Bundle args = new Bundle();
			args.putLong(ARG_FEED_ID, item.getFeed().getId());
			args.putLong(ARG_FEEDITEM_ID, item.getId());
			f.setArguments(args);
		}
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		FeedManager manager = FeedManager.getInstance();
		FeedItem item = null;
		Bundle args = getArguments();
		if (args != null) {
			long feedId = args.getLong(ARG_FEED_ID, -1);
			long itemId = args.getLong(ARG_FEEDITEM_ID, -1);
			if (feedId != -1 && itemId != -1) {
				Feed feed = manager.getFeed(feedId);
				item = manager.getFeedItem(itemId, feed);
				if (item != null) {
					media = item.getMedia();
				}
			} else {
				Log.e(TAG, TAG + " was called with invalid arguments");
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.cover_fragment, container, false);
		imgvCover = (ImageView) root.findViewById(R.id.imgvCover);
		viewCreated = true;
		return root;
	}

	private void loadMediaInfo() {
		if (media != null) {
			imgvCover.post(new Runnable() {

				@Override
				public void run() {
					FeedImageLoader.getInstance().loadCoverBitmap(
							media.getItem().getFeed().getImage(), imgvCover);
				}
			});
		} else {
			Log.w(TAG, "loadMediaInfo was called while media was null");
		}
	}

	@Override
	public void onStart() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "On Start");
		super.onStart();
		if (media != null) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Loading media info");
			loadMediaInfo();
		} else {
			Log.w(TAG, "Unable to load media info: media was null");
		}
	}

	@Override
	public void onDataSetChanged(FeedMedia media) {
		this.media = media;
		if (viewCreated) {
			loadMediaInfo();
		}

	}

}
