package de.danoeh.antennapod.activity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.FeedRemover;
import de.danoeh.antennapod.dialog.ConfirmationDialog;
import de.danoeh.antennapod.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.fragment.ExternalPlayerFragment;
import de.danoeh.antennapod.fragment.FeedlistFragment;
import de.danoeh.antennapod.fragment.ItemlistFragment;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.util.StorageUtils;
import de.danoeh.antennapod.util.menuhandler.FeedMenuHandler;

/** Displays a List of FeedItems */
public class FeedItemlistActivity extends SherlockFragmentActivity {
	private static final String TAG = "FeedItemlistActivity";

	private FeedManager manager;

	/** The feed which the activity displays */
	private Feed feed;
	private ItemlistFragment filf;
	private ExternalPlayerFragment externalPlayerFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(UserPreferences.getTheme());
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
		fT.replace(R.id.feeditemlistFragment, filf);

		externalPlayerFragment = new ExternalPlayerFragment();
		fT.replace(R.id.playerFragment, externalPlayerFragment);
		fT.commit();

	}

	@Override
	protected void onResume() {
		super.onResume();
		StorageUtils.checkStorageAvailability(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		TypedArray drawables = obtainStyledAttributes(new int[] { R.attr.action_search });
		menu.add(Menu.NONE, R.id.search_item, Menu.NONE, R.string.search_label)
				.setIcon(drawables.getDrawable(0))
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		return FeedMenuHandler
				.onCreateOptionsMenu(new MenuInflater(this), menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return FeedMenuHandler.onPrepareOptionsMenu(menu, feed);
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		try {
			if (FeedMenuHandler.onOptionsItemClicked(this, item, feed)) {
				filf.getListAdapter().notifyDataSetChanged();
			} else {
				switch (item.getItemId()) {
				case R.id.remove_item:
					final FeedRemover remover = new FeedRemover(
							FeedItemlistActivity.this, feed) {
						@Override
						protected void onPostExecute(Void result) {
							super.onPostExecute(result);
							finish();
						}
					};
					ConfirmationDialog conDialog = new ConfirmationDialog(this,
							R.string.remove_feed_label,
							R.string.feed_delete_confirmation_msg) {

						@Override
						public void onConfirmButtonPressed(
								DialogInterface dialog) {
							dialog.dismiss();
							remover.executeAsync();
						}
					};
					conDialog.createNewDialog().show();
					break;
				case R.id.search_item:
					onSearchRequested();
					break;
				case android.R.id.home:
					Intent intent = new Intent(this, MainActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					break;
				}
			}
		} catch (DownloadRequestException e) {
			e.printStackTrace();
			DownloadRequestErrorDialogCreator.newRequestErrorDialog(this,
					e.getMessage());
		}
		return true;
	}

	@Override
	public boolean onSearchRequested() {
		Bundle bundle = new Bundle();
		bundle.putLong(SearchActivity.EXTRA_FEED_ID, feed.getId());
		startSearch(null, false, bundle, false);
		return true;
	}

}
