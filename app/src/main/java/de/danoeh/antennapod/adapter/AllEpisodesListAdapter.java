package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.content.res.TypedArray;
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
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.Converter;

/**
 * List adapter for the list of new episodes
 */
public class AllEpisodesListAdapter extends BaseAdapter {

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
            holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
            holder.pubDate = (TextView) convertView
                    .findViewById(R.id.txtvPublished);
            holder.statusUnread = convertView.findViewById(R.id.statusUnread);
            holder.butSecondary = (ImageButton) convertView
                    .findViewById(R.id.butSecondaryAction);
            holder.queueStatus = (ImageView) convertView
                    .findViewById(R.id.imgvInPlaylist);
            holder.type = (ImageView) convertView.findViewById(R.id.imgvType);
            holder.progress = (ProgressBar) convertView
                    .findViewById(R.id.pbar_progress);
            holder.imageView = (ImageView) convertView.findViewById(R.id.imgvImage);
            holder.txtvDuration = (TextView) convertView.findViewById(R.id.txtvDuration);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

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

            TypedArray typeDrawables = context.obtainStyledAttributes(
                    new int[]{R.attr.type_audio, R.attr.type_video});
            final int[] labels = new int[]{R.string.media_type_audio_label, R.string.media_type_video_label};
            MediaType mediaType = item.getMedia().getMediaType();
            if (mediaType == MediaType.AUDIO) {
                holder.type.setImageDrawable(typeDrawables.getDrawable(0));
                holder.type.setContentDescription(context.getString(labels[0]));
                holder.type.setVisibility(View.VISIBLE);
            } else if (mediaType == MediaType.VIDEO) {
                holder.type.setImageDrawable(typeDrawables.getDrawable(1));
                holder.type.setContentDescription(context.getString(labels[1]));
                holder.type.setVisibility(View.VISIBLE);
            } else {
                holder.type.setImageBitmap(null);
                holder.type.setVisibility(View.GONE);
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
        ImageView type;
        ImageView imageView;
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
