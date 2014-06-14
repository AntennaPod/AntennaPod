package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.ImageLoader;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.storage.DownloadRequester;

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
                    null);
            holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
            holder.butSecondary = (ImageButton) convertView
                    .findViewById(R.id.butSecondaryAction);
            holder.position = (TextView) convertView.findViewById(R.id.txtvPosition);
            holder.progress = (ProgressBar) convertView
                    .findViewById(R.id.pbar_download_progress);
            holder.imageView = (ImageView) convertView.findViewById(R.id.imgvImage);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.title.setText(item.getTitle());

        AdapterUtils.updateEpisodePlaybackProgress(item, context.getResources(), holder.position, holder.progress);

        FeedMedia media = item.getMedia();
        if (media != null) {
            final boolean isDownloadingMedia = DownloadRequester.getInstance().isDownloadingFile(media);

            if (!media.isDownloaded()) {
                if (isDownloadingMedia) {
                    // item is being downloaded
                    holder.progress.setVisibility(View.VISIBLE);
                    holder.progress.setProgress(itemAccess.getItemDownloadProgressPercent(item));
                }
            }
        }

        actionButtonUtils.configureActionButton(holder.butSecondary, item);
        holder.butSecondary.setFocusable(false);
        holder.butSecondary.setTag(item);
        holder.butSecondary.setOnClickListener(secondaryActionListener);


        ImageLoader.getInstance().loadThumbnailBitmap(
                item,
                holder.imageView,
                (int) convertView.getResources().getDimension(
                        R.dimen.thumbnail_length)
        );
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
        ImageView imageView;
        TextView position;
        ProgressBar progress;
        ImageButton butSecondary;
    }

    public interface ItemAccess {
        int getCount();

        FeedItem getItem(int position);

        int getItemDownloadProgressPercent(FeedItem item);
    }
}
