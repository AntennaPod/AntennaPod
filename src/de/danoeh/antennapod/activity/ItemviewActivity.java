package de.danoeh.antennapod.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.fragment.ItemDescriptionFragment;
import de.danoeh.antennapod.fragment.ItemlistFragment;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.util.QueueAccess;
import de.danoeh.antennapod.util.StorageUtils;
import de.danoeh.antennapod.util.menuhandler.FeedItemMenuHandler;

import java.text.DateFormat;

/** Displays a single FeedItem and provides various actions */
public class ItemviewActivity extends ActionBarActivity {
	private static final String TAG = "ItemviewActivity";

    private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED | EventDistributor.DOWNLOAD_QUEUED;

	private FeedItem item;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(UserPreferences.getTheme());
		super.onCreate(savedInstanceState);
		StorageUtils.checkStorageAvailability(this);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
        EventDistributor.getInstance().register(contentUpdate);

        long itemId = getIntent().getLongExtra(
                ItemlistFragment.EXTRA_SELECTED_FEEDITEM, -1);
        if (itemId == -1) {
            Log.e(TAG, "Received invalid selection of either feeditem or feed.");
        } else {
            loadData(itemId);
        }
	}

	@Override
	protected void onResume() {
		super.onResume();
		StorageUtils.checkStorageAvailability(this);

	}

	@Override
	public void onStop() {
		super.onStop();
		if (AppConfig.DEBUG)
			Log.d(TAG, "Stopping Activity");
	}

    private void loadData(long itemId) {
        AsyncTask<Long, Void, FeedItem> loadTask = new AsyncTask<Long, Void, FeedItem>() {

            @Override
            protected FeedItem doInBackground(Long... longs) {
                return DBReader.getFeedItem(ItemviewActivity.this, longs[0]);
            }

            @Override
            protected void onPostExecute(FeedItem feedItem) {
                super.onPostExecute(feedItem);
                if (feedItem != null && feedItem.getFeed() != null) {
                    item = feedItem;
                    populateUI();
                    supportInvalidateOptionsMenu();
                } else {
                    if (feedItem == null) {
                        Log.e(TAG, "Error: FeedItem was null");
                    } else if (feedItem.getFeed() == null) {
                        Log.e(TAG, "Error: Feed was null");
                    }
                }
            }
        };
        loadTask.execute(itemId);
    }

	private void populateUI() {
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.feeditemview);
		TextView txtvTitle = (TextView) findViewById(R.id.txtvItemname);
		TextView txtvPublished = (TextView) findViewById(R.id.txtvPublished);
		setTitle(item.getFeed().getTitle());

		txtvPublished.setText(DateUtils.formatSameDayTime(item.getPubDate()
				.getTime(), System.currentTimeMillis(), DateFormat.MEDIUM,
				DateFormat.SHORT));
		txtvTitle.setText(item.getTitle());

		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager
				.beginTransaction();
		ItemDescriptionFragment fragment = ItemDescriptionFragment
				.newInstance(item, false);
		fragmentTransaction.replace(R.id.description_fragment, fragment);
		fragmentTransaction.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        if (item != null) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.feeditem, menu);
            return true;
        } else {
            return false;
        }
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		try {
			if (!FeedItemMenuHandler.onMenuItemClicked(this,
					menuItem.getItemId(), item)) {
				switch (menuItem.getItemId()) {
				case android.R.id.home:
					finish();
					break;
				}
			}
		} catch (DownloadRequestException e) {
			e.printStackTrace();
			DownloadRequestErrorDialogCreator.newRequestErrorDialog(this,
					e.getMessage());
		}
		supportInvalidateOptionsMenu();
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		return FeedItemMenuHandler.onPrepareMenu(
                new FeedItemMenuHandler.MenuInterface() {

                    @Override
                    public void setItemVisibility(int id, boolean visible) {
                        menu.findItem(id).setVisible(visible);
                    }
                }, item, true, QueueAccess.NotInQueueAccess());
	}

    private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {

        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((EVENTS & arg) != 0) {
                if (AppConfig.DEBUG)
                    Log.d(TAG, "Received contentUpdate Intent.");
                if (item != null) {
                    loadData(item.getId());
                }
            }
        }
    };


}
