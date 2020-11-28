package de.danoeh.antennapod.menuhandler;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.sync.SyncService;
import de.danoeh.antennapod.core.sync.model.EpisodeAction;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.ShareUtils;
import de.danoeh.antennapod.dialog.ShareDialog;

/**
 * Handles interactions with the FeedItemMenu.
 */
public class FeedItemMenuHandler {

    private static final String TAG = "FeedItemMenuHandler";

    private FeedItemMenuHandler() {
    }

    /**
     * This method should be called in the prepare-methods of menus. It changes
     * the visibility of the menu items depending on a FeedItem's attributes.
     *
     * @param menu               An instance of Menu
     * @param selectedItem     The FeedItem for which the menu is supposed to be prepared
     * @return Returns true if selectedItem is not null.
     */
    public static boolean onPrepareMenu(Menu menu, FeedItem selectedItem) {
        if (menu == null || selectedItem == null) {
            return false;
        }
        final boolean hasMedia = selectedItem.getMedia() != null;
        final boolean isPlaying = hasMedia && selectedItem.getState() == FeedItem.State.PLAYING;
        final boolean isInQueue = selectedItem.isTagged(FeedItem.TAG_QUEUE);
        final boolean fileDownloaded = hasMedia && selectedItem.getMedia().fileExists();
        final boolean isFavorite = selectedItem.isTagged(FeedItem.TAG_FAVORITE);

        setItemVisibility(menu, R.id.skip_episode_item, isPlaying);
        setItemVisibility(menu, R.id.remove_from_queue_item, isInQueue);
        setItemVisibility(menu, R.id.add_to_queue_item, !isInQueue && selectedItem.getMedia() != null);
        setItemVisibility(menu, R.id.visit_website_item, !selectedItem.getFeed().isLocalFeed()
                && ShareUtils.hasLinkToShare(selectedItem));
        setItemVisibility(menu, R.id.share_item, !selectedItem.getFeed().isLocalFeed());
        setItemVisibility(menu, R.id.remove_new_flag_item, selectedItem.isNew());
        setItemVisibility(menu, R.id.mark_read_item, !selectedItem.isPlayed());
        setItemVisibility(menu, R.id.mark_unread_item, selectedItem.isPlayed());
        setItemVisibility(menu, R.id.reset_position, hasMedia && selectedItem.getMedia().getPosition() != 0);

        if (!UserPreferences.isEnableAutodownload() || fileDownloaded || selectedItem.getFeed().isLocalFeed()) {
            setItemVisibility(menu, R.id.activate_auto_download, false);
            setItemVisibility(menu, R.id.deactivate_auto_download, false);
        } else {
            setItemVisibility(menu, R.id.activate_auto_download, !selectedItem.getAutoDownload());
            setItemVisibility(menu, R.id.deactivate_auto_download, selectedItem.getAutoDownload());
        }

        // Display proper strings when item has no media
        if (hasMedia) {
            setItemTitle(menu, R.id.mark_read_item, R.string.mark_read_label);
            setItemTitle(menu, R.id.mark_unread_item, R.string.mark_unread_label);
        } else {
            setItemTitle(menu, R.id.mark_read_item, R.string.mark_read_no_media_label);
            setItemTitle(menu, R.id.mark_unread_item, R.string.mark_unread_label_no_media);
        }

        setItemVisibility(menu, R.id.add_to_favorites_item, !isFavorite);
        setItemVisibility(menu, R.id.remove_from_favorites_item, isFavorite);
        setItemVisibility(menu, R.id.remove_item, fileDownloaded);
        return true;
    }

    /**
     * Used to set the viability of a menu item.
     * This method also does some null-checking so that neither menu nor the menu item are null
     * in order to prevent nullpointer exceptions.
     * @param menu The menu that should be used
     * @param menuId The id of the menu item that will be used
     * @param visibility The new visibility status of given menu item
     * */
    private static void setItemVisibility(Menu menu, int menuId, boolean visibility) {
        if (menu == null) {
            return;
        }
        MenuItem item = menu.findItem(menuId);
        if (item != null) {
            item.setVisible(visibility);
        }
    }

    /**
     * This method allows to replace to String of a menu item with a different one.
     * @param menu Menu item that should be used
     * @param id The id of the string that is going to be replaced.
     * @param noMedia The id of the new String that is going to be used.
     * */
    public static void setItemTitle(Menu menu, int id, int noMedia) {
        MenuItem item = menu.findItem(id);
        if (item != null) {
            item.setTitle(noMedia);
        }
    }

    /**
     * The same method as {@link #onPrepareMenu(Menu, FeedItem)}, but lets the
     * caller also specify a list of menu items that should not be shown.
     *
     * @param excludeIds Menu item that should be excluded
     * @return true if selectedItem is not null.
     */
    public static boolean onPrepareMenu(Menu menu, FeedItem selectedItem, int... excludeIds) {
        if (menu == null || selectedItem == null ) {
            return false;
        }
        boolean rc = onPrepareMenu(menu, selectedItem);
        if (rc && excludeIds != null) {
            for (int id : excludeIds) {
                setItemVisibility(menu, id, false);
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
                if (GpodnetPreferences.loggedIn()) {
                    FeedMedia media = selectedItem.getMedia();
                    // not all items have media, Gpodder only cares about those that do
                    if (media != null) {
                        EpisodeAction actionPlay = new EpisodeAction.Builder(selectedItem, EpisodeAction.PLAY)
                                .currentTimestamp()
                                .started(media.getDuration() / 1000)
                                .position(media.getDuration() / 1000)
                                .total(media.getDuration() / 1000)
                                .build();
                        SyncService.enqueueEpisodeAction(context, actionPlay);
                    }
                }
                break;
            case R.id.mark_unread_item:
                selectedItem.setPlayed(false);
                DBWriter.markItemPlayed(selectedItem, FeedItem.UNPLAYED, false);
                if (GpodnetPreferences.loggedIn() && selectedItem.getMedia() != null) {
                    EpisodeAction actionNew = new EpisodeAction.Builder(selectedItem, EpisodeAction.NEW)
                            .currentTimestamp()
                            .build();
                    SyncService.enqueueEpisodeAction(context, actionNew);
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
                if (PlaybackPreferences.getCurrentlyPlayingFeedMediaId() == selectedItem.getMedia().getId()) {
                    PlaybackPreferences.writeNoMediaPlaying();
                    IntentUtils.sendLocalBroadcast(context, PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE);
                }
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
            case R.id.share_item:
                ShareDialog shareDialog = ShareDialog.newInstance(selectedItem);
                shareDialog.show((fragment.getActivity().getSupportFragmentManager()), "ShareEpisodeDialog");
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


        Snackbar snackbar = ((MainActivity) fragment.getActivity()).showSnackbarAbovePlayer(
                R.string.removed_new_flag_label, Snackbar.LENGTH_LONG)
                .setAction(fragment.getString(R.string.undo), v -> {
                    DBWriter.markItemPlayed(FeedItem.NEW, item.getId());
                    // don't forget to cancel the thing that's going to remove the media
                    h.removeCallbacks(r);
                });
        h.postDelayed(r, (int) Math.ceil(snackbar.getDuration() * 1.05f));
    }

}
