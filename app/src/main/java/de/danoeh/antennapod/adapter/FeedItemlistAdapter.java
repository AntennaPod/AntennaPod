package de.danoeh.antennapod.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.event.PlaybackPositionEvent;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedComponent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import de.danoeh.antennapod.view.viewholder.FeedComponentViewHolder;
import de.danoeh.antennapod.view.viewholder.FeedViewHolder;

/**
 * List adapter for items of feeds that the user has already subscribed to.
 */
public class FeedItemlistAdapter extends BaseAdapter {

    private final ItemAccess itemAccess;
    private final MainActivity activity;
    private final boolean makePlayedItemsTransparent;
    private final boolean showIcons;

    private int currentlyPlayingItem = -1;

    public FeedItemlistAdapter(MainActivity activity, ItemAccess itemAccess,
                               boolean showIcons, boolean makePlayedItemsTransparent) {
        super();
        this.activity = activity;
        this.itemAccess = itemAccess;
        this.showIcons = showIcons;
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
    public FeedComponent getItem(int position) {
        return itemAccess.getItem(position);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final FeedComponent item = getItem(position);
        if (item instanceof Feed) {
            return getView((Feed) item, convertView, parent);
        } else {
            final FeedItem feeditem = (FeedItem) item;
            if (feeditem.getMedia() != null && feeditem.getMedia().isCurrentlyPlaying()) {
                currentlyPlayingItem = position;
            }
            return getView(feeditem, convertView, parent);
        }
    }

    private View getView(Feed item, View convertView, ViewGroup parent) {
        FeedViewHolder holder;
        if (convertView == null || !(convertView.getTag() instanceof FeedViewHolder)) {
            holder = new FeedViewHolder(activity, parent);
        } else {
            holder = (FeedViewHolder) convertView.getTag();
        }
        holder.bind(item);
        return holder.itemView;
    }

    private View getView(final FeedItem item, View convertView, ViewGroup parent) {
        EpisodeItemViewHolder holder;
        if (convertView == null || !(convertView.getTag() instanceof EpisodeItemViewHolder)) {
            holder = new EpisodeItemViewHolder(activity, parent);
        } else {
            holder = (EpisodeItemViewHolder) convertView.getTag();
        }

        if (!showIcons) {
            holder.coverHolder.setVisibility(View.GONE);
        }

        holder.bind(item);
        holder.dragHandle.setVisibility(View.GONE);

        if (!makePlayedItemsTransparent) {
            holder.itemView.setAlpha(1.0f);
        }

        holder.hideSeparatorIfNecessary();
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

        FeedComponent getItem(int position);
    }

}
