package de.danoeh.antennapod.adapter;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;

/**
 * List adapter for the queue.
 */
public class QueueRecyclerAdapter extends EpisodeItemListAdapter {
    private static final String TAG = "QueueRecyclerAdapter";

    private final ItemTouchHelper itemTouchHelper;
    private boolean locked;

    public QueueRecyclerAdapter(MainActivity mainActivity, ItemTouchHelper itemTouchHelper) {
        super(mainActivity);
        this.itemTouchHelper = itemTouchHelper;
        locked = UserPreferences.isQueueLocked();
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        notifyDataSetChanged();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public void onBindViewHolder(EpisodeItemViewHolder holder, int pos) {
        super.onBindViewHolder(holder, pos);
        View.OnTouchListener startDragTouchListener = (v1, event) -> {
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                Log.d(TAG, "startDrag()");
                itemTouchHelper.startDrag(holder);
            }
            return false;
        };

        if (locked) {
            holder.dragHandle.setVisibility(View.GONE);
            holder.dragHandle.setOnTouchListener(null);
            holder.coverHolder.setOnTouchListener(null);
        } else {
            holder.dragHandle.setVisibility(View.VISIBLE);
            holder.dragHandle.setOnTouchListener(startDragTouchListener);
            holder.coverHolder.setOnTouchListener(startDragTouchListener);
        }

        holder.isInQueue.setVisibility(View.GONE);
        holder.hideSeparatorIfNecessary();
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.queue_context, menu);
        super.onCreateContextMenu(menu, v, menuInfo);

        final boolean keepSorted = UserPreferences.isQueueKeepSorted();
        if (getItem(0).getId() == getSelectedItem().getId() || keepSorted) {
            menu.findItem(R.id.move_to_top_item).setVisible(false);
        }
        if (getItem(getItemCount() - 1).getId() == getSelectedItem().getId() || keepSorted) {
            menu.findItem(R.id.move_to_bottom_item).setVisible(false);
        }
    }
}
