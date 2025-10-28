package de.danoeh.antennapod.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.model.feed.Queue;

/**
 * RecyclerView adapter for displaying queue list with visual styling.
 *
 * <p>This adapter displays each queue as a styled button with:
 * - Color indicator (circle or left border) showing queue color
 * - Queue icon from Material Design icon set
 * - Queue name with text truncation
 * - Active indicator (checkmark) when queue is active
 * - Delete button (visible on long-press or in edit mode)
 *
 * <p>Supports click and long-click events for queue selection and deletion.
 *
 * <p>Usage:
 * <pre>
 * QueueButtonAdapter adapter = new QueueButtonAdapter(
 *     activeQueueId,
 *     this::onQueueClicked,
 *     this::onQueueLongClicked
 * );
 * recyclerView.setAdapter(adapter);
 * adapter.setQueues(queueList);
 * </pre>
 */
public class QueueButtonAdapter extends RecyclerView.Adapter<QueueButtonAdapter.QueueButtonViewHolder> {

    private List<Queue> queues = new ArrayList<>();
    private long activeQueueId = -1;
    private boolean showDeleteButtons = false;

    private final OnQueueClickListener clickListener;
    private final OnQueueLongClickListener longClickListener;

    /**
     * Interface for queue click events.
     */
    public interface OnQueueClickListener {
        /**
         * Called when a queue button is clicked.
         *
         * @param queue The clicked queue
         */
        void onQueueClick(@NonNull Queue queue);
    }

    /**
     * Interface for queue long-click events.
     */
    public interface OnQueueLongClickListener {
        /**
         * Called when a queue button is long-clicked.
         *
         * @param queue The long-clicked queue
         * @return True if the long click was handled, false otherwise
         */
        boolean onQueueLongClick(@NonNull Queue queue);
    }

    /**
     * Constructs a new QueueButtonAdapter.
     *
     * @param activeQueueId     ID of the currently active queue
     * @param clickListener     Listener for queue click events
     * @param longClickListener Listener for queue long-click events
     */
    public QueueButtonAdapter(long activeQueueId,
                              @NonNull OnQueueClickListener clickListener,
                              @Nullable OnQueueLongClickListener longClickListener) {
        this.activeQueueId = activeQueueId;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public QueueButtonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.queue_button_item, parent, false);
        return new QueueButtonViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueButtonViewHolder holder, int position) {
        Queue queue = queues.get(position);
        holder.bind(queue);
    }

    @Override
    public int getItemCount() {
        return queues.size();
    }

    /**
     * Updates the queue list and refreshes the display.
     *
     * @param queues New list of queues to display
     */
    public void setQueues(@NonNull List<Queue> queues) {
        this.queues = new ArrayList<>(queues);
        notifyDataSetChanged();
    }

    /**
     * Updates the active queue ID and refreshes displays.
     *
     * @param activeQueueId ID of the currently active queue
     */
    public void setActiveQueueId(long activeQueueId) {
        long oldActiveId = this.activeQueueId;
        this.activeQueueId = activeQueueId;

        // Notify items that changed active status
        for (int i = 0; i < queues.size(); i++) {
            Queue queue = queues.get(i);
            if (queue.getId() == oldActiveId || queue.getId() == activeQueueId) {
                notifyItemChanged(i);
            }
        }
    }

    /**
     * Shows or hides delete buttons for all queue items.
     *
     * @param show True to show delete buttons, false to hide
     */
    public void setShowDeleteButtons(boolean show) {
        if (this.showDeleteButtons != show) {
            this.showDeleteButtons = show;
            notifyDataSetChanged();
        }
    }

    /**
     * Gets whether delete buttons are currently shown.
     *
     * @return True if delete buttons are visible, false otherwise
     */
    public boolean isShowDeleteButtons() {
        return showDeleteButtons;
    }

    /**
     * ViewHolder for queue button items.
     *
     * <p>Binds queue data to UI elements and handles user interactions.
     */
    class QueueButtonViewHolder extends RecyclerView.ViewHolder {
        private final View colorIndicator;
        private final ImageView icon;
        private final TextView name;
        private final ImageView activeIndicator;
        private final ImageButton deleteButton;

        QueueButtonViewHolder(@NonNull View itemView) {
            super(itemView);
            colorIndicator = itemView.findViewById(R.id.color_indicator);
            icon = itemView.findViewById(R.id.queue_icon);
            name = itemView.findViewById(R.id.queue_name);
            activeIndicator = itemView.findViewById(R.id.active_indicator);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }

        /**
         * Binds queue data to UI elements.
         *
         * @param queue Queue to display
         */
        void bind(@NonNull Queue queue) {
            // Set queue name
            name.setText(queue.getName());

            // Set color indicator to default color (color customization removed for MVP)
            int color = QueueGradientHeader.getDefaultColor();
            colorIndicator.setBackgroundColor(color);

            // Set icon (placeholder for now - requires icon resource mapping)
            // In a full implementation, this would map queue.getIcon() to a drawable resource
            // For now, use a default icon
            // icon.setImageResource(resolveIconResource(queue.getIcon()));

            // Show/hide active indicator
            boolean isActive = queue.getId() == activeQueueId;
            activeIndicator.setVisibility(isActive ? View.VISIBLE : View.GONE);

            // Show/hide delete button
            // Note: Default queue status is managed by repository, not stored in Queue object
            // For now, always show delete button - UI/repository prevents deletion of default queue
            boolean canShowDelete = showDeleteButtons;
            deleteButton.setVisibility(canShowDelete ? View.VISIBLE : View.GONE);

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onQueueClick(queue);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    return longClickListener.onQueueLongClick(queue);
                }
                return false;
            });

            deleteButton.setOnClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onQueueLongClick(queue);
                }
            });
        }

        /**
         * Resolves queue icon string to drawable resource ID.
         *
         * <p>This is a placeholder for the full implementation which would
         * map Material Design icon names to their resource IDs.
         *
         * @param iconName Icon name from queue
         * @return Drawable resource ID
         */
        @SuppressWarnings("unused")
        private int resolveIconResource(@NonNull String iconName) {
            // Placeholder implementation
            // Full implementation would use a map or switch statement:
            // switch (iconName) {
            //     case "ic_queue_music_24dp": return R.drawable.ic_queue_music_24dp;
            //     case "ic_directions_run_24dp": return R.drawable.ic_directions_run_24dp;
            //     // ... more icons
            //     default: return R.drawable.ic_playlist_play_24dp;
            // }
            return R.drawable.ic_playlist_play_24dp;
        }
    }

    /**
     * Gets the queue at the specified position.
     *
     * @param position Position in the list
     * @return Queue at position, or null if position is invalid
     */
    @Nullable
    public Queue getQueueAtPosition(int position) {
        if (position >= 0 && position < queues.size()) {
            return queues.get(position);
        }
        return null;
    }

    /**
     * Finds the position of a queue by ID.
     *
     * @param queueId Queue ID to find
     * @return Position of queue, or -1 if not found
     */
    public int findQueuePosition(long queueId) {
        for (int i = 0; i < queues.size(); i++) {
            if (queues.get(i).getId() == queueId) {
                return i;
            }
        }
        return -1;
    }
}
