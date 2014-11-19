package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedComponent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.SearchResult;

/**
 * List adapter for search activity.
 */
public class SearchlistAdapter extends BaseAdapter {

    private final Context context;
    private final ItemAccess itemAccess;


    public SearchlistAdapter(Context context, ItemAccess itemAccess) {
        this.context = context;
        this.itemAccess = itemAccess;
    }

    @Override
    public int getCount() {
        return itemAccess.getCount();
    }

    @Override
    public SearchResult getItem(int position) {
        return itemAccess.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Holder holder;
        SearchResult result = getItem(position);
        FeedComponent component = result.getComponent();

        // Inflate Layout
        if (convertView == null) {
            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.searchlist_item, parent, false);
            holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
            holder.cover = (ImageView) convertView
                    .findViewById(R.id.imgvFeedimage);
            holder.subtitle = (TextView) convertView
                    .findViewById(R.id.txtvSubtitle);

            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }
        if (component.getClass() == Feed.class) {
            final Feed feed = (Feed) component;
            holder.title.setText(feed.getTitle());
            holder.subtitle.setVisibility(View.GONE);

            Picasso.with(context)
                    .load(feed.getImageUri())
                    .fit()
                    .into(holder.cover);

        } else if (component.getClass() == FeedItem.class) {
            final FeedItem item = (FeedItem) component;
            holder.title.setText(item.getTitle());
            if (result.getSubtitle() != null) {
                holder.subtitle.setVisibility(View.VISIBLE);
                holder.subtitle.setText(result.getSubtitle());
            }

            Picasso.with(context)
                    .load(item.getFeed().getImageUri())
                    .fit()
                    .into(holder.cover);

        }

        return convertView;
    }

    static class Holder {
        ImageView cover;
        TextView title;
        TextView subtitle;
    }

    public static interface ItemAccess {
        int getCount();

        SearchResult getItem(int position);
    }

}
