package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import de.danoeh.antennapod.asynctask.FeedImageLoader;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.R;

/** Displays the cover and the title of a FeedItem. */
public class CoverFragment extends SherlockFragment {
	private static final String TAG = "CoverFragment";
	private static final String ARG_FEED_ID = "arg.feedId";
	private static final String ARG_FEEDITEM_ID = "arg.feedItem";

	private FeedMedia media;

	private TextView txtvTitle;
	private TextView txtvFeed;
	private ImageView imgvCover;

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

		FeedManager manager = FeedManager.getInstance();
		FeedItem item = null;
		Bundle args = getArguments();
		if (args != null) {
			long feedId = args.getLong(ARG_FEED_ID, -1);
			long itemId = args.getLong(ARG_FEEDITEM_ID, -1);
			if (feedId != -1 && itemId != -1) {
				Feed feed = manager.getFeed(feedId);
				item = manager.getFeedItem(itemId, feed);
				media = item.getMedia();
			} else {
				Log.e(TAG, TAG + " was called with invalid arguments");
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.cover_fragment, container, false);
		txtvTitle = (TextView) root.findViewById(R.id.txtvTitle);
		txtvFeed = (TextView) root.findViewById(R.id.txtvFeed);
		imgvCover = (ImageView) root.findViewById(R.id.imgvCover);
		return root;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (media != null) {
			loadMediaInfo();
		} else {
			Log.w(TAG, "Unable to load media info: media was null");
		}
	}

	private void loadMediaInfo() {
		FeedImageLoader.getInstance().loadCoverBitmap(
				media.getItem().getFeed().getImage(), imgvCover);
		txtvTitle.setText(media.getItem().getTitle());
		txtvFeed.setText(media.getItem().getFeed().getTitle());
	}

}
