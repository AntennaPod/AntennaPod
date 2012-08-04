package de.danoeh.antennapod.activity;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.MiroGuideItemlistAdapter;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.miroguide.con.MiroGuideException;
import de.danoeh.antennapod.miroguide.con.MiroGuideService;
import de.danoeh.antennapod.miroguide.model.MiroGuideChannel;
import de.danoeh.antennapod.miroguide.model.MiroGuideItem;
import de.danoeh.antennapod.storage.DownloadRequester;

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
				invalidateOptionsMenu();
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
		boolean notAdded = channelLoaded
				&& !((FeedManager.getInstance().feedExists(channel
						.getDownloadUrl())) || (DownloadRequester.getInstance()
						.isDownloadingFile(channel.getDownloadUrl())));
		menu.findItem(R.id.add_feed).setVisible(notAdded);
		menu.findItem(R.id.visit_website_item).setVisible(
				channelLoaded && channel.getWebsiteUrl() != null);
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

}
