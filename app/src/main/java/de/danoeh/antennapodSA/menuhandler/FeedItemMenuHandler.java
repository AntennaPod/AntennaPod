package de.danoeh.antennapodSA.menuhandler;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import de.danoeh.antennapodSA.R;
import de.danoeh.antennapodSA.core.feed.FeedItem;
import de.danoeh.antennapodSA.core.feed.FeedMedia;
import de.danoeh.antennapodSA.core.gpoddernet.model.GpodnetEpisodeAction;
import de.danoeh.antennapodSA.core.gpoddernet.model.GpodnetEpisodeAction.Action;
import de.danoeh.antennapodSA.core.preferences.GpodnetPreferences;
import de.danoeh.antennapodSA.core.preferences.UserPreferences;
import de.danoeh.antennapodSA.core.service.playback.PlaybackService;
import de.danoeh.antennapodSA.core.storage.DBWriter;
import de.danoeh.antennapodSA.core.util.FeedItemUtil;
import de.danoeh.antennapodSA.core.util.IntentUtils;
import de.danoeh.antennapodSA.core.util.ShareUtils;

/**
 * Handles interactions with the FeedItemMenu.
 */
public class FeedItemMenuHandler {

    private static final String TAG = "FeedItemMenuHandler";

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
        void setItemVisibility(int id, boolean visible);
    }

    /**
     * This method should be called in the prepare-methods of menus. It changes
     * the visibility of the menu items depending on a FeedItem's attributes.
     *
     * @param mi               An instance of MenuInterface that the method uses to change a
     *                         MenuItem's visibility
     * @param selectedItem     The FeedItem for which the menu is supposed to be prepared
     * @return Returns true if selectedItem is not null.
     */
    public static boolean onPrepareMenu(MenuInterface mi,
                                        FeedItem selectedItem) {
        if (selectedItem == null) {
            return false;
        }
        boolean hasMedia = selectedItem.getMedia() != null;
        boolean isPlaying = hasMedia && selectedItem.getState() == FeedItem.State.PLAYING;

        if (!isPlaying) {
            mi.setItemVisibility(R.id.skip_episode_item, false);
        }

        boolean isInQueue = selectedItem.isTagged(FeedItem.TAG_QUEUE);
        if (!isInQueue) {
            mi.setItemVisibility(R.id.remove_from_queue_item, false);
        }
        if (!(!isInQueue && selectedItem.getMedia() != null)) {
            mi.setItemVisibility(R.id.add_to_queue_item, false);
        }

        if (!ShareUtils.hasLinkToShare(selectedItem)) {
            mi.setItemVisibility(R.id.visit_website_item, false);
            mi.setItemVisibility(R.id.share_link_item, false);
            mi.setItemVisibility(R.id.share_link_with_position_item, false);
        }
        if (!hasMedia || selectedItem.getMedia().getDownload_url() == null) {
            mi.setItemVisibility(R.id.share_download_url_item, false);
            mi.setItemVisibility(R.id.share_download_url_with_position_item, false);
        }
        if(!hasMedia || selectedItem.getMedia().getPosition() <= 0) {
            mi.setItemVisibility(R.id.share_link_with_position_item, false);
            mi.setItemVisibility(R.id.share_download_url_with_position_item, false);
        }

        boolean fileDownloaded = hasMedia && selectedItem.getMedia().fileExists();
        mi.setItemVisibility(R.id.share_file, fileDownloaded);

        mi.setItemVisibility(R.id.remove_new_flag_item, selectedItem.isNew());
        if (selectedItem.isPlayed()) {
            mi.setItemVisibility(R.id.mark_read_item, false);
        } else {
            mi.setItemVisibility(R.id.mark_unread_item, false);
        }

        if(selectedItem.getMedia() == null || selectedItem.getMedia().getPosition() == 0) {
            mi.setItemVisibility(R.id.reset_position, false);
        }

        if(!UserPreferences.isEnableAutodownload() || fileDownloaded) {
            mi.setItemVisibility(R.id.activate_auto_download, false);
            mi.setItemVisibility(R.id.deactivate_auto_download, false);
        } else if(selectedItem.getAutoDownload()) {
            mi.setItemVisibility(R.id.activate_auto_download, false);
        } else {
            mi.setItemVisibility(R.id.deactivate_auto_download, false);
        }

        boolean isFavorite = selectedItem.isTagged(FeedItem.TAG_FAVORITE);
        mi.setItemVisibility(R.id.add_to_favorites_item, !isFavorite);
        mi.setItemVisibility(R.id.remove_from_favorites_item, isFavorite);

        mi.setItemVisibility(R.id.remove_item, fileDownloaded);

        return true;
    }

    /**
     * The same method as onPrepareMenu(MenuInterface, FeedItem, boolean, QueueAccess), but lets the
     * caller also specify a list of menu items that should not be shown.
     *
     * @param excludeIds Menu item that should be excluded
     * @return true if selectedItem is not null.
     */
    public static boolean onPrepareMenu(MenuInterface mi,
                                        FeedItem selectedItem,
                                        int... excludeIds) {
        boolean rc = onPrepareMenu(mi, selectedItem);
        if (rc && excludeIds != null) {
            for (int id : excludeIds) {
                mi.setItemVisibility(id, false);
            }
        }
        return rc;
    }

    /**
     * Default menu handling for the given FeedItem.
     *
     * A Fragment instance, (rather than the more generic Context), is needed as a parameter
     * to support some UI operations, e.g., creating a Snackbar.
     */
    public static boolean onMenuItemClicked(@NonNull Fragment fragment, int menuItemId,
                                            @NonNull FeedItem selectedItem) {

        @NonNull Context context = fragment.requireContext();
        switch (menuItemId) {
            case R.id.skip_episode_item:
                IntentUtils.sendLocalBroadcast(context, PlaybackService.ACTION_SKIP_CURRENT_EPISODE);
                break;
            case R.id.remove_item:
                DBWriter.deleteFeedMediaOfItem(context, selectedItem.getMedia().getId());
                break;
            case R.id.remove_new_flag_item:
                removeNewFlagWithUndo(fragment, selectedItem);
                break;
            case R.id.mark_read_item:
                selectedItem.setPlayed(true);
                DBWriter.markItemPlayed(selectedItem, FeedItem.PLAYED, true);
                if(GpodnetPreferences.loggedIn()) {
                    FeedMedia media = selectedItem.getMedia();
                    // not all items have media, Gpodder only cares about those that do
                    if (media != null) {
                        GpodnetEpisodeAction actionPlay = new GpodnetEpisodeAction.Builder(selectedItem, Action.PLAY)
                                .currentDeviceId()
                                .currentTimestamp()
                                .started(media.getDuration() / 1000)
                                .position(media.getDuration() / 1000)
                                .total(media.getDuration() / 1000)
                                .build();
                        GpodnetPreferences.enqueueEpisodeAction(actionPlay);
                    }
                }
                break;
            case R.id.mark_unread_item:
                selectedItem.setPlayed(false);
                DBWriter.markItemPlayed(selectedItem, FeedItem.UNPLAYED, false);
                if(GpodnetPreferences.loggedIn() && selectedItem.getMedia() != null) {
                    GpodnetEpisodeAction actionNew = new GpodnetEpisodeAction.Builder(selectedItem, Action.NEW)
                            .currentDeviceId()
                            .currentTimestamp()
                            .build();
                    GpodnetPreferences.enqueueEpisodeAction(actionNew);
                }
                break;
            case R.id.add_to_queue_item:
                DBWriter.addQueueItem(context, selectedItem);
                break;
            case R.id.remove_from_queue_item:
                DBWriter.removeQueueItem(context, true, selectedItem);
                break;
            case R.id.add_to_favorites_item:
                DBWriter.addFavoriteItem(selectedItem);
                break;
            case R.id.remove_from_favorites_item:
                DBWriter.removeFavoriteItem(selectedItem);
                break;
            case R.id.reset_position:
                selectedItem.getMedia().setPosition(0);
                DBWriter.markItemPlayed(selectedItem, FeedItem.UNPLAYED, true);
                break;
            case R.id.activate_auto_download:
                selectedItem.setAutoDownload(true);
                DBWriter.setFeedItemAutoDownload(selectedItem, true);
                break;
            case R.id.deactivate_auto_download:
                selectedItem.setAutoDownload(false);
                DBWriter.setFeedItemAutoDownload(selectedItem, false);
                break;
            case R.id.visit_website_item:
                IntentUtils.openInBrowser(context, FeedItemUtil.getLinkWithFallback(selectedItem));
                break;
            case R.id.share_link_item:
                ShareUtils.shareFeedItemLink(context, selectedItem);
                break;
            case R.id.share_download_url_item:
                ShareUtils.shareFeedItemDownloadLink(context, selectedItem);
                break;
            case R.id.share_link_with_position_item:
                ShareUtils.shareFeedItemLink(context, selectedItem, true);
                break;
            case R.id.share_download_url_with_position_item:
                ShareUtils.shareFeedItemDownloadLink(context, selectedItem, true);
                break;
            case R.id.share_file:
                ShareUtils.shareFeedItemFile(context, selectedItem.getMedia());
                break;
            default:
                Log.d(TAG, "Unknown menuItemId: " + menuItemId);
                return false;
        }
        // Refresh menu state

        return true;
    }

    /**
     * Remove new flag with additional UI logic to allow undo with Snackbar.
     *
     * Undo is useful for Remove new flag, given there is no UI to undo it otherwise
     * ,i.e., there is (context) menu item for add new flag
     */
    public static void removeNewFlagWithUndo(@NonNull Fragment fragment, FeedItem item) {
        if (item == null) {
            return;
        }

        Log.d(TAG, "removeNewFlagWithUndo(" + item.getId() + ")");
        // we're marking it as unplayed since the user didn't actually play it
        // but they don't want it considered 'NEW' anymore
        DBWriter.markItemPlayed(FeedItem.UNPLAYED, item.getId());

        final Handler h = new Handler(fragment.requireContext().getMainLooper());
        final Runnable r = () -> {
            FeedMedia media = item.getMedia();
            if (media != null && media.hasAlmostEnded() && UserPreferences.isAutoDelete()) {
                DBWriter.deleteFeedMediaOfItem(fragment.requireContext(), media.getId());
            }
        };

        Snackbar snackbar = Snackbar.make(fragment.getView(), fragment.getString(R.string.removed_new_flag_label),
                Snackbar.LENGTH_LONG);
        snackbar.setAction(fragment.getString(R.string.undo), v -> {
            DBWriter.markItemPlayed(FeedItem.NEW, item.getId());
            // don't forget to cancel the thing that's going to remove the media
            h.removeCallbacks(r);
        });
        snackbar.show();
        h.postDelayed(r, (int) Math.ceil(snackbar.getDuration() * 1.05f));
    }

}
