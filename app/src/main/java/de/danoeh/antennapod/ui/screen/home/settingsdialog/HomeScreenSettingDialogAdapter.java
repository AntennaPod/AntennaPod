package de.danoeh.antennapod.ui.screen.home.settingsdialog;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.databinding.ChooseHomeScreenOrderDialogEntryBinding;
import de.danoeh.antennapod.databinding.ChooseHomeScreenOrderDialogHeaderBinding;

class HomeScreenSettingDialogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  {
    private static final int HEADER_VIEW = 0;
    private static final int ITEM_VIEW = 1;
    private final List<HomeScreenSettingsDialogItem> settingsDialogItems;
    @Nullable private Consumer<ItemViewHolder> dragListener;

    public HomeScreenSettingDialogAdapter(@NonNull List<HomeScreenSettingsDialogItem> dialogItems) {
        settingsDialogItems = dialogItems;
    }

    public void setDragListener(@Nullable Consumer<ItemViewHolder> dragListener) {
        this.dragListener = dragListener;
    }

    @NonNull
    public List<String> getOrderedSectionTags() {
        List<String> orderedSectionTags = new ArrayList<>();
        for (HomeScreenSettingsDialogItem item: settingsDialogItems) {
            if (item.getViewType() == HomeScreenSettingsDialogItem.ViewType.Header) {
                continue;
            }

            orderedSectionTags.add(item.getTitle());
        }

        return orderedSectionTags;
    }

    public List<String> getHiddenSectionTags() {
        List<String> hiddenSections = new ArrayList<>();
        for (int i = settingsDialogItems.size() - 1; i >= 0; i--) {
            HomeScreenSettingsDialogItem item = settingsDialogItems.get(i);
            if (item.getViewType() == HomeScreenSettingsDialogItem.ViewType.Header) {
                return hiddenSections;
            }

            hiddenSections.add(item.getTitle());
        }

        Collections.reverse(hiddenSections);
        return hiddenSections;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == HEADER_VIEW) {
            ChooseHomeScreenOrderDialogHeaderBinding binding = ChooseHomeScreenOrderDialogHeaderBinding.inflate(
                    inflater, parent, false);
            return new HeaderViewHolder(binding.getRoot(), binding.headerLabel);
        }

        ChooseHomeScreenOrderDialogEntryBinding binding = ChooseHomeScreenOrderDialogEntryBinding.inflate(
                inflater, parent, false);
        return new ItemViewHolder(binding.getRoot(), binding.sectionLabel, binding.dragHandle);
    }

    @Override
    public int getItemViewType(int position) {
        HomeScreenSettingsDialogItem.ViewType viewType = settingsDialogItems.get(position).getViewType();
        boolean isHeader = viewType == HomeScreenSettingsDialogItem.ViewType.Header;
        return isHeader ? HEADER_VIEW : ITEM_VIEW;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        String title = settingsDialogItems.get(position).getTitle();
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            headerViewHolder.categoryLabel.setText(title);
        } else if (holder instanceof ItemViewHolder) {
            ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
            String sectionName = HomePreferences.getNameFromTag(itemViewHolder.nameLabel.getContext(), title);
            itemViewHolder.nameLabel.setText(sectionName);
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

    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(settingsDialogItems, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(settingsDialogItems, i, i - 1);
            }
        }

        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameLabel;
        private final ImageView dragImage;

        ItemViewHolder(@NonNull View itemView, TextView nameLabel, ImageView dragImage) {
            super(itemView);
            this.nameLabel = nameLabel;
            this.dragImage = dragImage;
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        private final TextView categoryLabel;

        HeaderViewHolder(@NonNull View itemView, @NonNull TextView categoryLabel) {
            super(itemView);
            this.categoryLabel = categoryLabel;
        }
    }
}
