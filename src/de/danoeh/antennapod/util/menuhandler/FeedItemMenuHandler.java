package de.danoeh.antennapod.util.menuhandler;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.FlattrClickWorker;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedItem.State;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.ShareUtils;

/** Handles interactions with the FeedItemMenu. */
public class FeedItemMenuHandler {
	private FeedItemMenuHandler() {

	}

	public static boolean onPrepareMenu(Menu menu, FeedItem selectedItem) {
		FeedManager manager = FeedManager.getInstance();
		DownloadRequester requester = DownloadRequester.getInstance();
		boolean hasMedia = selectedItem.getMedia() != null;
		boolean downloaded = hasMedia && selectedItem.getMedia().isDownloaded();
		boolean downloading = hasMedia
				&& requester.isDownloadingFile(selectedItem.getMedia());
		boolean notLoadedAndNotLoading = hasMedia && (!downloaded)
				&& (!downloading);
		FeedItem.State state = selectedItem.getState();

		menu.findItem(R.id.play_item).setVisible(downloaded);
		menu.findItem(R.id.remove_item).setVisible(downloaded);
		menu.findItem(R.id.download_item).setVisible(notLoadedAndNotLoading);
		menu.findItem(R.id.stream_item).setVisible(
				notLoadedAndNotLoading | downloading);
		menu.findItem(R.id.cancel_download_item).setVisible(downloading);

		boolean isInQueue = manager.isInQueue(selectedItem);

		menu.findItem(R.id.remove_from_queue_item).setVisible(isInQueue);
		menu.findItem(R.id.add_to_queue_item).setVisible(
				!isInQueue && selectedItem.getMedia() != null);

		menu.findItem(R.id.share_link_item).setVisible(
				selectedItem.getLink() != null);

		menu.findItem(R.id.mark_unread_item).setVisible(
				state == FeedItem.State.IN_PROGRESS
						|| state == FeedItem.State.READ);
		menu.findItem(R.id.mark_read_item).setVisible(
				state == FeedItem.State.NEW
						|| state == FeedItem.State.IN_PROGRESS);

		if (selectedItem.getLink() != null) {
			menu.findItem(R.id.visit_website_item).setVisible(true);
		}

		if (selectedItem.getPaymentLink() != null) {
			menu.findItem(R.id.support_item).setVisible(true);
		}

		return true;
	}

	public static boolean onMenuItemClicked(Context context, MenuItem item,
			FeedItem selectedItem) throws DownloadRequestException {
		DownloadRequester requester = DownloadRequester.getInstance();
		FeedManager manager = FeedManager.getInstance();
		switch (item.getItemId()) {
		case R.id.download_item:
			manager.downloadFeedItem(context, selectedItem);
			break;
		case R.id.play_item:
			manager.playMedia(context, selectedItem.getMedia(), true, true,
					false);
			break;
		case R.id.remove_item:
			manager.deleteFeedMedia(context, selectedItem.getMedia());
			break;
		case R.id.cancel_download_item:
			requester.cancelDownload(context, selectedItem.getMedia());
			break;
		case R.id.mark_read_item:
			manager.markItemRead(context, selectedItem, true, true);
			break;
		case R.id.mark_unread_item:
			manager.markItemRead(context, selectedItem, false, true);
			break;
		case R.id.add_to_queue_item:
			manager.addQueueItem(context, selectedItem);
			break;
		case R.id.remove_from_queue_item:
			manager.removeQueueItem(context, selectedItem);
			break;
		case R.id.stream_item:
			manager.playMedia(context, selectedItem.getMedia(), true, true,
					true);
			break;
		case R.id.visit_website_item:
			Uri uri = Uri.parse(selectedItem.getLink());
			context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
			break;
		case R.id.support_item:
			new FlattrClickWorker(context, selectedItem.getPaymentLink())
					.executeAsync();
			break;
		case R.id.share_link_item:
			ShareUtils.shareFeedItemLink(context, selectedItem);
			break;
		default:
			return false;
		}
		// Refresh menu state

		return true;
	}

	public static boolean onCreateMenu(MenuInflater inflater, Menu menu) {
		inflater.inflate(R.menu.feeditem, menu);
		return true;
	}

}
