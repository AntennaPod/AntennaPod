package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.dialog.SwipeActionsDialog;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class SwipeActions {
    public static final String PREF_NAME = "SwipeActionsPrefs";
    public static final String PREF_SWIPEACTIONS = "PrefSwipeActions";
    public static final String PREF_NO_ACTION = "PrefsNoSwipeAction";

    public static final List<SwipeAction> swipeActions = Collections.unmodifiableList(
            Arrays.asList(new AddToQueueSwipeAction(), new MarkUnplayedSwipeAction(),
                    new StartDownloadSwipeAction(), new MarkFavouriteSwipeAction(),
                    new MarkPlayedSwipeAction(), new RemoveFromQueueSwipeAction())
    );

    private RecyclerView recyclerView;
    public ItemTouchHelper itemTouchHelper;
    private final Fragment fragment;
    private final String tag;
    private FeedItemFilter filter = null;

    private NewSwipeCallback newSwipeCallback = SimpleSwipeCallback::new;

    public SwipeActions(Fragment fragment, String tag) {
        this.fragment = fragment;
        this.tag = tag;
        itemTouchHelper();
    }

    public void setFilter(FeedItemFilter filter) {
        this.filter = filter;
    }

    public SwipeActions attachTo(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        itemTouchHelper.attachToRecyclerView(recyclerView);
        //reload as settings might have changed
        fragment.getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
            if (event == Lifecycle.Event.ON_RESUME) {
                resetItemTouchHelper();
            }
        });

        return this;
    }

    public void itemTouchHelper() {
        itemTouchHelper = new ItemTouchHelper(newSwipeCallback.construct());
    }

    private static List<SwipeAction> getPrefs(Context context, String tag, String defaultActions) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String prefsString = prefs.getString(PREF_SWIPEACTIONS + tag, defaultActions);

        ArrayList<SwipeAction> actions = new ArrayList<>();

        if (!prefsString.isEmpty()) {
            String[] rightleft = prefsString.split(",");

            for (String s : rightleft) {
                actions.add(Stream.of(swipeActions).filter(a -> a.id().equals(s)).single());
            }
        }

        return actions;
    }

    private static List<SwipeAction> getPrefs(Context context, String tag) {
        return getPrefs(context, tag, "");
    }

    public static List<SwipeAction> getPrefsWithDefaults(Context context, String tag) {
        String defaultActions;
        switch (tag) {
            /*case InboxFragment.TAG:
                defaultActions = new int[] {ADD_TO_QUEUE, MARK_UNPLAYED};
                break;*/
            case QueueFragment.TAG:
                defaultActions = SwipeAction.REMOVE_FROM_QUEUE + "," + SwipeAction.REMOVE_FROM_QUEUE;
                break;
            default:
            case EpisodesFragment.TAG:
                defaultActions = SwipeAction.MARK_FAV + "," + SwipeAction.START_DOWNLOAD;
                break;
        }

        return getPrefs(context, tag, defaultActions);
    }

    public static Boolean getNoActionPref(Context context, String tag) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_NO_ACTION + tag, false);
    }

    private Boolean getNoActionPref() {
        return getNoActionPref(fragment.requireContext(), tag);
    }

    public void resetItemTouchHelper() {
        //refresh itemTouchHelper after prefs changed
        if (itemTouchHelper != null) {
            itemTouchHelper.attachToRecyclerView(null);
        }
        itemTouchHelper();
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    public interface NewSwipeCallback {
        SimpleSwipeCallback construct();
    }

    public void setNewSwipeCallback(NewSwipeCallback newSwipeCallback) {
        this.newSwipeCallback = newSwipeCallback;
        itemTouchHelper();
    }

    private void showDialog(SwipeActionsDialog.Callback prefsChanged) {
        new SwipeActionsDialog(fragment.requireContext(), tag).show(prefsChanged);
    }

    public class SimpleSwipeCallback extends ItemTouchHelper.SimpleCallback {

        List<SwipeAction> rightleft = getPrefs(fragment.requireContext(), tag);
        boolean swipeOutEnabled = true;
        int swipedOutTo = 0;

        public SimpleSwipeCallback(int dragDirs) {
            super(dragDirs, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT);
        }

        public SimpleSwipeCallback() {
            this(0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int swipeDir) {
            if (rightleft.size() == 0) {
                //open settings dialog if no prefs are set
                showDialog(SwipeActions.this::resetItemTouchHelper);
                return;
            }

            FeedItem item = ((EpisodeItemViewHolder) viewHolder).getFeedItem();

            rightleft.get(swipeDir == ItemTouchHelper.RIGHT ? 0 : 1)
                    .action(item, fragment, filter);
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                float dx, float dy, int actionState, boolean isCurrentlyActive) {
            SwipeAction right;
            SwipeAction left;
            boolean hasSwipeActions = rightleft.size() > 0;
            if (hasSwipeActions) {
                right = rightleft.get(0);
                left = rightleft.get(1);
            } else {
                right = left = new ShowFirstSwipeDialogAction();
            }

            //check if it will be removed
            boolean rightWillRemove = hasSwipeActions && right.willRemove(filter);
            boolean leftWillRemove = hasSwipeActions && left.willRemove(filter);
            boolean wontLeave = (dx > 0 && !rightWillRemove) || (dx < 0 && !leftWillRemove);

            //Limit swipe if it's not removed
            int maxMovement = recyclerView.getWidth() * 2 / 5;
            float sign = dx > 0 ? 1 : -1;
            float limitMovement = Math.min(maxMovement, sign * dx);
            float displacementPercentage = limitMovement / maxMovement;

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && wontLeave) {
                swipeOutEnabled = false;

                boolean swipeThresholdReached = displacementPercentage == 1;

                // Move slower when getting near the maxMovement
                dx = sign * maxMovement * (float) Math.sin((Math.PI / 2) * displacementPercentage);

                if (isCurrentlyActive) {
                    int dir = dx > 0 ? ItemTouchHelper.RIGHT : ItemTouchHelper.LEFT;
                    swipedOutTo = swipeThresholdReached ? dir : 0;
                }
            } else {
                swipeOutEnabled = true;
            }

            //add color and icon (only if its not the very first time)
            if (hasSwipeActions) {
                Context context = fragment.requireContext();
                int themeColor = ThemeUtils.getColorFromAttr(context, android.R.attr.windowBackground);
                int actionColor = ContextCompat.getColor(context, dx > 0 ? right.actionColor() : left.actionColor());
                RecyclerViewSwipeDecorator.Builder builder = new RecyclerViewSwipeDecorator.Builder(
                        c, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive)
                        .addSwipeRightActionIcon(right.actionIcon())
                        .addSwipeLeftActionIcon(left.actionIcon())
                        .addSwipeRightBackgroundColor(ThemeUtils.getColorFromAttr(context, R.attr.background_elevated))
                        .addSwipeLeftBackgroundColor(ThemeUtils.getColorFromAttr(context, R.attr.background_elevated))
                        .setActionIconTint(
                                ColorUtils.blendARGB(themeColor,
                                        actionColor,
                                        Math.max(0.5f, displacementPercentage)));
                builder.create().decorate();
            }

            super.onChildDraw(c, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive);
        }

        @Override
        public float getSwipeEscapeVelocity(float defaultValue) {
            return swipeOutEnabled ? defaultValue : Float.MAX_VALUE;
        }

        @Override
        public float getSwipeVelocityThreshold(float defaultValue) {
            return swipeOutEnabled ? defaultValue : 0;
        }

        @Override
        public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
            return swipeOutEnabled ? 0.6f : 1.0f;
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);

            if (swipedOutTo != 0) {
                onSwiped(viewHolder, swipedOutTo);
                swipedOutTo = 0;
            }
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            if (getNoActionPref()) {
                return makeMovementFlags(getDragDirs(recyclerView, viewHolder), 0);
            } else {
                return super.getMovementFlags(recyclerView, viewHolder);
            }
        }
    }

}
