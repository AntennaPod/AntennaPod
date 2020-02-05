package de.danoeh.antennapod.adapter;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.fragment.ItemPagerFragment;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.view.EpisodeItemViewHolder;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.ref.WeakReference;

/**
 * List adapter for the queue.
 */
public class QueueRecyclerAdapter extends RecyclerView.Adapter<EpisodeItemViewHolder> implements View.OnCreateContextMenuListener {
    private static final String TAG = "QueueRecyclerAdapter";

    private final WeakReference<MainActivity> mainActivity;
    private final ItemAccess itemAccess;
    private final ItemTouchHelper itemTouchHelper;

    private boolean locked;

    private FeedItem selectedItem;

    public QueueRecyclerAdapter(MainActivity mainActivity,
                                ItemAccess itemAccess,
                                ItemTouchHelper itemTouchHelper) {
        super();
        this.mainActivity = new WeakReference<>(mainActivity);
        this.itemAccess = itemAccess;
        this.itemTouchHelper = itemTouchHelper;
        locked = UserPreferences.isQueueLocked();
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EpisodeItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EpisodeItemViewHolder(mainActivity.get(), parent);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public void onBindViewHolder(EpisodeItemViewHolder holder, int pos) {
        FeedItem item = itemAccess.getItem(pos);
        holder.bind(item);
        holder.dragHandle.setVisibility(locked ? View.GONE : View.VISIBLE);
        holder.itemView.setOnLongClickListener(v -> {
            selectedItem = item;
            return false;
        });
        holder.itemView.setOnClickListener(v -> {
            MainActivity activity = mainActivity.get();
            if (activity != null) {
                long[] ids = itemAccess.getQueueIds().toArray();
                int position = ArrayUtils.indexOf(ids, item.getId());
                activity.loadChildFragment(ItemPagerFragment.newInstance(ids, position));
            }
        });

        View.OnTouchListener startDragTouchListener = (v1, event) -> {
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                Log.d(TAG, "startDrag()");
                itemTouchHelper.startDrag(holder);
            }
            return false;
        };
        if (!locked) {
            holder.dragHandle.setOnTouchListener(startDragTouchListener);
            holder.coverHolder.setOnTouchListener(startDragTouchListener);
        } else {
            holder.dragHandle.setOnTouchListener(null);
            holder.coverHolder.setOnTouchListener(null);
        }

        holder.itemView.setOnCreateContextMenuListener(this);
        holder.isInQueue.setVisibility(View.GONE);
        holder.hideSeparatorIfNecessary();
    }

    @Nullable
    public FeedItem getSelectedItem() {
        return selectedItem;
    }

    @Override
    public long getItemId(int position) {
        FeedItem item = itemAccess.getItem(position);
        return item != null ? item.getId() : RecyclerView.NO_POSITION;
    }

    public int getItemCount() {
        return itemAccess.getCount();
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = mainActivity.get().getMenuInflater();
        inflater.inflate(R.menu.queue_context, menu); // queue-specific menu items
        inflater.inflate(R.menu.feeditemlist_context, menu); // generic menu items for item feeds

        menu.setHeaderTitle(selectedItem.getTitle());
        FeedItemMenuHandler.onPrepareMenu(menu, selectedItem, R.id.skip_episode_item);
        // Queue-specific menu preparation
        final boolean keepSorted = UserPreferences.isQueueKeepSorted();
        final LongList queueAccess = itemAccess.getQueueIds();
        if (queueAccess.size() == 0 || queueAccess.get(0) == selectedItem.getId() || keepSorted) {
            menu.findItem(R.id.move_to_top_item).setVisible(false);
        }
        if (queueAccess.size() == 0 || queueAccess.get(queueAccess.size() - 1) == selectedItem.getId() || keepSorted) {
            menu.findItem(R.id.move_to_bottom_item).setVisible(false);
        }
    }

    public interface ItemAccess {
        FeedItem getItem(int position);

        int getCount();

        LongList getQueueIds();
    }

    /**
     * Notifies a View Holder of relevant callbacks from
     * {@link ItemTouchHelper.Callback}.
     */
    public interface ItemTouchHelperViewHolder {

        /**
         * Called when the {@link ItemTouchHelper} first registers an
         * item as being moved or swiped.
         * Implementations should update the item view to indicate
         * it's active state.
         */
        void onItemSelected();


        /**
         * Called when the {@link ItemTouchHelper} has completed the
         * move or swipe, and the active item state should be cleared.
         */
        void onItemClear();
    }
}
