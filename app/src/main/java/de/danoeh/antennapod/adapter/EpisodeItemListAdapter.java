package de.danoeh.antennapod.adapter;

import android.app.Activity;
import android.graphics.Color;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.fragment.ItemPagerFragment;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * List adapter for the list of new episodes.
 */
public class EpisodeItemListAdapter extends RecyclerView.Adapter<EpisodeItemViewHolder>
        implements View.OnCreateContextMenuListener {

    private final WeakReference<MainActivity> mainActivityRef;
    private List<FeedItem> episodes = new ArrayList<>();
    private FeedItem selectedItem;
    protected Set<FeedItem> checkedItems = new HashSet<>();
    final ActionMode[] actionMode = {null};
    final private  Integer selectColor = Color.LTGRAY;

    public EpisodeItemListAdapter(MainActivity mainActivity) {
        super();
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

        if (actionMode[0] == null || !checkedItems.contains(item)) {
            holder.itemView.setBackgroundColor(Color.WHITE);
        } else {
            holder.itemView.setBackgroundColor(selectColor);
            holder.secondaryActionButton.setVisibility(View.INVISIBLE);
        }

        if (actionMode[0] != null) {
            holder.secondaryActionButton.setVisibility(View.INVISIBLE);
        }
        holder.itemView.setOnClickListener(v -> {
            MainActivity activity = mainActivityRef.get();

            if (activity != null && actionMode[0] == null) {
                long[] ids = FeedItemUtil.getIds(episodes);
                int position = ArrayUtils.indexOf(ids, item.getId());
                activity.loadChildFragment(ItemPagerFragment.newInstance(ids, position));
            } else {
                if (checkedItems.contains(item)) {
                    checkedItems.remove(item);
                    holder.itemView.setBackgroundColor(Color.WHITE);
                } else {
                   checkedItems.add(item);
                   holder.itemView.setBackgroundColor(selectColor);
                }
                actionMode[0].setTitle(getTitle());
            }

        });
        holder.itemView.setOnCreateContextMenuListener(this);
        holder.itemView.setOnLongClickListener(v -> {
            selectedItem = item;
            return false;
        });

        afterBindViewHolder(holder, pos);
        holder.hideSeparatorIfNecessary();
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
        if (actionMode[0] == null) {
            MenuInflater inflater = mainActivityRef.get().getMenuInflater();
            inflater.inflate(R.menu.feeditemlist_context, menu);
            menu.setHeaderTitle(selectedItem.getTitle());
            FeedItemMenuHandler.onPrepareMenu(menu, selectedItem, R.id.skip_episode_item);
        }
    }

    public void updateItems(List<FeedItem> items) {
        episodes = items;
        notifyDataSetChanged();
    }

    /**
     * Callbacks called by subclasses for custom behavior when starting action mode
     */
    public void onCreateActionMode() {}

    /**
     * Callbacks called by subclasses for custom behavior when ending action mode
     */
    public void onDestroyActionMode_() {}

    public Set<FeedItem> getCheckedItems() {
        return checkedItems;
    }


    public void startActionMode(FeedItem selectedItem) {
        onCreateActionMode();
         notifyDataSetChanged();
        if (checkedItems.contains(selectedItem)) {
        checkedItems.remove(selectedItem);
        } else {
            checkedItems.add(selectedItem);
        }
        notifyDataSetChanged();
        actionMode[0] = getActivity().startActionMode(new ActionMode.Callback() {
            private MenuItem selectAllItem = null;
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.multi_select_options, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                mode.setTitle(getTitle());
                selectAllItem = menu.findItem(R.id.select_toggle);
                selectAllItem.setIcon(ThemeUtils.getDrawableFromAttr(getActivity(), R.attr.ic_select_all));
                selectAllItem.setTitle(R.string.select_all_label);
                toggleSelectAllIcon(selectAllItem,false);
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == R.id.select_toggle) {
                    if (checkedItems.size() == episodes.size()) {
                        checkNone();
                        toggleSelectAll(selectAllItem, false);
                    } else {
                        checkAll();
                        toggleSelectAll(selectAllItem, true);
                    }
                    mode.setTitle(getTitle());
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                onDestroyActionMode_();
                actionMode[0] = null;
                checkedItems.clear();
                notifyDataSetChanged();

            }
        });
    }

    /**
     * Closes action mode
     */
    public void close() {
        if (actionMode[0] != null) {
            actionMode[0].finish();
        }
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


    protected void toggleSelectAll(MenuItem selectAllItem, boolean toggle) {
        if (toggle) {
            checkAll();
            toggleSelectAllIcon(selectAllItem, toggle);
        } else {
            checkNone();
            toggleSelectAllIcon(selectAllItem, toggle);

        }
    }

    protected void toggleSelectAllIcon(MenuItem selectAllItem, boolean toggle) {
        if (toggle) {
            selectAllItem.setIcon(ThemeUtils.getDrawableFromAttr(getActivity(), R.attr.ic_select_none));
            selectAllItem.setTitle(R.string.deselect_all_label);
        } else {
            selectAllItem.setIcon(ThemeUtils.getDrawableFromAttr(getActivity(), R.attr.ic_select_all));
            selectAllItem.setTitle(R.string.select_all_label);

        }
    }
    protected void checkAll() {
        for (FeedItem episode : episodes) {
            if (!checkedItems.contains(episode.getId())) {
                checkedItems.add(episode);
            }
        }
        notifyDataSetChanged();
    }

    protected void checkNone() {
        checkedItems.clear();
        notifyDataSetChanged();
    }

    protected String getTitle() {
        return checkedItems.size() + " / " + episodes.size() + " selected";
    }
}
