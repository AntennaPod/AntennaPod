package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.actionbutton.DownloadActionButton;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.dialog.SwipeActionsDialog;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class SwipeActions {
    public static final String PREF_NAME = "SwipeActionsPrefs";
    public static final String PREF_SWIPEACTIONS = "PrefsSwipeActions";
    public static final String ADD_TO_QUEUE = "ADDTOQUEUE";
    public static final String MARK_PLAYED = "MARKPLAYED";
    public static final String MARK_UNPLAYED = "MARKUNPLAYED";
    public static final String MARK_FAV = "MARKFAV";
    public static final String START_DOWNLOAD = "STARTDOWNLOAD";

    RecyclerView recyclerView;
    ItemTouchHelper itemTouchHelper;
    Fragment fragment;
    String tag;

    public SwipeActions(Fragment fragment, String tag) {
        this.fragment = fragment;
        this.tag = tag;
        itemTouchHelper();
    }

    public SwipeActions attachTo(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        itemTouchHelper.attachToRecyclerView(recyclerView);
        return this;
    }

    private void itemTouchHelper() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {

            String[] rightleft = getPrefs(fragment.requireContext(), tag);

            @Override
            public boolean onMove(@NotNull RecyclerView recyclerView, @NotNull RecyclerView.ViewHolder viewHolder,
                                  @NotNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NotNull RecyclerView.ViewHolder viewHolder, int swipeDir) {
                if (rightleft.length == 0) {
                    //open settings dialog if no prefs are set
                    show(() -> rightleft = getPrefs(fragment.requireContext(), tag));
                    return;
                }

                String action = rightleft[swipeDir == ItemTouchHelper.RIGHT ? 0 : 1];

                FeedItem item = ((EpisodeItemViewHolder) viewHolder).getFeedItem();
                switch (action) {
                    case ADD_TO_QUEUE:
                        if (!item.isTagged(FeedItem.TAG_QUEUE)) {
                            FeedItemMenuHandler.addToQueue(fragment.requireContext(), item);
                        } else {
                            //already in queue
                            resetItemTouchHelper();
                        }
                        break;
                    case MARK_PLAYED:
                        int togglePlayState =
                                item.getPlayState() != FeedItem.PLAYED  ? FeedItem.PLAYED : FeedItem.UNPLAYED;
                        FeedItemMenuHandler.markReadWithUndo(fragment,
                                item, togglePlayState);
                        break;
                    case MARK_UNPLAYED: //remove "new" flag
                        FeedItemMenuHandler.markReadWithUndo(fragment,
                                item, FeedItem.UNPLAYED);
                        break;
                    case MARK_FAV:
                        DBWriter.toggleFavoriteItem(item);
                        break;
                    case START_DOWNLOAD:
                        if (!item.isDownloaded()) {
                            new DownloadActionButton(item, item.isTagged(FeedItem.TAG_QUEUE))
                                    .onClick(fragment.requireContext());
                        } else {
                            resetItemTouchHelper();
                            ((MainActivity) fragment.requireActivity()).showSnackbarAbovePlayer(
                                    fragment.getResources().getString(R.string.already_downloaded),
                                    Snackbar.LENGTH_SHORT);
                        }
                        break;
                    default: break;
                }
            }

            @Override
            public void onChildDraw(@NotNull Canvas c, @NotNull RecyclerView recyclerView, @NotNull RecyclerView.ViewHolder viewHolder,
                                    float dx, float dy, int actionState, boolean isCurrentlyActive) {
                //display only if preferences are set
                if (rightleft.length > 0) {
                    new RecyclerViewSwipeDecorator.Builder(
                            c, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive)
                            .addSwipeRightBackgroundColor(
                                    ContextCompat.getColor(fragment.requireContext(), actionColorFor(rightleft[0])))
                            .addSwipeRightActionIcon(actionIconFor(rightleft[0]))
                            .addSwipeLeftBackgroundColor(
                                    ContextCompat.getColor(fragment.requireContext(), actionColorFor(rightleft[1])))
                            .addSwipeLeftActionIcon(actionIconFor(rightleft[1]))
                            .create()
                            .decorate();
                }

                super.onChildDraw(c, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive);
            }
        };

        itemTouchHelper = new ItemTouchHelper(simpleCallback);
    }

    public static int actionIconFor(String swipeAction) {
        switch (swipeAction) {
            case ADD_TO_QUEUE:
                return R.drawable.ic_playlist;
            case MARK_PLAYED:
                return R.drawable.ic_check;
            default:
            case MARK_UNPLAYED:
                return R.drawable.ic_check;
            case MARK_FAV:
                return R.drawable.ic_star;
            case START_DOWNLOAD:
                return R.drawable.ic_download;
        }
    }

    public static int actionColorFor(String swipeAction) {
        switch (swipeAction) {
            case ADD_TO_QUEUE:
                return R.color.swipe_light_green_200;
            case MARK_PLAYED:
                return R.color.swipe_light_red_200;
            default:
            case MARK_UNPLAYED:
                return R.color.swipe_light_blue_200;
            case MARK_FAV:
                return R.color.swipe_yellow_200;
            case START_DOWNLOAD:
                return R.color.swipe_light_blue_200;
        }
    }

    private static String[] getPrefs(Context context, String tag, String defaultActions) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String prefsString = prefs.getString(PREF_SWIPEACTIONS + tag, defaultActions);
        if (prefsString.isEmpty()) {
            //no prefs set
            return new String[]{};
        }
        return prefsString.split(",");
    }

    private static String[] getPrefs(Context context, String tag) {
        return getPrefs(context, tag, "");
    }

    public static String[] getPrefsWithDefaults(Context context, String tag) {
        String defaultActions;
        switch (tag) {
            /*case InboxFragment.TAG:
                defaultActions = ADD_TO_QUEUE + "," + MARK_UNPLAYED;
                break;*/
            default:
            case EpisodesFragment.TAG:
                defaultActions = MARK_FAV + "," + START_DOWNLOAD;
                break;
        }

        return getPrefs(context, tag, defaultActions);
    }

    public void resetItemTouchHelper() {
        //prevent swipe staying if item is staying in the list
        if (itemTouchHelper != null && recyclerView != null) {
            itemTouchHelper.attachToRecyclerView(null);
            itemTouchHelper.attachToRecyclerView(recyclerView);
        }
    }

    public void show() {
        show(this::resetItemTouchHelper);
    }

    private void show(SwipeActionsDialog.PrefsCallback callback) {
        new SwipeActionsDialog(fragment.requireContext(), tag).show(callback, this::resetItemTouchHelper);
    }


}
