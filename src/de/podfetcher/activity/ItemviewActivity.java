package de.podfetcher.activity;

import java.text.DateFormat;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.podfetcher.R;
import de.podfetcher.asynctask.DownloadObserver;
import de.podfetcher.asynctask.DownloadStatus;
import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedItem;
import de.podfetcher.feed.FeedManager;
import de.podfetcher.feed.FeedMedia;
import de.podfetcher.fragment.FeedItemlistFragment;
import de.podfetcher.fragment.FeedlistFragment;
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
	private WebView webvDescription;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		manager = FeedManager.getInstance();
		extractFeeditem();
		populateUI();

		butDownload.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				requester = DownloadRequester.getInstance();
				requester.downloadMedia(v.getContext(), item.getMedia());
				//getDownloadStatus();
			}
		});

		butPlay.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				manager.playMedia(v.getContext(), item.getMedia());
			}
		});
		
		butRemove.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (manager.deleteFeedMedia(v.getContext(), item.getMedia())) {
					//setNotDownloadedState();
				}
				
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
				FeedItemlistFragment.EXTRA_SELECTED_FEEDITEM, -1);
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
		webvDescription = (WebView) findViewById(R.id.webvDescription);
		setTitle(item.getFeed().getTitle());

		txtvPublished.setText(DateUtils.formatSameDayTime(item.getPubDate()
				.getTime(), System.currentTimeMillis(), DateFormat.MEDIUM,
				DateFormat.SHORT));
		txtvTitle.setText(item.getTitle());
		if (item.getFeed().getImage() != null) {
			imgvImage
					.setImageBitmap(item.getFeed().getImage().getImageBitmap());
		}
		webvDescription.loadData(item.getDescription(), "text/html", null);
	}

	private void getDownloadStatus(Menu menu) {
		FeedMedia media = item.getMedia();
		if (media.getFile_url() == null) {
			setNotDownloadedState(menu);
		} else if (media.isDownloaded()) {
			setDownloadedState(menu);
		} else {
			// observe
			setDownloadingState(menu);
			//downloadObserver.execute(media);
		}
	}

	final DownloadObserver downloadObserver = new DownloadObserver(this) {
		@Override
		protected void onProgressUpdate(
				DownloadStatus... values) {

		}

		@Override
		protected void onPostExecute(Boolean result) {
			boolean r = getStatusList()[0].isSuccessful();
			if (r) {
				//setDownloadedState();
			} else {
				//setNotDownloadedState();
			}
		}
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = new MenuInflater(this);
	    inflater.inflate(R.menu.feeditemlist, menu);
	    getDownloadStatus(menu);
	    return true;
	}
	
	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		return super.onOptionsItemSelected(item);
	}

	
	private void setDownloadingState(Menu menu) {
		
		butDownload.setEnabled(false);
		butPlay.setEnabled(false);
		butRemove.setEnabled(false);
	}

	private void setDownloadedState(Menu menu) {
		butDownload.setEnabled(false);
		butPlay.setEnabled(true);
		butRemove.setEnabled(true);
	}

	private void setNotDownloadedState(Menu menu) {
		butPlay.setEnabled(false);
		butDownload.setEnabled(true);
		butRemove.setEnabled(false);
	}
}
