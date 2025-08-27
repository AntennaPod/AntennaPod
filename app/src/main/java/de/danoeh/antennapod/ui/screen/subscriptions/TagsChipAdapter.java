package de.danoeh.antennapod.ui.screen.subscriptions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import java.util.List;
import de.danoeh.antennapod.R;

public class TagsChipAdapter extends RecyclerView.Adapter<TagsChipAdapter.TagViewHolder> {
    public interface OnTagClickListener {
        void onTagClick(String tag);
    }

    private final List<String> tags;
    private final OnTagClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public TagsChipAdapter(List<String> tags, OnTagClickListener listener) {
        this.tags = tags;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tag_chip, parent, false);
        return new TagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        String tag = tags.get(position);
        Chip chip = (Chip) holder.itemView;
        chip.setText(tag);
        chip.setChecked(position == selectedPosition);
        chip.setOnClickListener(v -> {
            int oldPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPosition);
            notifyItemChanged(selectedPosition);
            listener.onTagClick(tag);
        });
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    public static class TagViewHolder extends RecyclerView.ViewHolder {
        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}

