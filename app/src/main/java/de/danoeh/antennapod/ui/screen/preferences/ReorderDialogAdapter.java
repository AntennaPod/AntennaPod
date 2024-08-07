package de.danoeh.antennapod.ui.screen.preferences;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.databinding.ReorderDialogEntryBinding;
import de.danoeh.antennapod.databinding.ReorderDialogHeaderBinding;

import java.util.List;

public class ReorderDialogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  {
    private static final int HEADER_VIEW = 0;
    private static final int ITEM_VIEW = 1;
    private final List<ReorderDialogItem> settingsDialogItems;
    @Nullable private Consumer<ItemViewHolder> dragListener;

    public ReorderDialogAdapter(@NonNull List<ReorderDialogItem> dialogItems) {
        settingsDialogItems = dialogItems;
    }

    public void setDragListener(@Nullable Consumer<ItemViewHolder> dragListener) {
        this.dragListener = dragListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == HEADER_VIEW) {
            ReorderDialogHeaderBinding binding = ReorderDialogHeaderBinding.inflate(inflater, parent, false);
            return new HeaderViewHolder(binding.getRoot(), binding.headerLabel);
        }

        ReorderDialogEntryBinding binding = ReorderDialogEntryBinding.inflate(inflater, parent, false);
        return new ItemViewHolder(binding.getRoot(), binding.sectionLabel, binding.dragHandle);
    }

    @Override
    public int getItemViewType(int position) {
        ReorderDialogItem.ViewType viewType = settingsDialogItems.get(position).getViewType();
        boolean isHeader = viewType == ReorderDialogItem.ViewType.Header;
        return isHeader ? HEADER_VIEW : ITEM_VIEW;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            headerViewHolder.categoryLabel.setText(settingsDialogItems.get(position).getTitle());
        } else if (holder instanceof ItemViewHolder) {
            ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
            itemViewHolder.nameLabel.setText(settingsDialogItems.get(position).getTitle());
            itemViewHolder.dragImage.setOnTouchListener((view, motionEvent) -> {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if (dragListener != null) {
                        dragListener.accept(itemViewHolder);
                    }
                }
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return settingsDialogItems.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameLabel;
        private final ImageView dragImage;

        ItemViewHolder(@NonNull View itemView, TextView nameLabel, ImageView dragImage) {
            super(itemView);
            this.nameLabel = nameLabel;
            this.dragImage = dragImage;
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {

        private final TextView categoryLabel;

        HeaderViewHolder(@NonNull View itemView, @NonNull TextView categoryLabel) {
            super(itemView);
            this.categoryLabel = categoryLabel;
        }
    }
}
