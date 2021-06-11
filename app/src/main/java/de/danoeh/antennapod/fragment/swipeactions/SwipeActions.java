package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.dialog.SwipeActionsDialog;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class SwipeActions {
    public static final String PREF_NAME = "SwipeActionsPrefs";
    public static final String PREF_SWIPEACTION_RIGHT = "PrefsSwipeActionRight";
    public static final String PREF_SWIPEACTION_LEFT = "PrefsSwipeActionLeft";

    public static final int ADD_TO_QUEUE = 0;
    public static final int MARK_UNPLAYED = 1;
    public static final int START_DOWNLOAD = 2;
    public static final int MARK_FAV = 3;
    public static final int MARK_PLAYED = 4;
    //indexes of swipeActions
    public static final List<SwipeAction> swipeActions = Arrays.asList(new AddToQueue(),
            new MarkUnplayed(), new StartDownload(),
            new MarkFavourite(), new MarkPlayed());

    RecyclerView recyclerView;
    ItemTouchHelper itemTouchHelper;
    Fragment fragment;
    String tag;
    FeedItemFilter filter = null;

    public SwipeActions(Fragment fragment, String tag) {
        this.fragment = fragment;
        this.tag = tag;
        itemTouchHelper();
    }

    public void setFilter (FeedItemFilter filter) {
        this.filter = filter;
    }

    public SwipeActions attachTo(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        itemTouchHelper.attachToRecyclerView(recyclerView);
        return this;
    }

    private void itemTouchHelper() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {

            int[] rightleft = getPrefs(fragment.requireContext(), tag);

            @Override
            public boolean onMove(RecyclerView  recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                if (rightleft.length == 0) {
                    //open settings dialog if no prefs are set
                    showDialog(() -> rightleft = getPrefs(fragment.requireContext(), tag));
                    return;
                }

                FeedItem item = ((EpisodeItemViewHolder) viewHolder).getFeedItem();

                int index = rightleft[swipeDir == ItemTouchHelper.RIGHT ? 0 : 1];
                swipeActions.get(index).action(item, fragment, filter);
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder,
                                    float dx, float dy, int actionState, boolean isCurrentlyActive) {
                //display only if preferences are set
                if (rightleft.length > 0) {
                    SwipeAction right = swipeActions.get(rightleft[0]);
                    SwipeAction left = swipeActions.get(rightleft[1]);

                    new RecyclerViewSwipeDecorator.Builder(
                            c, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive)
                            .addSwipeRightBackgroundColor(
                                    ContextCompat.getColor(fragment.requireContext(), right.actionColor()))
                            .addSwipeRightActionIcon(right.actionIcon())
                            .addSwipeLeftBackgroundColor(
                                    ContextCompat.getColor(fragment.requireContext(), left.actionColor()))
                            .addSwipeLeftActionIcon(left.actionIcon())
                            .create()
                            .decorate();
                }

                super.onChildDraw(c, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive);
            }
        };

        itemTouchHelper = new ItemTouchHelper(simpleCallback);
    }

    private static int[] getPrefs(Context context, String tag, int[] defaultActions) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int prefRight = prefs.getInt(PREF_SWIPEACTION_RIGHT + tag, defaultActions[0]);
        int prefLeft = prefs.getInt(PREF_SWIPEACTION_LEFT + tag, defaultActions[1]);
        if (prefRight == -1 || prefLeft == -1 || prefRight > swipeActions.size() || prefLeft > swipeActions.size()) {
            //no prefs set or out of bounds
            return new int[]{};
        }
        return new int[] {prefRight, prefLeft};
    }

    private static int[] getPrefs(Context context, String tag) {
        return getPrefs(context, tag, new int[] {-1, -1});
    }

    public static int[] getPrefsWithDefaults(Context context, String tag) {
        int[] defaultActions;
        switch (tag) {
            /*case InboxFragment.TAG:
                defaultActions = new int[] {ADD_TO_QUEUE, MARK_UNPLAYED};
                break;*/
            default:
            case EpisodesFragment.TAG:
                defaultActions = new int[] {MARK_FAV, START_DOWNLOAD};
                break;
        }

        return getPrefs(context, tag, defaultActions);
    }

    public void reattachItemTouchHelper() {
        //prevent swipe staying if item is staying in the list
        if (itemTouchHelper != null && recyclerView != null) {
            itemTouchHelper.attachToRecyclerView(null);
            itemTouchHelper.attachToRecyclerView(recyclerView);
        }
    }

    public void refreshItemTouchHelper() {
        //refresh itemTouchHelper after prefs changed
        if (itemTouchHelper != null) {
            itemTouchHelper.attachToRecyclerView(null);
        }
        itemTouchHelper();
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    public void showDialog() {
        showDialog(this::refreshItemTouchHelper);
    }

    private void showDialog(SwipeActionsDialog.Callback prefsChanged) {
        new SwipeActionsDialog(fragment.requireContext(), tag).show(prefsChanged, this::reattachItemTouchHelper);
    }


}
