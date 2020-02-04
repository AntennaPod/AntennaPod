package de.danoeh.antennapod.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.event.PlaybackPositionEvent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.view.EpisodeItemViewHolder;

/**
 * List adapter for items of feeds that the user has already subscribed to.
 */
public class FeedItemlistAdapter extends BaseAdapter {

    private final ItemAccess itemAccess;
    private final MainActivity activity;
    /** true if played items should be made partially transparent */
    private final boolean makePlayedItemsTransparent;

    private int currentlyPlayingItem = -1;

    public FeedItemlistAdapter(MainActivity activity,
                               ItemAccess itemAccess,
                               boolean showFeedtitle,
                               boolean makePlayedItemsTransparent) {
        super();
        this.activity = activity;
        this.itemAccess = itemAccess;
        this.makePlayedItemsTransparent = makePlayedItemsTransparent;
    }

    @Override
    public int getCount() {
        return itemAccess.getCount();

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public FeedItem getItem(int position) {
        return itemAccess.getItem(position);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        EpisodeItemViewHolder holder;
        if (convertView == null) {
            holder = new EpisodeItemViewHolder(activity, parent);
        } else {
            holder = (EpisodeItemViewHolder) convertView.getTag();
        }

        final FeedItem item = getItem(position);
        holder.bind(item);
        holder.dragHandle.setVisibility(View.GONE);

        if (item.getMedia() != null && item.getMedia().isCurrentlyPlaying()) {
            currentlyPlayingItem = position;
        }
        return holder.itemView;
    }

    public void notifyCurrentlyPlayingItemChanged(PlaybackPositionEvent event, ListView listView) {
        if (currentlyPlayingItem != -1 && currentlyPlayingItem < getCount()) {
            View view = listView.getChildAt(currentlyPlayingItem
                    - listView.getFirstVisiblePosition() + listView.getHeaderViewsCount());
            if (view == null) {
                return;
            }
            EpisodeItemViewHolder holder = (EpisodeItemViewHolder) view.getTag();
            holder.notifyPlaybackPositionUpdated(event);
        }
    }

    public interface ItemAccess {
        int getCount();

        FeedItem getItem(int position);
    }

}
