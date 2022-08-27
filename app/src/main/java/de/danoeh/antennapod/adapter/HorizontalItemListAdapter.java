package de.danoeh.antennapod.adapter;

import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.fragment.ItemPagerFragment;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.view.viewholder.HorizontalItemViewHolder;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class HorizontalItemListAdapter extends RecyclerView.Adapter<HorizontalItemViewHolder>
        implements View.OnCreateContextMenuListener {
    private final WeakReference<MainActivity> mainActivityRef;
    private List<FeedItem> data = new ArrayList<>();
    private FeedItem longPressedItem;
    private int dummyViews = 0;

    public HorizontalItemListAdapter(MainActivity mainActivity) {
        this.mainActivityRef = new WeakReference<>(mainActivity);
        setHasStableIds(true);
    }

    public void setDummyViews(int dummyViews) {
        this.dummyViews = dummyViews;
    }

    public void updateData(List<FeedItem> newData) {
        data = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HorizontalItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new HorizontalItemViewHolder(mainActivityRef.get(), parent);
    }

    @Override
    public void onBindViewHolder(@NonNull HorizontalItemViewHolder holder, int position) {
        if (position >= data.size()) {
            holder.bindDummy();
            return;
        }

        final FeedItem item = data.get(position);
        holder.bind(item);

        holder.card.setOnCreateContextMenuListener(this);
        holder.card.setOnLongClickListener(v -> {
            longPressedItem = item;
            return false;
        });
        holder.secondaryActionIcon.setOnCreateContextMenuListener(this);
        holder.secondaryActionIcon.setOnLongClickListener(v -> {
            longPressedItem = item;
            return false;
        });
        holder.card.setOnClickListener(v -> {
            MainActivity activity = mainActivityRef.get();
            if (activity != null) {
                long[] ids = FeedItemUtil.getIds(data);
                int clickPosition = ArrayUtils.indexOf(ids, item.getId());
                activity.loadChildFragment(ItemPagerFragment.newInstance(ids, clickPosition));
            }
        });
    }

    @Override
    public long getItemId(int position) {
        if (position >= data.size()) {
            return RecyclerView.NO_ID; // Dummy views
        }
        return data.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return dummyViews + data.size();
    }

    @Override
    public void onViewRecycled(@NonNull HorizontalItemViewHolder holder) {
        super.onViewRecycled(holder);
        // Set all listeners to null. This is required to prevent leaking fragments that have set a listener.
        // Activity -> recycledViewPool -> ViewHolder -> Listener -> Fragment (can not be garbage collected)
        holder.card.setOnClickListener(null);
        holder.card.setOnCreateContextMenuListener(null);
        holder.card.setOnLongClickListener(null);
        holder.secondaryActionIcon.setOnClickListener(null);
        holder.secondaryActionIcon.setOnCreateContextMenuListener(null);
        holder.secondaryActionIcon.setOnLongClickListener(null);
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
    public void onCreateContextMenu(final ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = mainActivityRef.get().getMenuInflater();
        if (longPressedItem == null) {
            return;
        }
        menu.clear();
        inflater.inflate(R.menu.feeditemlist_context, menu);
        menu.setHeaderTitle(longPressedItem.getTitle());
        FeedItemMenuHandler.onPrepareMenu(menu, longPressedItem, R.id.skip_episode_item);
    }


}
