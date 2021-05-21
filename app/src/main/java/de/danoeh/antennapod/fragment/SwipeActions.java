package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
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
    public static final String MARK_FAV = "MARKFAV";
    public static final String START_DOWNLOAD = "STARTDOWNLOAD";

    public static ItemTouchHelper itemTouchHelper(Fragment fragment, String tag) {
        SharedPreferences prefs = fragment.requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        //TODO
        prefs.edit().putString(PREF_SWIPEACTIONS+InboxFragment.TAG,ADD_TO_QUEUE+","+MARK_UNPLAYED).apply();
        prefs.edit().putString(PREF_SWIPEACTIONS+ EpisodesFragment.TAG,START_DOWNLOAD+","+MARK_PLAYED).apply();

        String[] leftright = prefs.getString(PREF_SWIPEACTIONS+tag,"").split(",");

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
                    case MARK_FAV:
                        DBWriter.toggleFavoriteItem(item);
                        break;
                    case START_DOWNLOAD:
                        try {
                            DownloadRequester.getInstance().downloadMedia(fragment.requireContext(), true, item);
                        } catch (DownloadRequestException e) {
                            e.printStackTrace();
                            DownloadRequestErrorDialogCreator.newRequestErrorDialog(fragment.requireContext(), e.getMessage());
                        }
                        break;
                }
            }

            @Override
            public void onChildDraw (Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive){
                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addSwipeRightBackgroundColor(
                                ContextCompat.getColor(fragment.requireContext(), actionColorFor(leftright[0])))
                        .addSwipeRightActionIcon(actionIconFor(leftright[0]))
                        .addSwipeLeftBackgroundColor(
                                ContextCompat.getColor(fragment.requireContext(), actionColorFor(leftright[1])))
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
            case MARK_FAV:
                return R.drawable.ic_star;
            case START_DOWNLOAD:
                return R.drawable.ic_download;
        }
    }
    private static int actionColorFor(String swipeAction) {
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

    public abstract class SwipeActionsDialog {

        protected Context context;

        public SwipeActionsDialog(Context context) {
            this.context = context;
        }

        private void savePrefs(String tag, String left, String right){
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(PREF_SWIPEACTIONS+tag,left+","+right).apply();

        }

        //TODO
    }
}
