package de.podfetcher.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.podfetcher.R;
import de.podfetcher.asynctask.FeedImageLoader;
import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedManager;

/** Displays information about a feed. */
public class FeedInfoActivity extends SherlockActivity {
	private static final String TAG = "FeedInfoActivity";

	public static final String EXTRA_FEED_ID = "de.podfetcher.extra.feedId";

	private ImageView imgvCover;
	private TextView txtvTitle;
	private TextView txtvDescription;
	private TextView txtvLanguage;
	private TextView txtvAuthor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feedinfo);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		long feedId = getIntent().getLongExtra(EXTRA_FEED_ID, -1);
		FeedManager manager = FeedManager.getInstance();
		Feed feed = manager.getFeed(feedId);
		if (feed != null) {
			Log.d(TAG, "Language is " + feed.getLanguage());
			Log.d(TAG, "Author is " + feed.getAuthor());
			imgvCover = (ImageView) findViewById(R.id.imgvCover);
			txtvTitle = (TextView) findViewById(R.id.txtvTitle);
			txtvDescription = (TextView) findViewById(R.id.txtvDescription);
			txtvLanguage = (TextView) findViewById(R.id.txtvLanguage);
			txtvAuthor = (TextView) findViewById(R.id.txtvAuthor);
			FeedImageLoader.getInstance().loadBitmap(feed.getImage(), imgvCover);
			
			txtvTitle.setText(feed.getTitle());
			txtvDescription.setText(feed.getDescription());
			if (feed.getAuthor() != null) {
				txtvAuthor.setText(feed.getAuthor());
			}
			if (feed.getLanguage() != null) {
				txtvLanguage.setText(feed.getLanguage());
			}
		} else {
			Log.e(TAG, "Activity was started with invalid arguments");
		}

	}

	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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
