package de.podfetcher.util;

import android.content.Context;

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

	
	public static boolean onPrepareOptionsMenu(Menu menu) {
		return true;
	}

	/** NOTE: This method does not handle clicks on the 'remove feed' - item. */
	public static boolean onOptionsItemClicked(Context context, MenuItem item, Feed selectedFeed) {
		FeedManager manager = FeedManager.getInstance();
		switch (item.getItemId()) {	
		case R.id.mark_all_read_item:
			manager.markFeedRead(context, selectedFeed);
			break;
		default:
			return false;
		}
		return true;
	}
}
