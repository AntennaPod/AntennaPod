package de.podfetcher.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.podfetcher.R;
import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedItem;
import de.podfetcher.feed.FeedManager;

/** Handles interactions with the FeedItemMenu. */
public class FeedMenuHandler {

	public static boolean onCreateOptionsMenu(MenuInflater inflater, Menu menu) {
		inflater.inflate(R.menu.feedlist, menu);
		return true;
	}

	public static boolean onPrepareOptionsMenu(Menu menu, Feed selectedFeed) {
		if (selectedFeed.getPaymentLink() != null) {
			menu.findItem(R.id.support_item).setVisible(true);
		}
		return true;
	}

	/** NOTE: This method does not handle clicks on the 'remove feed' - item. */
	public static boolean onOptionsItemClicked(Context context, MenuItem item,
			Feed selectedFeed) {
		FeedManager manager = FeedManager.getInstance();
		switch (item.getItemId()) {
		case R.id.mark_all_read_item:
			manager.markFeedRead(context, selectedFeed);
			break;
		case R.id.visit_website_item:
			Uri uri = Uri.parse(selectedFeed.getLink());
			context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
			break;
		case R.id.support_item:
			Uri supportUri = Uri.parse(selectedFeed.getPaymentLink());
			context.startActivity(new Intent(Intent.ACTION_VIEW, supportUri));
			break;
		default:
			return false;
		}
		return true;
	}
}
