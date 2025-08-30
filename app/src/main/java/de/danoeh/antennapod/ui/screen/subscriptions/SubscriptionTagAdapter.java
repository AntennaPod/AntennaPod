package de.danoeh.antennapod.ui.screen.subscriptions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedPreferences;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionTagAdapter extends RecyclerView.Adapter<SubscriptionTagAdapter.TagViewHolder> {
    private List<String> tags = new ArrayList<>();
    private String selectedTag = null;

    public SubscriptionTagAdapter() {
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
        notifyDataSetChanged();
    }

    public void setSelectedTag(String tag) {
        this.selectedTag = tag;
        notifyDataSetChanged();
    }

    public String getSelectedTag() {
        return selectedTag;
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
        if (FeedPreferences.TAG_ROOT.equals(tag)) {
            holder.chip.setText(R.string.tag_all);
        } else {
            holder.chip.setText(tag);
        }
        float dp = holder.itemView.getContext().getResources().getDisplayMetrics().density;
        holder.chip.setElevation(tag.equals(selectedTag) ? (4 * dp) : 0);
        holder.chip.setOnClickListener(v -> onTagClick(tag));
        holder.chip.setOnLongClickListener(v -> {
            onTagLongClick(tag);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return tags != null ? tags.size() : 0;
    }

    protected void onTagClick(String tag) {
    }

    protected void onTagLongClick(String tag) {
    }

    public static class TagViewHolder extends RecyclerView.ViewHolder {
        public final Chip chip;

        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
            chip = (Chip) itemView;
        }
    }
}
