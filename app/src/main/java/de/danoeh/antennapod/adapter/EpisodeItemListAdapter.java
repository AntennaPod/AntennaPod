package de.danoeh.antennapod.adapter;

import android.app.Activity;
import android.graphics.Color;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.FeedItem;
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
    private Set<Long> selectedIds = new HashSet<>();
    private List<FeedItem> selectedItems = new ArrayList<>();
    private FeedItem selectedItem;
    int selectedPosition = 0; // used to init actionMode
    private OnEndSelectModeListener onEndSelectModeListener;

    public EpisodeItemListAdapter(MainActivity mainActivity) {
        super(mainActivity);
        this.mainActivityRef = new WeakReference<>(mainActivity);
        setHasStableIds(true);
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
                selectHandler(pos);
            }
        });
        holder.itemView.setOnCreateContextMenuListener(this);
        holder.itemView.setOnLongClickListener(v -> {
            selectedItem = item;
            selectedPosition = pos;
            return false;
        });


        afterBindViewHolder(holder, pos);
        holder.hideSeparatorIfNecessary();
        super.onBindViewHolder(holder, pos);

    }

    @Override
    public void onViewRecycled(@NonNull EpisodeItemViewHolder holder) {
        super.onViewRecycled(holder);
        // Set all listeners to null. This is required to prevent leaking fragments that have set a listener.
        // Activity -> recycledViewPool -> EpisodeItemViewHolder -> Listener -> Fragment (can not be garbage collected)
        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnCreateContextMenuListener(null);
        holder.itemView.setOnLongClickListener(null);
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
    public FeedItem getSelectedItem() {
        return selectedItem;
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

    @Override
    public void onCreateContextMenu(final ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (!inActionMode()) {
            MenuInflater inflater = mainActivityRef.get().getMenuInflater();
            inflater.inflate(R.menu.feeditemlist_context, menu);
            menu.setHeaderTitle(selectedItem.getTitle());
            FeedItemMenuHandler.onPrepareMenu(menu, selectedItem, R.id.skip_episode_item);
        }
    }

    @Override
    public void onSelectChanged(int pos, boolean selected) {
        super.onSelectChanged(pos, selected);
        FeedItem item = episodes.get(pos);
        if (selected && !selectedIds.contains(item.getId())) {
            selectedIds.add(item.getId());
            selectedItems.add(item);
        } else if(!selected){
            selectedIds.remove(item.getId());
            selectedItems.remove(item);
        }
    }

    public void updateItems(List<FeedItem> items) {
        episodes = items;
        notifyDataSetChanged();
    }

    public void setOnEndSelectModeListener(OnEndSelectModeListener onEndSelectModeListener) {
       this.onEndSelectModeListener = onEndSelectModeListener;
    }

    protected void beforeBindViewHolder(EpisodeItemViewHolder holder, int pos) {
    }

    protected void afterBindViewHolder(EpisodeItemViewHolder holder, int pos) {
    }

    protected FeedItem getItem(int index) {
        return episodes.get(index);
    }

    protected Activity getActivity() {
        return mainActivityRef.get();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public List<FeedItem> getSelectedItems() {
        return selectedItems;
    }

    @Override
    protected void onEndActionMode() {
        super.onEndActionMode();
        selectedItems.clear();
        selectedIds.clear();
        if(onEndSelectModeListener != null) onEndSelectModeListener.onEndSelectMode();
    }

    public interface OnEndSelectModeListener {
        public void onEndSelectMode();
    }

}
