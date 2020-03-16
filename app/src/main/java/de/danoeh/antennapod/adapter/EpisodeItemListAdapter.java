package de.danoeh.antennapod.adapter;

import android.app.Activity;
import android.view.ContextMenu;
import android.view.MenuInflater;
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
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * List adapter for the list of new episodes.
 */
public class EpisodeItemListAdapter extends RecyclerView.Adapter<EpisodeItemViewHolder>
        implements View.OnCreateContextMenuListener {

    private final WeakReference<MainActivity> mainActivityRef;
    private List<FeedItem> episodes = new ArrayList<>();
    private FeedItem selectedItem;

    public EpisodeItemListAdapter(MainActivity mainActivity) {
        super();
        this.mainActivityRef = new WeakReference<>(mainActivity);
    }

    public void updateItems(List<FeedItem> items) {
        episodes = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EpisodeItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EpisodeItemViewHolder(mainActivityRef.get(), parent);
    }

    @Override
    public void onBindViewHolder(EpisodeItemViewHolder holder, int pos) {
        FeedItem item = episodes.get(pos);
        holder.bind(item);
        holder.itemView.setOnLongClickListener(v -> {
            selectedItem = item;
            return false;
        });
        holder.itemView.setOnClickListener(v -> {
            MainActivity activity = mainActivityRef.get();
            if (activity != null) {
                long[] ids = FeedItemUtil.getIds(episodes);
                int position = ArrayUtils.indexOf(ids, item.getId());
                activity.loadChildFragment(ItemPagerFragment.newInstance(ids, position));
            }
        });
        holder.itemView.setOnCreateContextMenuListener(this);
        holder.hideSeparatorIfNecessary();
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

    protected FeedItem getItem(int index) {
        return episodes.get(index);
    }

    protected Activity getActivity() {
        return mainActivityRef.get();
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = mainActivityRef.get().getMenuInflater();
        inflater.inflate(R.menu.feeditemlist_context, menu);
        menu.setHeaderTitle(selectedItem.getTitle());
        FeedItemMenuHandler.onPrepareMenu(menu, selectedItem, R.id.skip_episode_item);
    }
}
