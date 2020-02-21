package de.danoeh.antennapod.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.util.ThemeUtils;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;

/**
 * Shows a list of downloaded episodes.
 */
public class DownloadedEpisodesListAdapter extends BaseAdapter {

    private final MainActivity activity;
    private final ItemAccess itemAccess;

    public DownloadedEpisodesListAdapter(MainActivity activity, ItemAccess itemAccess) {
        super();
        this.activity = activity;
        this.itemAccess = itemAccess;
    }

    @Override
    public int getCount() {
        return itemAccess.getCount();
    }

    @Override
    public FeedItem getItem(int position) {
        return itemAccess.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        EpisodeItemViewHolder holder;
        if (convertView == null) {
            holder = new EpisodeItemViewHolder(activity, parent);
        } else {
            holder = (EpisodeItemViewHolder) convertView.getTag();
        }

        final FeedItem item = getItem(position);
        holder.bind(item);
        holder.dragHandle.setVisibility(View.GONE);
        holder.secondaryActionIcon.setImageResource(ThemeUtils.getDrawableFromAttr(activity, R.attr.content_discard));
        holder.secondaryActionButton.setOnClickListener(v -> itemAccess.onFeedItemSecondaryAction(item));
        holder.hideSeparatorIfNecessary();

        return holder.itemView;
    }

    public interface ItemAccess {
        int getCount();

        FeedItem getItem(int position);

        void onFeedItemSecondaryAction(FeedItem item);
    }
}
