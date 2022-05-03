package de.danoeh.antennapod.adapter;

import android.app.Activity;
import android.os.Build;
import android.view.ContextMenu;
import android.view.InputDevice;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.fragment.ItemPagerFragment;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;

/**
 * List adapter for the list of new episodes.
 */
public class EpisodeItemListAdapter extends SelectableAdapter<EpisodeItemViewHolder>
        implements View.OnCreateContextMenuListener {

    private final WeakReference<MainActivity> mainActivityRef;
    private List<FeedItem> episodes = new ArrayList<>();
    private FeedItem longPressedItem;
    int longPressedPosition = 0; // used to init actionMode

    public EpisodeItemListAdapter(MainActivity mainActivity) {
        super(mainActivity);
        this.mainActivityRef = new WeakReference<>(mainActivity);
        setHasStableIds(true);
    }

    public void updateItems(List<FeedItem> items) {
        episodes = items;
        notifyDataSetChanged();
    }

    @Override
    public final int getItemViewType(int position) {
        return R.id.view_type_episode_item;
    }

    @NonNull
    @Override
    public final EpisodeItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EpisodeItemViewHolder(mainActivityRef.get(), parent);
    }

    @Override
    public final void onBindViewHolder(EpisodeItemViewHolder holder, int pos) {
        // Reset state of recycled views
        holder.coverHolder.setVisibility(View.VISIBLE);
        holder.dragHandle.setVisibility(View.GONE);

        beforeBindViewHolder(holder, pos);

        FeedItem item = episodes.get(pos);
        holder.bind(item);

        holder.itemView.setOnClickListener(v -> {
            MainActivity activity = mainActivityRef.get();
            if (activity != null && !inActionMode()) {
                long[] ids = FeedItemUtil.getIds(episodes);
                int position = ArrayUtils.indexOf(ids, item.getId());
                activity.loadChildFragment(ItemPagerFragment.newInstance(ids, position));
            } else {
                toggleSelection(holder.getBindingAdapterPosition());
            }
        });
        holder.itemView.setOnCreateContextMenuListener(this);
        holder.itemView.setOnLongClickListener(v -> {
            longPressedItem = item;
            longPressedPosition = holder.getBindingAdapterPosition();
            return false;
        });
        holder.itemView.setOnTouchListener((v, e) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (e.isFromSource(InputDevice.SOURCE_MOUSE)
                        && e.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                    longPressedItem = item;
                    longPressedPosition = holder.getBindingAdapterPosition();
                    return false;
                }
            }
            return false;
        });

        if (inActionMode()) {
            holder.secondaryActionButton.setVisibility(View.GONE);
            holder.selectCheckBox.setOnClickListener(v -> toggleSelection(holder.getBindingAdapterPosition()));
            holder.selectCheckBox.setChecked(isSelected(pos));
            holder.selectCheckBox.setVisibility(View.VISIBLE);
        } else {
            holder.selectCheckBox.setVisibility(View.GONE);
        }

        afterBindViewHolder(holder, pos);
        holder.hideSeparatorIfNecessary();
    }

    protected void beforeBindViewHolder(EpisodeItemViewHolder holder, int pos) {
    }

    protected void afterBindViewHolder(EpisodeItemViewHolder holder, int pos) {
    }

    @Override
    public void onViewRecycled(@NonNull EpisodeItemViewHolder holder) {
        super.onViewRecycled(holder);
        // Set all listeners to null. This is required to prevent leaking fragments that have set a listener.
        // Activity -> recycledViewPool -> EpisodeItemViewHolder -> Listener -> Fragment (can not be garbage collected)
        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnCreateContextMenuListener(null);
        holder.itemView.setOnLongClickListener(null);
        holder.itemView.setOnTouchListener(null);
        holder.secondaryActionButton.setOnClickListener(null);
        holder.dragHandle.setOnTouchListener(null);
        holder.coverHolder.setOnTouchListener(null);
    }

    /**
     * {@link #notifyItemChanged(int)} is final, so we can not override.
     * Calling {@link #notifyItemChanged(int)} may bind the item to a new ViewHolder and execute a transition.
     * This causes flickering and breaks the download animation that stores the old progress in the View.
     * Instead, we tell the adapter to use partial binding by calling {@link #notifyItemChanged(int, Object)}.
     * We actually ignore the payload and always do a full bind but calling the partial bind method ensures
     * that ViewHolders are always re-used.
     *
     * @param position Position of the item that has changed
     */
    public void notifyItemChangedCompat(int position) {
        notifyItemChanged(position, "foo");
    }

    @Nullable
    public FeedItem getLongPressedItem() {
        return longPressedItem;
    }

    @Override
    public long getItemId(int position) {
        FeedItem item = episodes.get(position);
        return item != null ? item.getId() : RecyclerView.NO_POSITION;
    }

    @Override
    public int getItemCount() {
        return episodes.size();
    }

    protected FeedItem getItem(int index) {
        return episodes.get(index);
    }

    protected Activity getActivity() {
        return mainActivityRef.get();
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = mainActivityRef.get().getMenuInflater();
        if (inActionMode()) {
            inflater.inflate(R.menu.multi_select_context_popup, menu);
        } else {
            if (longPressedItem == null) {
                return;
            }
            inflater.inflate(R.menu.feeditemlist_context, menu);
            menu.setHeaderTitle(longPressedItem.getTitle());
            FeedItemMenuHandler.onPrepareMenu(menu, longPressedItem, R.id.skip_episode_item);
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.multi_select) {
            startSelectMode(longPressedPosition);
            return true;
        } else if (item.getItemId() == R.id.select_all_above) {
            setSelected(0, longPressedPosition, true);
            return true;
        } else if (item.getItemId() == R.id.select_all_below) {
            setSelected(longPressedPosition + 1, getItemCount(), true);
            return true;
        }
        return false;
    }

    public List<FeedItem> getSelectedItems() {
        List<FeedItem> items = new ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            if (isSelected(i)) {
                items.add(getItem(i));
            }
        }
        return items;
    }

}
