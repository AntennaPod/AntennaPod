package de.danoeh.antennapod.ui.screen.queue;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.queue.Queue;

public class QueuesDialogRecyclerAdapter extends ListAdapter<Queue, QueuesDialogRecyclerAdapter.QueuesViewHolder> {

    private OnQueueActionsListener listener;

    public QueuesDialogRecyclerAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Queue> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Queue oldQueue, @NonNull Queue newQueue) {
            return oldQueue.getId() == newQueue.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Queue oldQueue, @NonNull Queue newQueue) {
            return oldQueue.getName().equals(newQueue.getName());
        }
    };


    public interface OnQueueActionsListener {
        void onQueueClicked(Queue queue);

        void onQueueDeleteClicked(Queue queue);
    }

    public void setOnQueueActionsListener(OnQueueActionsListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public QueuesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.queues_dialog_list_item, parent, false);
        return new QueuesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueuesViewHolder holder, int position) {
        Queue queue = getItem(position);
        holder.queueName.setText(queue.getName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQueueClicked(queue);
            }
        });

        holder.deleteQueue.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQueueDeleteClicked(queue);
            }
        });
    }

    public static class QueuesViewHolder extends RecyclerView.ViewHolder {
        private final TextView queueName;
        private final TextView queueInfo;
        private final ImageButton deleteQueue;

        public QueuesViewHolder(@NonNull View itemView) {
            super(itemView);
            queueName = itemView.findViewById(R.id.queue_name);
            queueInfo = itemView.findViewById(R.id.queue_info);
            deleteQueue = itemView.findViewById(R.id.queue_delete_button);
        }
    }

}
