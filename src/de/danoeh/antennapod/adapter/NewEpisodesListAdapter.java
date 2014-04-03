package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.ImageLoader;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.Converter;

/**
 * List adapter for the list of new episodes
 */
public class NewEpisodesListAdapter extends BaseAdapter {

    private static final int VIEW_TYPE_FEEDITEM = 0;
    private static final int VIEW_TYPE_DIVIDER = 1;

    private final Context context;
    private final ItemAccess itemAccess;
    private final TypedArray drawables;
    private final int[] labels;

    public NewEpisodesListAdapter(Context context, ItemAccess itemAccess) {
        super();
        this.context = context;
        this.itemAccess = itemAccess;
        drawables = context.obtainStyledAttributes(new int[]{
                R.attr.av_play, R.attr.navigation_cancel, R.attr.av_download});
        labels = new int[]{R.string.play_label, R.string.cancel_download_label, R.string.download_label};
    }

    @Override
    public int getCount() {
        int unreadItems = itemAccess.getUnreadItemsCount();
        int recentItems = itemAccess.getRecentItemsCount();
        return unreadItems + recentItems + 1;
    }

    @Override
    public Object getItem(int position) {
        int unreadItems = itemAccess.getUnreadItemsCount();

        if (position == unreadItems) {
            return null;
        }
        if (position < unreadItems && unreadItems > 0) {
            return itemAccess.getUnreadItem(position);
        }
        return itemAccess.getRecentItem(position - unreadItems - 1);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        int unreadItems = itemAccess.getUnreadItemsCount();
        if (position == unreadItems) {
            return VIEW_TYPE_DIVIDER;
        } else {
            return VIEW_TYPE_FEEDITEM;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_FEEDITEM) {
            return getFeedItemView(position, convertView, parent);
        } else {
            return getDividerView(position, convertView, parent);
        }
    }

    public View getDividerView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.new_episodes_listdivider,
                null);
        convertView.setOnClickListener(null);
        convertView.setEnabled(false);
        return convertView;
    }

    public View getFeedItemView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        final FeedItem item = (FeedItem) getItem(position);
        if (item == null) return null;

        if (convertView == null) {
            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.new_episodes_listitem,
                    null);
            holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
            holder.pubDate = (TextView) convertView
                    .findViewById(R.id.txtvPublished);
            holder.butSecondary = (ImageButton) convertView
                    .findViewById(R.id.butSecondaryAction);
            holder.queueStatus = (ImageView) convertView
                    .findViewById(R.id.imgvInPlaylist);
            holder.statusPlaying = (ImageView) convertView
                    .findViewById(R.id.statusPlaying);
            holder.downloadProgress = (ProgressBar) convertView
                    .findViewById(R.id.pbar_download_progress);
            holder.imageView = (ImageView) convertView.findViewById(R.id.imgvImage);
            holder.txtvDuration = (TextView) convertView.findViewById(R.id.txtvDuration);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.title.setText(item.getTitle());
        holder.pubDate.setText(DateUtils.formatDateTime(context, item.getPubDate().getTime(), DateUtils.FORMAT_SHOW_DATE));
        FeedItem.State state = item.getState();

        if (state == FeedItem.State.PLAYING) {
            holder.statusPlaying.setVisibility(View.VISIBLE);
        } else {
            holder.statusPlaying.setVisibility(View.INVISIBLE);
        }

        FeedMedia media = item.getMedia();
        if (media != null) {
            final boolean isDownloadingMedia = DownloadRequester.getInstance().isDownloadingFile(media);

            if (media.getDuration() > 0) {
                holder.txtvDuration.setText(Converter.getDurationStringLong(media.getDuration()));
            } else {
                holder.txtvDuration.setText("");
            }

            if (isDownloadingMedia) {
                holder.downloadProgress.setVisibility(View.VISIBLE);
                holder.txtvDuration.setVisibility(View.GONE);
            } else {
                holder.txtvDuration.setVisibility(View.VISIBLE);
                holder.downloadProgress.setVisibility(View.GONE);
            }

            if (!media.isDownloaded()) {
                if (isDownloadingMedia) {
                    // item is being downloaded
                    holder.butSecondary.setVisibility(View.VISIBLE);
                    holder.butSecondary.setImageDrawable(drawables
                            .getDrawable(1));
                    holder.butSecondary.setContentDescription(context.getString(labels[1]));

                    holder.downloadProgress.setProgress(itemAccess.getItemDownloadProgressPercent(item));
                } else {
                    // item is not downloaded and not being downloaded
                    holder.butSecondary.setVisibility(View.VISIBLE);
                    holder.butSecondary.setImageDrawable(drawables.getDrawable(2));
                    holder.butSecondary.setContentDescription(context.getString(labels[2]));
                }
            } else {
                // item is not being downloaded
                holder.butSecondary.setVisibility(View.VISIBLE);
                holder.butSecondary
                        .setImageDrawable(drawables.getDrawable(0));
                holder.butSecondary.setContentDescription(context.getString(labels[0]));
            }
        } else {
            holder.butSecondary.setVisibility(View.INVISIBLE);
        }

        if (itemAccess.isInQueue(item)) {
            holder.queueStatus.setVisibility(View.VISIBLE);
        } else {
            holder.queueStatus.setVisibility(View.INVISIBLE);
        }

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
            itemAccess.onFeedItemSecondaryAction(item);
        }
    };


    static class Holder {
        TextView title;
        TextView pubDate;
        ImageView queueStatus;
        ImageView imageView;
        ImageView statusPlaying;
        ProgressBar downloadProgress;
        TextView txtvDuration;
        ImageButton butSecondary;
    }

    public interface ItemAccess {
        int getUnreadItemsCount();

        int getRecentItemsCount();

        FeedItem getUnreadItem(int position);

        FeedItem getRecentItem(int position);

        int getItemDownloadProgressPercent(FeedItem item);

        boolean isInQueue(FeedItem item);

        void onFeedItemSecondaryAction(FeedItem item);
    }
}
