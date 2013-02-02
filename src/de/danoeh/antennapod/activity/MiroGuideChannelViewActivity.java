package de.danoeh.antennapod.activity;

import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.MiroGuideItemlistAdapter;
import de.danoeh.antennapod.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.miroguide.conn.MiroGuideException;
import de.danoeh.antennapod.miroguide.conn.MiroGuideService;
import de.danoeh.antennapod.miroguide.model.MiroGuideChannel;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.storage.DownloadRequester;

/**
 * Displays information about one channel and lets the user add this channel to
 * his library.
 */
public class MiroGuideChannelViewActivity extends SherlockActivity {
	private static final String TAG = "MiroGuideChannelViewActivity";

	public static final String EXTRA_CHANNEL_ID = "id";
	public static final String EXTRA_CHANNEL_URL = "url";

	private RelativeLayout layoutContent;
	private ProgressBar progLoading;
	private TextView txtvTitle;
	private TextView txtVDescription;
	private ListView listEntries;

	private long channelId;
	private String channelUrl;
	private MiroGuideChannel channel;

	@Override
	protected void onPause() {
		super.onPause();
		channelLoader.cancel(true);
	}

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(PodcastApp.getThemeResourceId());
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.miroguide_channelview);

		layoutContent = (RelativeLayout) findViewById(R.id.layout_content);
		progLoading = (ProgressBar) findViewById(R.id.progLoading);
		txtvTitle = (TextView) findViewById(R.id.txtvTitle);
		txtVDescription = (TextView) findViewById(R.id.txtvDescription);
		listEntries = (ListView) findViewById(R.id.itemlist);

		channelId = getIntent().getLongExtra(EXTRA_CHANNEL_ID, -1);
		channelUrl = getIntent().getStringExtra(EXTRA_CHANNEL_URL);

		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			channelLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			channelLoader.execute();
		}

	}

	/** Is used to load channel information asynchronously. */
	private AsyncTask<Void, Void, Void> channelLoader = new AsyncTask<Void, Void, Void>() {
		private static final String TAG = "ChannelLoader";
		private Exception exception;

		@Override
		protected Void doInBackground(Void... params) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Starting background task");
			MiroGuideService service = new MiroGuideService();
			try {
				channel = service.getChannel(channelId);
			} catch (MiroGuideException e) {
				e.printStackTrace();
				exception = e;
			}
			return null;
		}

		@SuppressLint("NewApi")
		@Override
		protected void onPostExecute(Void result) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Loading finished");
			if (exception == null) {
				txtvTitle.setText(channel.getName());
				txtVDescription.setText(channel.getDescription());

				MiroGuideItemlistAdapter listAdapter = new MiroGuideItemlistAdapter(
						MiroGuideChannelViewActivity.this, 0,
						channel.getItems());
				listEntries.setAdapter(listAdapter);
				progLoading.setVisibility(View.GONE);
				layoutContent.setVisibility(View.VISIBLE);
				supportInvalidateOptionsMenu();
			} else {
				finish();
			}
		}

	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.channelview, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean channelLoaded = channel != null;
		boolean beingDownloaded = channelLoaded
				&& DownloadRequester.getInstance().isDownloadingFile(
						channel.getDownloadUrl());
		boolean notAdded = channelLoaded
				&& !((FeedManager.getInstance().feedExists(
						channel.getDownloadUrl()) || beingDownloaded));
		menu.findItem(R.id.add_feed).setVisible(notAdded);
		menu.findItem(R.id.visit_website_item).setVisible(
				channelLoaded && channel.getWebsiteUrl() != null);
		return true;
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.visit_website_item:
			Uri uri = Uri.parse(channel.getWebsiteUrl());
			startActivity(new Intent(Intent.ACTION_VIEW, uri));
			return true;
		case R.id.add_feed:
			try {
				DownloadRequester.getInstance().downloadFeed(
						this,
						new Feed(channel.getDownloadUrl(), new Date(), channel
								.getName()));
			} catch (DownloadRequestException e) {
				e.printStackTrace();
				DownloadRequestErrorDialogCreator.newRequestErrorDialog(this,
						e.getMessage());
			}
			Toast toast = Toast.makeText(this, R.string.miro_feed_added,
					Toast.LENGTH_LONG);
			toast.show();
			supportInvalidateOptionsMenu();
			return true;
		default:
			return false;
		}
	}

}
