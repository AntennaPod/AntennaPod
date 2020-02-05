package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.os.Build;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedComponent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.glide.ApGlideSettings;

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
    public FeedComponent getItem(int position) {
        return itemAccess.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Holder holder;
        FeedComponent component = getItem(position);

        // Inflate Layout
        if (convertView == null) {
            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.searchlist_item, parent, false);
            holder.title = convertView.findViewById(R.id.txtvTitle);
            if(Build.VERSION.SDK_INT >= 23) {
                holder.title.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL);
            }
            holder.cover = convertView
                    .findViewById(R.id.imgvFeedimage);
            holder.subtitle = convertView
                    .findViewById(R.id.txtvSubtitle);

            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }
        if (component.getClass() == Feed.class) {
            final Feed feed = (Feed) component;
            holder.title.setText(feed.getTitle());
            holder.subtitle.setVisibility(View.GONE);

            Glide.with(context)
                    .load(feed.getImageLocation())
                    .apply(new RequestOptions()
                        .placeholder(R.color.light_gray)
                        .error(R.color.light_gray)
                        .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                        .fitCenter()
                        .dontAnimate())
                    .into(holder.cover);

        } else if (component.getClass() == FeedItem.class) {
            final FeedItem item = (FeedItem) component;
            holder.title.setText(item.getTitle());
            holder.subtitle.setVisibility(View.VISIBLE);

            convertView.setAlpha(item.isPlayed() ? 0.5f : 1.0f);

            Glide.with(context)
                    .load(item.getFeed().getImageLocation())
                    .apply(new RequestOptions()
                        .placeholder(R.color.light_gray)
                        .error(R.color.light_gray)
                        .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                        .fitCenter()
                        .dontAnimate())
                    .into(holder.cover);

        }

        return convertView;
    }

    static class Holder {
        ImageView cover;
        TextView title;
        TextView subtitle;
    }

    public interface ItemAccess {
        int getCount();

        FeedComponent getItem(int position);
    }

}
