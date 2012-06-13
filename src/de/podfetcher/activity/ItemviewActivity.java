package de.podfetcher.activity;

import java.text.DateFormat;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

import de.podfetcher.R;
import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedItem;
import de.podfetcher.feed.FeedManager;
import de.podfetcher.feed.FeedMedia;
import de.podfetcher.fragment.FeedlistFragment;
import de.podfetcher.service.DownloadObserver;
import de.podfetcher.service.PlaybackService;
import de.podfetcher.storage.DownloadRequester;

/** Displays a single FeedItem and provides various actions */
public class ItemviewActivity extends SherlockActivity {
	private static final String TAG = "ItemviewActivity";

	private FeedManager manager;
	private DownloadRequester requester;
	private FeedItem item;

	// Widgets
	private ImageView imgvImage;
	private TextView txtvTitle;
	private TextView txtvPublished;
	private Button butPlay;
	private Button butDownload;
	private Button butRemove;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		manager = FeedManager.getInstance();
		extractFeeditem();
		populateUI();
		getDownloadStatus();

		butDownload.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				requester = DownloadRequester.getInstance();
				requester.downloadMedia(v.getContext(), item.getMedia());
				getDownloadStatus();
			}
		});

		butPlay.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Start playback Service
				Intent launchIntent = new Intent(v.getContext(),
						PlaybackService.class);
				launchIntent.putExtra(PlaybackService.EXTRA_MEDIA_ID, item
						.getMedia().getId());
				launchIntent.putExtra(PlaybackService.EXTRA_FEED_ID, item
						.getFeed().getId());
				v.getContext().startService(launchIntent);

				// Launch Mediaplayer
				Intent playerIntent = new Intent(v.getContext(),
						MediaplayerActivity.class);
				v.getContext().startActivity(playerIntent);
			}
		});
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d(TAG, "Stopping Activity");
		downloadObserver.cancel(true);
	}

	/** Extracts FeedItem object the activity is supposed to display */
	private void extractFeeditem() {
		long itemId = getIntent().getLongExtra(
				FeedItemlistActivity.EXTRA_SELECTED_FEEDITEM, -1);
		long feedId = getIntent().getLongExtra(
				FeedlistFragment.EXTRA_SELECTED_FEED, -1);
		if (itemId == -1 || feedId == -1) {
			Log.e(TAG, "Received invalid selection of either feeditem or feed.");
		}
		Feed feed = manager.getFeed(feedId);
		item = manager.getFeedItem(itemId, feed);
		Log.d(TAG, "Title of item is " + item.getTitle());
		Log.d(TAG, "Title of feed is " + item.getFeed().getTitle());
	}

	private void populateUI() {
		setContentView(R.layout.feeditemview);
		txtvTitle = (TextView) findViewById(R.id.txtvItemname);
		txtvPublished = (TextView) findViewById(R.id.txtvPublished);
		imgvImage = (ImageView) findViewById(R.id.imgvFeedimage);
		butPlay = (Button) findViewById(R.id.butPlay);
		butDownload = (Button) findViewById(R.id.butDownload);
		butRemove = (Button) findViewById(R.id.butRemove);

		setTitle(item.getFeed().getTitle());

		txtvPublished.setText(DateUtils.formatSameDayTime(item.getPubDate()
				.getTime(), System.currentTimeMillis(), DateFormat.MEDIUM,
				DateFormat.SHORT));
		txtvTitle.setText(item.getTitle());
		if (item.getFeed().getImage() != null) {
			imgvImage
					.setImageBitmap(item.getFeed().getImage().getImageBitmap());
		}
	}

	private void getDownloadStatus() {
		FeedMedia media = item.getMedia();
		if (media.getFile_url() == null) {
			setNotDownloadedState();
		} else if (media.isDownloaded()) {
			setDownloadedState();
		} else {
			// observe
			setDownloadingState();
			downloadObserver.execute(media);
		}
	}

	final DownloadObserver downloadObserver = new DownloadObserver(this) {
		@Override
		protected void onProgressUpdate(
				DownloadObserver.DownloadStatus... values) {

		}

		@Override
		protected void onPostExecute(Boolean result) {
			boolean r = getStatusList()[0].isSuccessful();
			if (r) {
				setDownloadedState();
			} else {
				setNotDownloadedState();
			}
		}
	};

	private void setDownloadingState() {
		butDownload.setEnabled(false);
		butPlay.setEnabled(false);
		butRemove.setEnabled(false);
	}

	private void setDownloadedState() {
		butDownload.setEnabled(false);
		butPlay.setEnabled(true);
		butRemove.setEnabled(true);
	}

	private void setNotDownloadedState() {
		butPlay.setEnabled(false);
		butDownload.setEnabled(true);
		butRemove.setEnabled(false);
	}
}
