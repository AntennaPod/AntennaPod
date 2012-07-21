package de.danoeh.antennapod.activity;

import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import de.danoeh.antennapod.asynctask.FeedRemover;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.fragment.FeedlistFragment;
import de.danoeh.antennapod.fragment.ItemlistFragment;
import de.danoeh.antennapod.util.FeedMenuHandler;
import de.danoeh.antennapod.util.StorageUtils;
import de.danoeh.antennapod.R;

/** Displays a List of FeedItems */
public class FeedItemlistActivity extends SherlockFragmentActivity {
	private static final String TAG = "FeedItemlistActivity";

	private FeedManager manager;

	/** The feed which the activity displays */
	private Feed feed;
	private ItemlistFragment filf;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		StorageUtils.checkStorageAvailability(this);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.feeditemlist_activity);

		manager = FeedManager.getInstance();
		long feedId = getIntent().getLongExtra(
				FeedlistFragment.EXTRA_SELECTED_FEED, -1);
		if (feedId == -1)
			Log.e(TAG, "Received invalid feed selection.");

		feed = manager.getFeed(feedId);
		setTitle(feed.getTitle());

		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fT = fragmentManager.beginTransaction();

		filf = ItemlistFragment.newInstance(feed.getId());
		fT.add(R.id.feeditemlistFragment, filf);
		fT.commit();

	}

	@Override
	protected void onResume() {
		super.onResume();
		StorageUtils.checkStorageAvailability(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return FeedMenuHandler
				.onCreateOptionsMenu(new MenuInflater(this), menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return FeedMenuHandler.onPrepareOptionsMenu(menu, feed);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (FeedMenuHandler.onOptionsItemClicked(this, item, feed)) {
			filf.getListAdapter().notifyDataSetChanged();
		} else {
			switch (item.getItemId()) {
			case R.id.remove_item:
				FeedRemover remover = new FeedRemover(this) {
					@Override
					protected void onPostExecute(Void result) {
						super.onPostExecute(result);
						finish();
					}
				};
				if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
					remover.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
							feed);
				} else {
					remover.execute(feed);
				}
				break;
			case android.R.id.home:
				finish();
			}
		}
		return true;
	}

}
