package de.danoeh.antennapod.util.menuhandler;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.FlattrClickWorker;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.service.playback.PlaybackService;
import de.danoeh.antennapod.storage.DBTasks;
import de.danoeh.antennapod.storage.DBWriter;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.QueueAccess;
import de.danoeh.antennapod.util.ShareUtils;

/** Handles interactions with the FeedItemMenu. */
public class FeedItemMenuHandler {
	private FeedItemMenuHandler() {

	}

	/**
	 * Used by the MenuHandler to access different types of menus through one
	 * interface
	 */
	public interface MenuInterface {
		/**
		 * Implementations of this method should call findItem(id) on their
		 * menu-object and call setVisibility(visibility) on the returned
		 * MenuItem object.
		 */
		abstract void setItemVisibility(int id, boolean visible);
	}

	/**
	 * This method should be called in the prepare-methods of menus. It changes
	 * the visibility of the menu items depending on a FeedItem's attributes.
	 * 
	 * @param mi
	 *            An instance of MenuInterface that the method uses to change a
	 *            MenuItem's visibility
	 * @param selectedItem
	 *            The FeedItem for which the menu is supposed to be prepared
	 * @param showExtendedMenu
	 *            True if MenuItems that let the user share information about
	 *            the FeedItem and visit its website should be set visible. This
	 *            parameter should be set to false if the menu space is limited.
     * @param queueAccess
     *            Used for testing if the queue contains the selected item
	 * @return Returns true if selectedItem is not null.
	 * */
	public static boolean onPrepareMenu(MenuInterface mi,
			FeedItem selectedItem, boolean showExtendedMenu, QueueAccess queueAccess) {
        if (selectedItem == null) {
            return false;
        }
		DownloadRequester requester = DownloadRequester.getInstance();
		boolean hasMedia = selectedItem.getMedia() != null;
		boolean downloaded = hasMedia && selectedItem.getMedia().isDownloaded();
		boolean downloading = hasMedia
				&& requester.isDownloadingFile(selectedItem.getMedia());
		boolean notLoadedAndNotLoading = hasMedia && (!downloaded)
				&& (!downloading);
		boolean isPlaying = hasMedia
				&& selectedItem.getState() == FeedItem.State.PLAYING;

		FeedItem.State state = selectedItem.getState();

		if (!isPlaying) {
			mi.setItemVisibility(R.id.skip_episode_item, false);
		}
		if (!downloaded || isPlaying) {
			mi.setItemVisibility(R.id.play_item, false);
			mi.setItemVisibility(R.id.remove_item, false);
		}
		if (!notLoadedAndNotLoading) {
			mi.setItemVisibility(R.id.download_item, false);
		}
		if (!(notLoadedAndNotLoading | downloading) | isPlaying) {
			mi.setItemVisibility(R.id.stream_item, false);
		}
		if (!downloading) {
			mi.setItemVisibility(R.id.cancel_download_item, false);
		}

		boolean isInQueue = queueAccess.contains(selectedItem.getId());
		if (!isInQueue || isPlaying) {
			mi.setItemVisibility(R.id.remove_from_queue_item, false);
		}
		if (!(!isInQueue && selectedItem.getMedia() != null)) {
			mi.setItemVisibility(R.id.add_to_queue_item, false);
		}
		if (!showExtendedMenu || selectedItem.getLink() == null) {
			mi.setItemVisibility(R.id.share_link_item, false);
		}

		if (!AppConfig.DEBUG
				|| !(state == FeedItem.State.IN_PROGRESS || state == FeedItem.State.READ)) {
			mi.setItemVisibility(R.id.mark_unread_item, false);
		}
		if (!(state == FeedItem.State.NEW || state == FeedItem.State.IN_PROGRESS)) {
			mi.setItemVisibility(R.id.mark_read_item, false);
		}

		if (!showExtendedMenu || selectedItem.getLink() == null) {
			mi.setItemVisibility(R.id.visit_website_item, false);
		}

		if (selectedItem.getPaymentLink() == null) {
			mi.setItemVisibility(R.id.support_item, false);
		}
		return true;
	}

	public static boolean onMenuItemClicked(Context context, int menuItemId,
			FeedItem selectedItem) throws DownloadRequestException {
		DownloadRequester requester = DownloadRequester.getInstance();
		switch (menuItemId) {
		case R.id.skip_episode_item:
			context.sendBroadcast(new Intent(
					PlaybackService.ACTION_SKIP_CURRENT_EPISODE));
			break;
		case R.id.download_item:
			DBTasks.downloadFeedItems(context, selectedItem);
			break;
		case R.id.play_item:
			DBTasks.playMedia(context, selectedItem.getMedia(), true, true,
					false);
			break;
		case R.id.remove_item:
			DBWriter.deleteFeedMediaOfItem(context, selectedItem.getMedia().getId());
			break;
		case R.id.cancel_download_item:
			requester.cancelDownload(context, selectedItem.getMedia());
			break;
		case R.id.mark_read_item:
            DBWriter.markItemRead(context, selectedItem, true, true);
			break;
		case R.id.mark_unread_item:
            DBWriter.markItemRead(context, selectedItem, false, true);
			break;
		case R.id.add_to_queue_item:
			DBWriter.addQueueItem(context, selectedItem.getId());
			break;
		case R.id.remove_from_queue_item:
            DBWriter.removeQueueItem(context, selectedItem.getId(), true);
			break;
		case R.id.stream_item:
			DBTasks.playMedia(context, selectedItem.getMedia(), true, true,
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

}
