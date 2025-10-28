package de.danoeh.antennapod.ui.screen.queue;

import android.content.res.Resources;
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
import de.danoeh.antennapod.ui.common.Converter;

public class QueuesDialogRecyclerAdapter extends ListAdapter<QueueInfo, QueuesDialogRecyclerAdapter.QueuesViewHolder> {

    private OnQueueActionsListener listener;

    public QueuesDialogRecyclerAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<QueueInfo> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull QueueInfo oldInfo, @NonNull QueueInfo newInfo) {
            return oldInfo.getQueue().getId() == newInfo.getQueue().getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull QueueInfo oldInfo, @NonNull QueueInfo newInfo) {
            return oldInfo.getQueue().getName().equals(newInfo.getQueue().getName())
                    && oldInfo.getItemCount() == newInfo.getItemCount()
                    && oldInfo.getTimeLeft() == newInfo.getTimeLeft();
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
        QueueInfo queueInfo = getItem(position);
        Queue queue = queueInfo.getQueue();
        holder.queueName.setText(queue.getName());

        Resources res = holder.itemView.getContext().getResources();
        String info = res.getQuantityString(R.plurals.num_episodes, queueInfo.getItemCount(), queueInfo.getItemCount());
        if (queueInfo.getItemCount() > 0) {
            info += " • ";
            info += Converter.getDurationStringLocalized(res, queueInfo.getTimeLeft(), false);
        }
        holder.queueInfo.setText(info);

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

        if (queue.getId() == 1) {
            holder.deleteQueue.setVisibility(View.INVISIBLE);
        } else {
            holder.deleteQueue.setVisibility(View.VISIBLE);
        }
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
