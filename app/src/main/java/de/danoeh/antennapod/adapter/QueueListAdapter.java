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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

import java.lang.ref.WeakReference;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
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

    private boolean locked;


    public QueueListAdapter(Context context, ItemAccess itemAccess, ActionButtonCallback actionButtonCallback) {
        super();
        this.context = context;
        this.itemAccess = itemAccess;
        this.actionButtonUtils = new ActionButtonUtils(context);
        this.actionButtonCallback = actionButtonCallback;
        locked = UserPreferences.isQueueLocked();
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        notifyDataSetChanged();
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
            holder.dragHandle = (ImageView) convertView.findViewById(R.id.drag_handle);
            holder.feed = (TextView) convertView.findViewById(R.id.txtvImage);
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

        if(locked) {
            holder.dragHandle.setVisibility(View.GONE);
        } else {
            holder.dragHandle.setVisibility(View.VISIBLE);
        }

        holder.feed.setText(item.getFeed().getTitle());

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
                if(media.getSize() > 0) {
                    holder.progressLeft.setText(Converter.byteToString(media.getSize()));
                } else {
                    holder.progressLeft.setText("");
                }
                holder.progressRight.setText(Converter.getDurationStringLong(media.getDuration()));
                holder.progress.setVisibility(View.GONE);
            }
        }

        actionButtonUtils.configureActionButton(holder.butSecondary, item);
        holder.butSecondary.setFocusable(false);
        holder.butSecondary.setTag(item);
        holder.butSecondary.setOnClickListener(secondaryActionListener);

        Glide.with(context)
                .load(item.getImageUri())
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .fitCenter()
                .dontAnimate()
                .into(new CustomTarget(holder.feed, holder.imageView));

        return convertView;
    }

    private class CustomTarget extends GlideDrawableImageViewTarget {

        private final WeakReference<TextView> mPlaceholder;

        public CustomTarget(TextView placeholder, ImageView imageView) {
            super(imageView);
            mPlaceholder = new WeakReference<TextView>(placeholder);
        }

        @Override
        public void onResourceReady(GlideDrawable drawable, GlideAnimation anim) {
            super.onResourceReady(drawable, anim);
            TextView txtvPlaceholder = mPlaceholder.get();
            if(txtvPlaceholder != null) {
                txtvPlaceholder.setVisibility(View.INVISIBLE);
            }
        }
    }

    private View.OnClickListener secondaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            FeedItem item = (FeedItem) v.getTag();
            actionButtonCallback.onActionButtonPressed(item);
        }
    };


    static class Holder {
        ImageView dragHandle;
        ImageView imageView;
        TextView feed;
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
