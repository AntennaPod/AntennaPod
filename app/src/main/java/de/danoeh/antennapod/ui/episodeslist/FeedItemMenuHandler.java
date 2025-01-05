package de.danoeh.antennapod.ui.episodeslist;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.playback.service.PlaybackServiceInterface;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings;
import de.danoeh.antennapod.ui.common.IntentUtils;
import de.danoeh.antennapod.playback.service.PlaybackStatus;
import de.danoeh.antennapod.ui.share.ShareUtils;
import de.danoeh.antennapod.ui.share.ShareDialog;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.sync.serviceinterface.EpisodeAction;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.appstartintent.MediaButtonStarter;
import de.danoeh.antennapod.ui.view.LocalDeleteModal;

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
     * @param menu          An instance of Menu
     * @param selectedItems The FeedItem for which the menu is supposed to be prepared
     * @param excludeIds Menu item that should be excluded
     * @return Returns true if selectedItem is not null.
     */
    public static boolean onPrepareMenu(Menu menu, List<FeedItem> selectedItems, int... excludeIds) {
        if (menu == null || selectedItems == null) {
            return false;
        }
        boolean canSkip = false;
        boolean canRemoveFromQueue = false;
        boolean canAddToQueue = false;
        boolean canVisitWebsite = false;
        boolean canShare = false;
        boolean canRemoveFromInbox = false;
        boolean canMarkPlayed = false;
        boolean canMarkUnplayed = false;
        boolean canResetPosition = false;
        boolean canDelete = false;
        boolean canDownload = false;
        boolean canAddFavorite = false;
        boolean canRemoveFavorite = false;
        boolean canShowTranscript = false;

        for (FeedItem item : selectedItems) {
            boolean hasMedia = item.getMedia() != null;
            canSkip |= hasMedia && PlaybackStatus.isPlaying(item.getMedia());
            canRemoveFromQueue |= item.isTagged(FeedItem.TAG_QUEUE);
            canAddToQueue |= hasMedia && !item.isTagged(FeedItem.TAG_QUEUE);
            canVisitWebsite |= !item.getFeed().isLocalFeed() && ShareUtils.hasLinkToShare(item);
            canShare |= !item.getFeed().isLocalFeed();
            canRemoveFromInbox |= item.isNew();
            canMarkPlayed |= !item.isPlayed();
            canMarkUnplayed |= item.isPlayed();
            canResetPosition |= hasMedia && item.getMedia().getPosition() != 0;
            canDelete |= (hasMedia && item.getMedia().isDownloaded()) || item.getFeed().isLocalFeed();
            canDownload |= hasMedia && !item.getMedia().isDownloaded() && !item.getFeed().isLocalFeed();
            canAddFavorite |= !item.isTagged(FeedItem.TAG_FAVORITE);
            canRemoveFavorite |= item.isTagged(FeedItem.TAG_FAVORITE);
            canShowTranscript |= item.hasTranscript();
        }

        if (selectedItems.size() > 1) {
            canVisitWebsite = false;
            canShare = false;
            canShowTranscript = false;
        }

        setItemVisibility(menu, R.id.skip_episode_item, canSkip);
        setItemVisibility(menu, R.id.remove_from_queue_item, canRemoveFromQueue);
        setItemVisibility(menu, R.id.add_to_queue_item, canAddToQueue);
        setItemVisibility(menu, R.id.visit_website_item, canVisitWebsite);
        setItemVisibility(menu, R.id.share_item, canShare);
        setItemVisibility(menu, R.id.remove_inbox_item, canRemoveFromInbox);
        setItemVisibility(menu, R.id.mark_read_item, canMarkPlayed);
        setItemVisibility(menu, R.id.mark_unread_item, canMarkUnplayed);
        setItemVisibility(menu, R.id.reset_position, canResetPosition);

        // Display proper strings when item has no media
        if (selectedItems.size() == 1 && selectedItems.get(0).getMedia() == null) {
            setItemTitle(menu, R.id.mark_read_item, R.string.mark_read_no_media_label);
            setItemTitle(menu, R.id.mark_unread_item, R.string.mark_unread_label_no_media);
        } else {
            setItemTitle(menu, R.id.mark_read_item, R.string.mark_read_label);
            setItemTitle(menu, R.id.mark_unread_item, R.string.mark_unread_label);
        }

        setItemVisibility(menu, R.id.add_to_favorites_item, canAddFavorite);
        setItemVisibility(menu, R.id.remove_from_favorites_item, canRemoveFavorite);
        setItemVisibility(menu, R.id.remove_item, canDelete);
        setItemVisibility(menu, R.id.download_item, canDownload);
        setItemVisibility(menu, R.id.transcript_item, canShowTranscript);

        if (selectedItems.size() == 1 && selectedItems.get(0).getFeed().getState() != Feed.STATE_SUBSCRIBED) {
            setItemVisibility(menu, R.id.mark_read_item, false);
        }

        if (excludeIds != null) {
            for (int id : excludeIds) {
                setItemVisibility(menu, id, false);
            }
        }
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
     * Default menu handling for the given FeedItem.
     *
     * A Fragment instance, (rather than the more generic Context), is needed as a parameter
     * to support some UI operations, e.g., creating a Snackbar.
     */
    public static boolean onMenuItemClicked(@NonNull Fragment fragment, int menuItemId,
                                            @NonNull FeedItem selectedItem) {

        @NonNull Context context = fragment.requireContext();
        if (menuItemId == R.id.skip_episode_item) {
            context.sendBroadcast(MediaButtonStarter.createIntent(context, KeyEvent.KEYCODE_MEDIA_NEXT));
        } else if (menuItemId == R.id.remove_item) {
            LocalDeleteModal.showLocalFeedDeleteWarningIfNecessary(context, Arrays.asList(selectedItem),
                    () -> DBWriter.deleteFeedMediaOfItem(context, selectedItem.getMedia()));
        } else if (menuItemId == R.id.remove_inbox_item) {
            removeNewFlagWithUndo(fragment, selectedItem);
        } else if (menuItemId == R.id.mark_read_item) {
            selectedItem.setPlayed(true);
            DBWriter.markItemPlayed(selectedItem, FeedItem.PLAYED, true);
            if (!selectedItem.getFeed().isLocalFeed() && selectedItem.getFeed().getState() == Feed.STATE_SUBSCRIBED
                    && SynchronizationSettings.isProviderConnected()) {
                FeedMedia media = selectedItem.getMedia();
                // not all items have media, Gpodder only cares about those that do
                if (media != null) {
                    EpisodeAction actionPlay = new EpisodeAction.Builder(selectedItem, EpisodeAction.PLAY)
                            .currentTimestamp()
                            .started(media.getDuration() / 1000)
                            .position(media.getDuration() / 1000)
                            .total(media.getDuration() / 1000)
                            .build();
                    SynchronizationQueue.getInstance().enqueueEpisodeAction(actionPlay);
                }
            }
        } else if (menuItemId == R.id.mark_unread_item) {
            selectedItem.setPlayed(false);
            DBWriter.markItemPlayed(selectedItem, FeedItem.UNPLAYED, false);
            if (!selectedItem.getFeed().isLocalFeed() && selectedItem.getMedia() != null
                    && selectedItem.getFeed().getState() == Feed.STATE_SUBSCRIBED) {
                SynchronizationQueue.getInstance().enqueueEpisodeAction(
                        new EpisodeAction.Builder(selectedItem, EpisodeAction.NEW)
                            .currentTimestamp()
                            .build());
            }
        } else if (menuItemId == R.id.add_to_queue_item) {
            DBWriter.addQueueItem(context, selectedItem);
        } else if (menuItemId == R.id.remove_from_queue_item) {
            DBWriter.removeQueueItem(context, true, selectedItem);
        } else if (menuItemId == R.id.add_to_favorites_item) {
            DBWriter.addFavoriteItem(selectedItem);
        } else if (menuItemId == R.id.remove_from_favorites_item) {
            DBWriter.removeFavoriteItem(selectedItem);
        } else if (menuItemId == R.id.reset_position) {
            selectedItem.getMedia().setPosition(0);
            if (PlaybackPreferences.getCurrentlyPlayingFeedMediaId() == selectedItem.getMedia().getId()) {
                PlaybackPreferences.writeNoMediaPlaying();
                IntentUtils.sendLocalBroadcast(context, PlaybackServiceInterface.ACTION_SHUTDOWN_PLAYBACK_SERVICE);
            }
            DBWriter.markItemPlayed(selectedItem, FeedItem.UNPLAYED, true);
        } else if (menuItemId == R.id.visit_website_item) {
            IntentUtils.openInBrowser(context, selectedItem.getLinkWithFallback());
        } else if (menuItemId == R.id.share_item) {
            ShareDialog shareDialog = ShareDialog.newInstance(selectedItem);
            shareDialog.show((fragment.getActivity().getSupportFragmentManager()), "ShareEpisodeDialog");
        } else {
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
    public static void markReadWithUndo(@NonNull Fragment fragment, FeedItem item,
                                        int playState, boolean showSnackbar) {
        if (item == null) {
            return;
        }

        Log.d(TAG, "markReadWithUndo(" + item.getId() + ")");
        // we're marking it as unplayed since the user didn't actually play it
        // but they don't want it considered 'NEW' anymore
        DBWriter.markItemPlayed(playState, item.getId());

        final Handler h = new Handler(fragment.requireContext().getMainLooper());
        final Runnable r = () -> {
            FeedMedia media = item.getMedia();
            if (media == null) {
                return;
            }
            boolean shouldAutoDelete = UserPreferences.isAutoDelete()
                    && (!item.getFeed().isLocalFeed() || UserPreferences.isAutoDeleteLocal());
            int smartMarkAsPlayedSecs = UserPreferences.getSmartMarkAsPlayedSecs();
            boolean almostEnded = media.getDuration() > 0
                    && media.getPosition() >= media.getDuration() - smartMarkAsPlayedSecs * 1000;
            if (almostEnded && shouldAutoDelete) {
                DBWriter.deleteFeedMediaOfItem(fragment.requireContext(), media);
            }
        };

        int playStateStringRes;
        switch (playState) {
            default:
            case FeedItem.UNPLAYED:
                if (item.getPlayState() == FeedItem.NEW) {
                    //was new
                    playStateStringRes = R.string.removed_inbox_label;
                } else {
                    //was played
                    playStateStringRes = R.string.marked_as_unplayed_label;
                }
                break;
            case FeedItem.PLAYED:
                playStateStringRes = R.string.marked_as_played_label;
                break;
        }

        int duration = Snackbar.LENGTH_LONG;

        if (showSnackbar) {
            ((MainActivity) fragment.getActivity()).showSnackbarAbovePlayer(
                    playStateStringRes, duration)
                    .setAction(fragment.getString(R.string.undo), v -> {
                        DBWriter.markItemPlayed(item.getPlayState(), item.getId());
                        // don't forget to cancel the thing that's going to remove the media
                        h.removeCallbacks(r);
                    });
        }

        h.postDelayed(r, (int) Math.ceil(duration * 1.05f));
    }

    public static void removeNewFlagWithUndo(@NonNull Fragment fragment, FeedItem item) {
        markReadWithUndo(fragment, item, FeedItem.UNPLAYED, false);
    }

}
