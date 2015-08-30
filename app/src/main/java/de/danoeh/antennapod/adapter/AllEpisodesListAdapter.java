package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.joanzapata.android.iconify.Iconify;

import java.lang.ref.WeakReference;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.NetworkUtils;

/**
 * List adapter for the list of new episodes
 */
public class AllEpisodesListAdapter extends BaseAdapter {

    private static final String TAG = AllEpisodesListAdapter.class.getSimpleName();

    private final Context context;
    private final ItemAccess itemAccess;
    private final ActionButtonCallback actionButtonCallback;
    private final ActionButtonUtils actionButtonUtils;
    private final boolean showOnlyNewEpisodes;

    public AllEpisodesListAdapter(Context context, ItemAccess itemAccess, ActionButtonCallback actionButtonCallback,
                                  boolean showOnlyNewEpisodes) {
        super();
        this.context = context;
        this.itemAccess = itemAccess;
        this.actionButtonUtils = new ActionButtonUtils(context);
        this.actionButtonCallback = actionButtonCallback;
        this.showOnlyNewEpisodes = showOnlyNewEpisodes;
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
            holder.placeholder = (TextView) convertView.findViewById(R.id.txtvPlaceholder);
            holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
            holder.pubDate = (TextView) convertView
                    .findViewById(R.id.txtvPublished);
            holder.statusUnread = convertView.findViewById(R.id.statusUnread);
            holder.butSecondary = (ImageButton) convertView
                    .findViewById(R.id.butSecondaryAction);
            holder.queueStatus = (ImageView) convertView
                    .findViewById(R.id.imgvInPlaylist);
            holder.progress = (ProgressBar) convertView
                    .findViewById(R.id.pbar_progress);
            holder.cover = (ImageView) convertView.findViewById(R.id.imgvCover);
            holder.txtvDuration = (TextView) convertView.findViewById(R.id.txtvDuration);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.placeholder.setVisibility(View.VISIBLE);
        holder.placeholder.setText(item.getFeed().getTitle());
        holder.title.setText(item.getTitle());
        holder.pubDate.setText(DateUtils.formatDateTime(context, item.getPubDate().getTime(), DateUtils.FORMAT_ABBREV_ALL));
        if (showOnlyNewEpisodes || false == item.isNew()) {
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
            } else if(false == media.checkedOnSizeButUnknown()) {
                holder.txtvDuration.setText("{fa-spinner}");
                Iconify.addIcons(holder.txtvDuration);
                NetworkUtils.getFeedMediaSizeObservable(media)
                        .subscribe(size -> {
                                    if (size > 0) {
                                        holder.txtvDuration.setText(Converter.byteToString(size));
                                    } else {
                                        holder.txtvDuration.setText("");
                                    }
                        });
            } else {
                holder.txtvDuration.setText("");
            }

            FeedItem.State state = item.getState();
            if (isDownloadingMedia) {
                holder.progress.setVisibility(View.VISIBLE);
                // item is being downloaded
                holder.progress.setProgress(itemAccess.getItemDownloadProgressPercent(item));
            } else if (state == FeedItem.State.PLAYING
                || state == FeedItem.State.IN_PROGRESS) {
                if (media.getDuration() > 0) {
                    int progress = (int) (100.0 * media.getPosition() / media.getDuration());
                    holder.progress.setProgress(progress);
                    holder.progress.setVisibility(View.VISIBLE);
                }
            } else {
                holder.progress.setVisibility(View.GONE);
            }

        } else {
            holder.progress.setVisibility(View.GONE);
            holder.txtvDuration.setVisibility(View.GONE);
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

        Glide.with(context)
                .load(item.getImageUri())
                .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                .fitCenter()
                .dontAnimate()
                .into(new CoverTarget(item.getFeed().getImageUri(), holder.placeholder, holder.cover));

        return convertView;
    }

    private class CoverTarget extends GlideDrawableImageViewTarget {

        private final WeakReference<Uri> fallback;
        private final WeakReference<TextView> placeholder;
        private final WeakReference<ImageView> cover;

        public CoverTarget(Uri fallbackUri, TextView txtvPlaceholder, ImageView imgvCover) {
            super(imgvCover);
            fallback = new WeakReference<>(fallbackUri);
            placeholder = new WeakReference<>(txtvPlaceholder);
            cover = new WeakReference<>(imgvCover);
        }

        @Override
        public void onLoadFailed(Exception e, Drawable errorDrawable) {
            Uri fallbackUri = fallback.get();
            TextView txtvPlaceholder = placeholder.get();
            ImageView imgvCover = cover.get();
            if(fallbackUri != null && txtvPlaceholder != null && imgvCover != null) {
                Glide.with(context)
                        .load(fallbackUri)
                        .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                        .fitCenter()
                        .dontAnimate()
                        .into(new CoverTarget(null, txtvPlaceholder, imgvCover));
            }
        }

        @Override
        public void onResourceReady(GlideDrawable drawable, GlideAnimation anim) {
            super.onResourceReady(drawable, anim);
            TextView txtvPlaceholder = placeholder.get();
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
        TextView placeholder;
        TextView title;
        TextView pubDate;
        View statusUnread;
        ImageView queueStatus;
        ImageView cover;
        ProgressBar progress;
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
