package de.danoeh.antennapod.ui.home.settingsdialog;

import android.content.Context;
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

import de.danoeh.antennapod.R;

class HomeScreenSettingDialogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemMoveHandler {
    private static final int HEADER_VIEW = 0;
    private static final int ITEM_VIEW = 1;
    private final List<SettingsDialogItem> settingsDialogItems;
    @Nullable private Consumer<ItemViewHolder> dragListener;

    public HomeScreenSettingDialogAdapter(@NonNull Context context) {
        List<String> sectionTags = HomeUtil.getSortedSectionTags(context);
        List<String> hiddenSectionTags = HomeUtil.getHiddenSectionTags(context);

        List<SettingsDialogItem> settingsDialogItemList = new ArrayList<>();
        for (String sectionTag: sectionTags) {
            settingsDialogItemList.add(new SettingsDialogItem(SettingsDialogItem.ViewType.Section, sectionTag));
        }
        settingsDialogItemList.add(new SettingsDialogItem(SettingsDialogItem.ViewType.Header, context.getString(R.string.section_hidden)));
        for (String sectionTag: hiddenSectionTags) {
            settingsDialogItemList.add(new SettingsDialogItem(SettingsDialogItem.ViewType.Section, sectionTag));
        }

        settingsDialogItems = settingsDialogItemList;
    }

    public void setDragListener(@Nullable Consumer<ItemViewHolder> dragListener) {
        this.dragListener = dragListener;
    }

    @NonNull
    public List<String> getOrderedSectionTags() {
        List<String> orderedSectionTags = new ArrayList<>();
        for (SettingsDialogItem item: settingsDialogItems) {
            if(item.getViewType() == SettingsDialogItem.ViewType.Header)
                continue;

            orderedSectionTags.add(item.getTitle());
        }

        return orderedSectionTags;
    }

    public List<String> getHiddenSectionTags() {
        List<String> hiddenSections = new ArrayList<>();
        for (int i = settingsDialogItems.size() - 1; i >= 0; i--) {
            SettingsDialogItem item = settingsDialogItems.get(i);
            if(item.getViewType() == SettingsDialogItem.ViewType.Header)
                return hiddenSections;

            hiddenSections.add(item.getTitle());
        }

        return hiddenSections;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if(viewType == HEADER_VIEW){
            View entryView = inflater.inflate(R.layout.choose_home_screen_order_dialog_header, parent, false);
            return new HeaderViewHolder(entryView);
        }

        View entryView = inflater.inflate(R.layout.choose_home_screen_order_dialog_entry, parent, false);
        return new ItemViewHolder(entryView);
    }

    @Override
    public int getItemViewType(int position) {
        return settingsDialogItems.get(position).getViewType() == SettingsDialogItem.ViewType.Header ? HEADER_VIEW : ITEM_VIEW;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        String title = settingsDialogItems.get(position).getTitle();
        if(holder instanceof HeaderViewHolder headerViewHolder)
        {
            headerViewHolder.categoryLabel.setText(title);
        }
        else if(holder instanceof ItemViewHolder itemViewHolder) {
            itemViewHolder.nameLabel.setText(HomeUtil.getNameFromTag(itemViewHolder.nameLabel.getContext(), title));
            itemViewHolder.dragImage.setOnTouchListener((view, motionEvent) -> {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if (dragListener != null)
                        dragListener.accept(itemViewHolder);
                }
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return settingsDialogItems.size();
    }

    @Override
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

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            nameLabel = itemView.findViewById(R.id.home_screen_section_label);
            dragImage = itemView.findViewById(R.id.home_screen_section_drag_image);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        private final TextView categoryLabel;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryLabel = itemView.findViewById(R.id.header_label);
        }
    }
}
