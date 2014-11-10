package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.Converter;

/**
 * List adapter for the list of new episodes
 */
public class NewEpisodesListAdapter extends BaseAdapter {

    private final Context context;
    private final ItemAccess itemAccess;
    private final ActionButtonCallback actionButtonCallback;
    private final ActionButtonUtils actionButtonUtils;

    public NewEpisodesListAdapter(Context context, ItemAccess itemAccess, ActionButtonCallback actionButtonCallback) {
        super();
        this.context = context;
        this.itemAccess = itemAccess;
        this.actionButtonUtils = new ActionButtonUtils(context);
        this.actionButtonCallback = actionButtonCallback;
    }

    @Override
    public int getCount() {
        return itemAccess.getCount();
    }

    @Override
    public Object getItem(int position) {
        return itemAccess.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
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
            convertView = inflater.inflate(R.layout.new_episodes_listitem,
                    parent, false);
            holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
            holder.pubDate = (TextView) convertView
                    .findViewById(R.id.txtvPublished);
            holder.statusUnread = convertView.findViewById(R.id.statusUnread);
            holder.butSecondary = (ImageButton) convertView
                    .findViewById(R.id.butSecondaryAction);
            holder.queueStatus = (ImageView) convertView
                    .findViewById(R.id.imgvInPlaylist);
            holder.downloadProgress = (ProgressBar) convertView
                    .findViewById(R.id.pbar_download_progress);
            holder.imageView = (ImageView) convertView.findViewById(R.id.imgvImage);
            holder.txtvDuration = (TextView) convertView.findViewById(R.id.txtvDuration);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.title.setText(item.getTitle());
        holder.pubDate.setText(DateUtils.formatDateTime(context, item.getPubDate().getTime(), DateUtils.FORMAT_ABBREV_ALL));
        if (item.isRead()) {
            holder.statusUnread.setVisibility(View.INVISIBLE);
        } else {
            holder.statusUnread.setVisibility(View.VISIBLE);
        }

        FeedMedia media = item.getMedia();
        if (media != null) {
            final boolean isDownloadingMedia = DownloadRequester.getInstance().isDownloadingFile(media);

            if (media.getDuration() > 0) {
                holder.txtvDuration.setText(Converter.getDurationStringLong(media.getDuration()));
            } else if (media.getSize() > 0) {
                holder.txtvDuration.setText(Converter.byteToString(media.getSize()));
            } else {
                holder.txtvDuration.setText("");
            }

            if (isDownloadingMedia) {
                holder.downloadProgress.setVisibility(View.VISIBLE);
                holder.txtvDuration.setVisibility(View.GONE);
                holder.pubDate.setVisibility(View.GONE);
            } else {
                holder.txtvDuration.setVisibility(View.VISIBLE);
                holder.pubDate.setVisibility(View.VISIBLE);
                holder.downloadProgress.setVisibility(View.GONE);
            }

            if (!media.isDownloaded()) {
                if (isDownloadingMedia) {
                    // item is being downloaded
                    holder.downloadProgress.setProgress(itemAccess.getItemDownloadProgressPercent(item));
                }
            }
        }
        if (itemAccess.isInQueue(item)) {
            holder.queueStatus.setVisibility(View.VISIBLE);
        } else {
            holder.queueStatus.setVisibility(View.INVISIBLE);
        }

        actionButtonUtils.configureActionButton(holder.butSecondary, item);
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
            actionButtonCallback.onActionButtonPressed(item);
        }
    };


    static class Holder {
        TextView title;
        TextView pubDate;
        View statusUnread;
        ImageView queueStatus;
        ImageView imageView;
        ProgressBar downloadProgress;
        TextView txtvDuration;
        ImageButton butSecondary;
    }

    public interface ItemAccess {

        int getCount();

        FeedItem getItem(int position);

        int getItemDownloadProgressPercent(FeedItem item);

        boolean isInQueue(FeedItem item);
    }
}
