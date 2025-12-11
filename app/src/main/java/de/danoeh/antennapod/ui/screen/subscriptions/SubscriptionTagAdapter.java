package de.danoeh.antennapod.ui.screen.subscriptions;

import android.app.Activity;
import android.os.Build;
import android.view.ContextMenu;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.storage.database.NavDrawerData;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionTagAdapter extends RecyclerView.Adapter<SubscriptionTagAdapter.TagViewHolder>
        implements View.OnCreateContextMenuListener {
    private final WeakReference<Activity> activityRef;
    private List<NavDrawerData.TagItem> tags = new ArrayList<>();
    private String selectedTag = null;
    private NavDrawerData.TagItem longPressedItem = null;

    public SubscriptionTagAdapter(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
    }

    public void setTags(List<NavDrawerData.TagItem> tags) {
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

    public int getSelectedTagPosition() {
        if (selectedTag == null) {
            return -1;
        }
        for (int i = 0; i < tags.size(); i++) {
            if (tags.get(i).getTitle().equals(selectedTag)) {
                return i;
            }
        }
        return -1;
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
        NavDrawerData.TagItem tag = tags.get(position);
        if (FeedPreferences.TAG_ROOT.equals(tag.getTitle())) {
            holder.chip.setText(R.string.tag_all);
        } else if (FeedPreferences
                .TAG_UNTAGGED.equals(tag.getTitle())) {
            holder.chip.setText(R.string.tag_untagged);
        } else {
            String title = tag.getTitle();
            if (title.length() > 20) {
                title = title.substring(0, 19) + "…";
            }
            holder.chip.setText(title);
        }
        holder.chip.setChecked(tag.getTitle().equals(selectedTag));
        holder.chip.setElevation(0);
        holder.chip.setOnClickListener(v -> onTagClick(tag));
        holder.chip.setOnTouchListener((v, e) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (e.isFromSource(InputDevice.SOURCE_MOUSE)
                        &&  e.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                    longPressedItem = tag;
                }
            }
            return false;
        });
        holder.chip.setOnLongClickListener(v -> {
            longPressedItem = tag;
            return false;
        });
        holder.chip.setOnCreateContextMenuListener(this);
    }

    @Override
    public int getItemCount() {
        return tags != null ? tags.size() : 0;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (longPressedItem == null
                || FeedPreferences.TAG_ROOT.equals(longPressedItem.getTitle())
                || FeedPreferences.TAG_UNTAGGED.equals(longPressedItem.getTitle())) {
            return;
        }
        MenuInflater inflater = activityRef.get().getMenuInflater();
        inflater.inflate(R.menu.nav_folder_context, menu);
        menu.setHeaderTitle(longPressedItem.getTitle());
    }

    protected void onTagClick(NavDrawerData.TagItem tag) {
    }

    public NavDrawerData.TagItem getLongPressedItem() {
        return longPressedItem;
    }

    public static class TagViewHolder extends RecyclerView.ViewHolder {
        public final Chip chip;

        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
            chip = itemView.findViewById(R.id.tag_chip);
        }
    }
}
