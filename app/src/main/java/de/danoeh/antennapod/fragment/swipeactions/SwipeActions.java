package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

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
    public static final String PREF_NO_ACTION = "PrefNoSwipeAction";

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
        setItemTouchHelper();
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

    public void setItemTouchHelper() {
        itemTouchHelper = new ItemTouchHelper(newSwipeCallback.construct());
    }

    private static Pair<SwipeAction, SwipeAction> getPrefs(Context context, String tag, String defaultActions) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String prefsString = prefs.getString(PREF_SWIPEACTIONS + tag, defaultActions);

        String[] rightleft = prefsString.split(",");

        Optional<SwipeAction> right = Stream.of(swipeActions)
                .filter(a -> a.id().equals(rightleft[0])).findFirst();
        Optional<SwipeAction> left = Stream.of(swipeActions)
                .filter(a -> a.id().equals(rightleft[1])).findFirst();

        //no preferences set, no default (very fist swipe) or invalid ids
        if (rightleft.length < 2 || !right.isPresent() || !left.isPresent()) {
            return null;
        }

        return new Pair<>(right.get(), left.get());
    }

    private static Pair<SwipeAction, SwipeAction> getPrefs(Context context, String tag) {
        return getPrefs(context, tag, "");
    }

    public static Pair<SwipeAction, SwipeAction> getPrefsWithDefaults(Context context, String tag) {
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

    public static boolean isSwipeActionEnabled(Context context, String tag) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_NO_ACTION + tag, true);
    }

    private Boolean isSwipeActionEnabled() {
        return isSwipeActionEnabled(fragment.requireContext(), tag);
    }

    public void detachItemTouchHelper() {
        itemTouchHelper.attachToRecyclerView(null);
    }

    public void reattachItemTouchHelper() {
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    public void resetItemTouchHelper() {
        //refresh itemTouchHelper after prefs changed
        if (itemTouchHelper != null) {
            itemTouchHelper.attachToRecyclerView(null);
        }
        setItemTouchHelper();
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    public interface NewSwipeCallback {
        SimpleSwipeCallback construct();
    }

    public void setNewSwipeCallback(NewSwipeCallback newSwipeCallback) {
        this.newSwipeCallback = newSwipeCallback;
        setItemTouchHelper();
    }

    public class SimpleSwipeCallback extends ItemTouchHelper.SimpleCallback {

        Pair<SwipeAction, SwipeAction> rightleft = getPrefs(fragment.requireContext(), tag);
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
            if (rightleft == null) {
                //open settings dialog if no prefs are set
                new SwipeActionsDialog(fragment.requireContext(), tag)
                        .show(SwipeActions.this::resetItemTouchHelper);
                return;
            }

            FeedItem item = ((EpisodeItemViewHolder) viewHolder).getFeedItem();

            if (swipeDir == ItemTouchHelper.RIGHT && rightleft.first != null) {
                rightleft.first.action(item, fragment, filter);
            } else if (swipeDir == ItemTouchHelper.LEFT && rightleft.second != null) {
                rightleft.second.action(item, fragment, filter);
            }
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                float dx, float dy, int actionState, boolean isCurrentlyActive) {
            SwipeAction right;
            SwipeAction left;
            if (rightleft != null) {
                right = rightleft.first;
                left = rightleft.second;
                if (left == null || right == null) {
                    return;
                }
            } else {
                right = left = new ShowFirstSwipeDialogAction();
            }

            //check if it will be removed
            boolean rightWillRemove = right.willRemove(filter);
            boolean leftWillRemove = left.willRemove(filter);
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

            //add color and icon
            Context context = fragment.requireContext();
            int themeColor = ThemeUtils.getColorFromAttr(context, android.R.attr.windowBackground);
            int actionColor = ThemeUtils.getColorFromAttr(context,
                    dx > 0 ? right.actionColor() : left.actionColor());
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
            if (!isSwipeActionEnabled()) {
                return makeMovementFlags(getDragDirs(recyclerView, viewHolder), 0);
            } else {
                return super.getMovementFlags(recyclerView, viewHolder);
            }
        }
    }

}
