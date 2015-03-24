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
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.Converter;

/**
 * List adapter for the queue.
 */
public class QueueListAdapter extends BaseAdapter {


    private final Context context;
    private final ItemAccess itemAccess;
    private final ActionButtonCallback actionButtonCallback;
    private final ActionButtonUtils actionButtonUtils;


    public QueueListAdapter(Context context, ItemAccess itemAccess, ActionButtonCallback actionButtonCallback) {
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
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        final FeedItem item = (FeedItem) getItem(position);
        if (item == null) return null;

        if (convertView == null) {
            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.queue_listitem,
                    parent, false);
            holder.imageView = (ImageView) convertView.findViewById(R.id.imgvImage);
            holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
            holder.pubDate = (TextView) convertView.findViewById(R.id.txtvPubDate);
            holder.progressLeft = (TextView) convertView.findViewById(R.id.txtvProgressLeft);
            holder.progressRight = (TextView) convertView
                    .findViewById(R.id.txtvProgressRight);
            holder.butSecondary = (ImageButton) convertView
                    .findViewById(R.id.butSecondaryAction);
            holder.progress = (ProgressBar) convertView
                    .findViewById(R.id.progressBar);
            holder.imageView = (ImageView) convertView.findViewById(R.id.imgvImage);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.title.setText(item.getTitle());
        FeedMedia media = item.getMedia();


        holder.title.setText(item.getTitle());
        String pubDate = DateUtils.formatDateTime(context, item.getPubDate().getTime(), DateUtils.FORMAT_ABBREV_ALL);
        holder.pubDate.setText(pubDate.replace(" ", "\n"));

        if (media != null) {
            final boolean isDownloadingMedia = DownloadRequester.getInstance().isDownloadingFile(media);
            FeedItem.State state = item.getState();
            if (isDownloadingMedia) {
                holder.progressLeft.setText(Converter.byteToString(itemAccess.getItemDownloadedBytes(item)));
                if(itemAccess.getItemDownloadSize(item) > 0) {
                    holder.progressRight.setText(Converter.byteToString(itemAccess.getItemDownloadSize(item)));
                } else {
                    holder.progressRight.setText(Converter.byteToString(media.getSize()));
                }
                holder.progress.setProgress(itemAccess.getItemDownloadProgressPercent(item));
                holder.progress.setVisibility(View.VISIBLE);
            } else if (state == FeedItem.State.PLAYING
                    || state == FeedItem.State.IN_PROGRESS) {
                if (media.getDuration() > 0) {
                    int progress = (int) (100.0 * media.getPosition() / media.getDuration());
                    holder.progress.setProgress(progress);
                    holder.progress.setVisibility(View.VISIBLE);
                    holder.progressLeft.setText(Converter
                            .getDurationStringLong(media.getPosition()));
                    holder.progressRight.setText(Converter.getDurationStringLong(media.getDuration()));
                }
            } else {
                holder.progressLeft.setText(Converter.byteToString(media.getSize()));
                holder.progressRight.setText(Converter.getDurationStringLong(media.getDuration()));
                holder.progress.setVisibility(View.GONE);
            }
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
            EventDistributor.getInstance().sendQueueUpdateBroadcast();
        }
    };


    static class Holder {
        ImageView imageView;
        TextView title;
        TextView pubDate;
        TextView progressLeft;
        TextView progressRight;
        ProgressBar progress;
        ImageButton butSecondary;
    }

    public interface ItemAccess {
        FeedItem getItem(int position);
        int getCount();
        long getItemDownloadedBytes(FeedItem item);
        long getItemDownloadSize(FeedItem item);
        int getItemDownloadProgressPercent(FeedItem item);
    }
}
