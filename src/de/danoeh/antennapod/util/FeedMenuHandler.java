package de.danoeh.antennapod.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.activity.FeedInfoActivity;
import de.danoeh.antennapod.asynctask.FlattrClickWorker;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.service.DownloadService;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;

/** Handles interactions with the FeedItemMenu. */
public class FeedMenuHandler {
	private static final String TAG = "FeedMenuHandler";

	public static boolean onCreateOptionsMenu(MenuInflater inflater, Menu menu) {
		inflater.inflate(R.menu.feedlist, menu);
		return true;
	}

	public static boolean onPrepareOptionsMenu(Menu menu, Feed selectedFeed) {
		if (AppConfig.DEBUG) Log.d(TAG, "Preparing options menu");
		if (selectedFeed.getPaymentLink() != null) {
			menu.findItem(R.id.support_item).setVisible(true);
		}
		MenuItem refresh = menu.findItem(R.id.refresh_item);
		if (DownloadService.isRunning
				&& DownloadRequester.getInstance().isDownloadingFile(
						selectedFeed)) {
			refresh.setVisible(false);
		} else {
			refresh.setVisible(true);
		}
		
		menu.findItem(R.id.share_link_item).setVisible(selectedFeed.getLink() != null);
		
		return true;
	}

	/** NOTE: This method does not handle clicks on the 'remove feed' - item. */
	public static boolean onOptionsItemClicked(Context context, MenuItem item,
			Feed selectedFeed) {
		FeedManager manager = FeedManager.getInstance();
		switch (item.getItemId()) {
		case R.id.show_info_item:
			Intent startIntent = new Intent(context, FeedInfoActivity.class);
			startIntent.putExtra(FeedInfoActivity.EXTRA_FEED_ID,
					selectedFeed.getId());
			context.startActivity(startIntent);
			break;
		case R.id.refresh_item:
			manager.refreshFeed(context, selectedFeed);
			break;
		case R.id.mark_all_read_item:
			manager.markFeedRead(context, selectedFeed);
			break;
		case R.id.visit_website_item:
			Uri uri = Uri.parse(selectedFeed.getLink());
			context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
			break;
		case R.id.support_item:
			new FlattrClickWorker(context, selectedFeed.getPaymentLink()).executeAsync();
			break;
		case R.id.share_link_item:
			ShareUtils.shareFeedlink(context, selectedFeed);
			break;
		case R.id.share_source_item:
			ShareUtils.shareFeedDownloadLink(context, selectedFeed);
			break;
		default:
			return false;
		}
		return true;
	}
}
