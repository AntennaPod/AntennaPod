package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class SwipeActions {
    private static final String PREF_NAME = "SwipeActionsPrefs";
    private static final String PREF_SWIPEACTIONS = "swipeactions";
    private static final String PREF_FIRSTSWIPE = "firstswipe";
    public static final String ADD_TO_QUEUE = "ADDTOQUEUE";
    public static final String MARK_PLAYED = "MARKPLAYED";
    public static final String MARK_UNPLAYED = "MARKUNPLAYED";

    public static ItemTouchHelper itemTouchHelper(Fragment fragment) {
        SharedPreferences prefs = fragment.requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_SWIPEACTIONS,MARK_PLAYED+","+MARK_PLAYED).apply();
        String[] leftright = prefs.getString(PREF_SWIPEACTIONS,"").split(",");

        if (prefs.getBoolean(PREF_FIRSTSWIPE, true) || leftright.length == 0) {
            prefs.edit().putBoolean(PREF_FIRSTSWIPE, false).apply();
            //TODO
        }

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                String action = leftright[swipeDir == ItemTouchHelper.RIGHT ? 0 : 1];

                FeedItem item = ((EpisodeItemViewHolder) viewHolder).getFeedItem();
                switch (action) {
                    case ADD_TO_QUEUE:
                        FeedItemMenuHandler.addToQueue(fragment.requireContext(),item);
                        break;
                    case MARK_PLAYED:
                        FeedItemMenuHandler.removeNewFlagWithUndo(fragment,
                                item, FeedItem.PLAYED);
                        break;
                    case MARK_UNPLAYED:
                        FeedItemMenuHandler.removeNewFlagWithUndo(fragment,
                                item, FeedItem.UNPLAYED);
                        break;
                }
            }

            @Override
            public void onChildDraw (Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive){
                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addSwipeRightBackgroundColor(actionColorFor(fragment.requireContext(), leftright[0]))
                        .addSwipeRightActionIcon(actionIconFor(leftright[0]))
                        .addSwipeLeftBackgroundColor(actionColorFor(fragment.requireContext(), leftright[1]))
                        .addSwipeLeftActionIcon(actionIconFor(leftright[1]))
                        .create()
                        .decorate();

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        return new ItemTouchHelper(simpleCallback);
    }

    private static int actionIconFor(String swipeAction) {
        switch (swipeAction) {
            case ADD_TO_QUEUE:
                return R.drawable.ic_playlist;
            case MARK_PLAYED:
                return R.drawable.ic_check;
            default:
            case MARK_UNPLAYED:
                return R.drawable.ic_check;
        }
    }
    private static int actionColorFor(Context context, String swipeAction) {
        int color;
        switch (swipeAction) {
            case ADD_TO_QUEUE:
                color = R.color.swipe_light_green_200;
            case MARK_PLAYED:
                color = R.color.swipe_light_blue_200;
            default:
            case MARK_UNPLAYED:
                color = R.color.swipe_light_blue_200;
        }
        return ContextCompat.getColor(context, color);
    }

    public abstract class SwipeActionsDialog {

        protected Context context;

        public SwipeActionsDialog(Context context) {
            this.context = context;
        }

        //TODO
    }
}
