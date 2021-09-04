package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.NavDrawerData;

public class FeedTagAdapter extends RecyclerView.Adapter<FeedTagAdapter.TagViewHolder> {
    private List<NavDrawerData.FolderDrawerItem> feedFolders;
    private NavDrawerData.FolderDrawerItem defaultAll;
    public FeedTagAdapter(Context context, List<NavDrawerData.FolderDrawerItem> feedFolders) {
        this.defaultAll = new NavDrawerData.FolderDrawerItem(context.getString(R.string.tag_all));
        this.feedFolders = feedFolders;
        defaultAll.id = RecyclerView.NO_ID;
        init();
    }
    private void init() {
        if (this.feedFolders.size() == 0) {
            this.feedFolders.add(defaultAll);
        }
    }

    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View itemView = layoutInflater.inflate(R.layout.feed_tag, parent, false);

        return new TagViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        holder.bind(feedFolders.get(position));
    }

    @Override
    public int getItemCount() {
        return feedFolders.size();
    }

    public boolean isEmpyty() {
        if (feedFolders.size() == 1 && feedFolders.get(0).id == defaultAll.id) {
            return true;

        } else {
            return false;
        }
    }

    public List<NavDrawerData.FolderDrawerItem> getFeedFolders() {
        return feedFolders;
    }

    public void addItem(NavDrawerData.FolderDrawerItem folderDrawerItem) {
        feedFolders.add(folderDrawerItem);
        if (feedFolders.size() > 1) {
            feedFolders.remove(defaultAll);
        }
        notifyDataSetChanged();
    }

    public void removeItem(NavDrawerData.FolderDrawerItem folderDrawerItem) {
        this.feedFolders.remove(folderDrawerItem);
        if (feedFolders.size() == 0) {
            feedFolders.add(defaultAll);
        }
        notifyDataSetChanged();
    }

    public void clear() {
        feedFolders.clear();
        init();
        notifyDataSetChanged();
    }

    public class TagViewHolder extends RecyclerView.ViewHolder {
        private Chip chip;
        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
            chip = itemView.findViewById(R.id.feedChip);
        }

        public void bind(NavDrawerData.FolderDrawerItem folderDrawerItem) {
            chip.setText(folderDrawerItem.name);
        }
    }
}
