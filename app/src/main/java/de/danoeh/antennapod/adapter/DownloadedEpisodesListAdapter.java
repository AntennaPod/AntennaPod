package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.util.Converter;

/**
 * Shows a list of downloaded episodes
 */
public class DownloadedEpisodesListAdapter extends BaseAdapter {

    private final Context context;
    private final ItemAccess itemAccess;

    private final int imageSize;

    public DownloadedEpisodesListAdapter(Context context, ItemAccess itemAccess) {
        super();
        this.context = context;
        this.itemAccess = itemAccess;
        this.imageSize = (int) context.getResources().getDimension(R.dimen.thumbnail_length_downloaded_item);
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
        Holder holder;
        final FeedItem item = (FeedItem) getItem(position);
        if (item == null) return null;

        if (convertView == null) {
            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.downloaded_episodeslist_item,
                    parent, false);
            holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
            holder.pubDate = (TextView) convertView
                    .findViewById(R.id.txtvPublished);
            holder.butSecondary = (ImageButton) convertView
                    .findViewById(R.id.butSecondaryAction);
            holder.imageView = (ImageView) convertView.findViewById(R.id.imgvImage);
            holder.txtvSize = (TextView) convertView.findViewById(R.id.txtvSize);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.title.setText(item.getTitle());
        holder.pubDate.setText(DateUtils.formatDateTime(context, item.getPubDate().getTime(), DateUtils.FORMAT_ABBREV_ALL));
        holder.txtvSize.setText(Converter.byteToString(item.getMedia().getSize()));
        FeedItem.State state = item.getState();

        if (state == FeedItem.State.PLAYING) {
            holder.butSecondary.setEnabled(false);
        } else {
            holder.butSecondary.setEnabled(true);
        }

        holder.butSecondary.setFocusable(false);
        holder.butSecondary.setTag(item);
        holder.butSecondary.setOnClickListener(secondaryActionListener);


        Picasso.with(context)
                .load(item.getImageUri())
                .fit()
                .into(holder.imageView);

        return convertView;
    }

    private View.OnClickListener secondaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            FeedItem item = (FeedItem) v.getTag();
            itemAccess.onFeedItemSecondaryAction(item);
        }
    };


    static class Holder {
        TextView title;
        TextView pubDate;
        ImageView imageView;
        TextView txtvSize;
        ImageButton butSecondary;
    }

    public interface ItemAccess {
        int getCount();

        FeedItem getItem(int position);

        void onFeedItemSecondaryAction(FeedItem item);
    }
}
