package de.podfetcher.syndication.util;

import android.content.Context;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.podfetcher.R;
import de.podfetcher.feed.FeedItem;
import de.podfetcher.feed.FeedManager;
import de.podfetcher.storage.DownloadRequester;

/** Handles interactions with the FeedItemMenu. */
public class FeedItemMenuHandler {
	private FeedItemMenuHandler() {
		
	}
	
	public static boolean onPrepareMenu(Menu menu, FeedItem selectedItem) {
		FeedManager manager = FeedManager.getInstance();
		
		if (selectedItem.getMedia().isDownloaded()) {
			menu.findItem(R.id.play_item).setVisible(true);
			menu.findItem(R.id.remove_item).setVisible(true);
		} else if (selectedItem.getMedia().getFile_url() == null) {
			menu.findItem(R.id.download_item).setVisible(true);
			menu.findItem(R.id.stream_item).setVisible(true);
		} else {
			menu.findItem(R.id.cancel_download_item).setVisible(true);
		}
		
		if (selectedItem.isRead()) {
			menu.findItem(R.id.mark_unread_item).setVisible(true);
		} else {
			menu.findItem(R.id.mark_read_item).setVisible(true);
		}
		
		if (manager.isInQueue(selectedItem)) {
			menu.findItem(R.id.remove_from_queue_item).setVisible(true);
		} else {
			menu.findItem(R.id.add_to_queue_item).setVisible(true);
		}
		return true;
	}
	
	public static boolean onMenuItemClicked(Context context, MenuItem item, FeedItem selectedItem) {
		DownloadRequester requester = DownloadRequester.getInstance();
		FeedManager manager = FeedManager.getInstance();
		switch (item.getItemId()) {
		case R.id.download_item:
			requester.downloadMedia(context,
					selectedItem.getMedia());
			break;
		case R.id.play_item:
			manager.playMedia(context,
					selectedItem.getMedia(), true, true, false);
			break;
		case R.id.remove_item:
			manager.deleteFeedMedia(context,
					selectedItem.getMedia());
			break;
		case R.id.cancel_download_item:
			requester.cancelDownload(context, selectedItem
					.getMedia().getDownloadId());
			break;
		case R.id.mark_read_item:
			manager.markItemRead(context, selectedItem, true);
			break;
		case R.id.mark_unread_item:
			manager.markItemRead(context, selectedItem, false);
			break;
		case R.id.add_to_queue_item:
			manager.addQueueItem(context, selectedItem);
			break;
		case R.id.remove_from_queue_item:
			manager.removeQueueItem(context, selectedItem);
			break;
		case R.id.stream_item:
			manager.playMedia(context, selectedItem.getMedia(), true, true, true);
		}
		// Refresh menu state
		
		return true;
	}
	
	public static boolean onCreateMenu(MenuInflater inflater, Menu menu) {
		inflater.inflate(R.menu.feeditem, menu);
		return true;
	}
	
	
}
