package de.danoeh.antennapod.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.FeedImageLoader;
import de.danoeh.antennapod.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.util.LangUtils;
import de.danoeh.antennapod.util.menuhandler.FeedMenuHandler;

/** Displays information about a feed. */
public class FeedInfoActivity extends SherlockActivity {
	private static final String TAG = "FeedInfoActivity";

	public static final String EXTRA_FEED_ID = "de.danoeh.antennapod.extra.feedId";

	private Feed feed;

	private ImageView imgvCover;
	private TextView txtvTitle;
	private TextView txtvDescription;
	private TextView txtvLanguage;
	private TextView txtvAuthor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(PodcastApp.getThemeResourceId());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feedinfo);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		long feedId = getIntent().getLongExtra(EXTRA_FEED_ID, -1);
		FeedManager manager = FeedManager.getInstance();
		feed = manager.getFeed(feedId);
		if (feed != null) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Language is " + feed.getLanguage());
			if (AppConfig.DEBUG)
				Log.d(TAG, "Author is " + feed.getAuthor());
			imgvCover = (ImageView) findViewById(R.id.imgvCover);
			txtvTitle = (TextView) findViewById(R.id.txtvTitle);
			txtvDescription = (TextView) findViewById(R.id.txtvDescription);
			txtvLanguage = (TextView) findViewById(R.id.txtvLanguage);
			txtvAuthor = (TextView) findViewById(R.id.txtvAuthor);
			imgvCover.post(new Runnable() {

				@Override
				public void run() {
					FeedImageLoader.getInstance().loadThumbnailBitmap(
							feed.getImage(), imgvCover);
				}
			});

			txtvTitle.setText(feed.getTitle());
			txtvDescription.setText(feed.getDescription());
			if (feed.getAuthor() != null) {
				txtvAuthor.setText(feed.getAuthor());
			}
			if (feed.getLanguage() != null) {
				txtvLanguage.setText(LangUtils.getLanguageString(feed
						.getLanguage()));
			}
		} else {
			Log.e(TAG, "Activity was started with invalid arguments");
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.feedinfo, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.support_item).setVisible(
				feed.getPaymentLink() != null);
		menu.findItem(R.id.share_link_item).setVisible(feed.getLink() != null);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			try {
				return FeedMenuHandler.onOptionsItemClicked(this, item, feed);
			} catch (DownloadRequestException e) {
				e.printStackTrace();
				DownloadRequestErrorDialogCreator.newRequestErrorDialog(this, e.getMessage());
			}
			return super.onOptionsItemSelected(item);
		}
	}
}
