package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import androidx.core.content.ContextCompat;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.actionbutton.ItemActionButton;
import de.danoeh.antennapod.core.event.PlaybackPositionEvent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.DateUtils;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.core.util.ThemeUtils;

/**
 * List adapter for items of feeds that the user has already subscribed to.
 */
public class FeedItemlistAdapter extends BaseAdapter {

    private final ItemAccess itemAccess;
    private final Context context;
    private final boolean showFeedtitle;
    /** true if played items should be made partially transparent */
    private final boolean makePlayedItemsTransparent;
    private final int playingBackGroundColor;
    private final int normalBackGroundColor;

    private int currentlyPlayingItem = -1;

    public FeedItemlistAdapter(Context context,
                               ItemAccess itemAccess,
                               boolean showFeedtitle,
                               boolean makePlayedItemsTransparent) {
        super();
        this.context = context;
        this.itemAccess = itemAccess;
        this.showFeedtitle = showFeedtitle;
        this.makePlayedItemsTransparent = makePlayedItemsTransparent;

        playingBackGroundColor = ThemeUtils.getColorFromAttr(context, R.attr.currently_playing_background);
        normalBackGroundColor = ContextCompat.getColor(context, android.R.color.transparent);
    }

    @Override
    public int getCount() {
        return itemAccess.getCount();

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public FeedItem getItem(int position) {
        return itemAccess.getItem(position);
    }

    @Override
    @SuppressWarnings("ResourceType")
    public View getView(final int position, View convertView, ViewGroup parent) {
        Holder holder;
        final FeedItem item = getItem(position);

        if (convertView == null) {
            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.feeditemlist_item, parent, false);
            holder.container = convertView
                    .findViewById(R.id.container);
            holder.title = convertView.findViewById(R.id.txtvItemname);
            if(Build.VERSION.SDK_INT >= 23) {
                holder.title.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL);
            }
            holder.lenSize = convertView
                    .findViewById(R.id.txtvLenSize);
            holder.butAction = convertView
                    .findViewById(R.id.butSecondaryAction);
            holder.published = convertView
                    .findViewById(R.id.txtvPublished);
            holder.inPlaylist = convertView
                    .findViewById(R.id.imgvInPlaylist);
            holder.type = convertView.findViewById(R.id.imgvType);
            holder.statusUnread = convertView
                    .findViewById(R.id.statusUnread);
            holder.episodeProgress = convertView
                    .findViewById(R.id.pbar_episode_progress);

            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        if (!(getItemViewType(position) == Adapter.IGNORE_ITEM_VIEW_TYPE)) {
            convertView.setVisibility(View.VISIBLE);

            StringBuilder buffer = new StringBuilder(item.getTitle());
            if (showFeedtitle) {
                buffer.append(" (");
                buffer.append(item.getFeed().getTitle());
                buffer.append(")");
            }
            holder.title.setText(buffer.toString());

            if(item.isNew()) {
                holder.statusUnread.setVisibility(View.VISIBLE);
            } else {
                holder.statusUnread.setVisibility(View.INVISIBLE);
            }
            if(item.isPlayed() && makePlayedItemsTransparent) {
                convertView.setAlpha(0.5f);
            } else {
                convertView.setAlpha(1.0f);
            }

            String pubDateStr = DateUtils.formatAbbrev(context, item.getPubDate());
            holder.published.setText(pubDateStr);

            boolean isInQueue = item.isTagged(FeedItem.TAG_QUEUE);

            FeedMedia media = item.getMedia();
            if (media == null) {
                holder.episodeProgress.setVisibility(View.INVISIBLE);
                holder.inPlaylist.setVisibility(View.INVISIBLE);
                holder.type.setVisibility(View.INVISIBLE);
                holder.lenSize.setVisibility(View.INVISIBLE);
            } else {

                AdapterUtils.updateEpisodePlaybackProgress(item, holder.lenSize, holder.episodeProgress);

                if (isInQueue) {
                    holder.inPlaylist.setVisibility(View.VISIBLE);
                } else {
                    holder.inPlaylist.setVisibility(View.INVISIBLE);
                }

                if (DownloadRequester.getInstance().isDownloadingFile(item.getMedia())) {
                    holder.episodeProgress.setVisibility(View.VISIBLE);
                    holder.episodeProgress.setProgress(itemAccess.getItemDownloadProgressPercent(item));
                } else {
                    if(media.getPosition() == 0) {
                        holder.episodeProgress.setVisibility(View.INVISIBLE);
                    }
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
                typeDrawables.recycle();

                if (media.isCurrentlyPlaying()) {
                    holder.container.setBackgroundColor(playingBackGroundColor);
                    currentlyPlayingItem = position;
                } else {
                    holder.container.setBackgroundColor(normalBackGroundColor);
                }
            }

            ItemActionButton actionButton = ItemActionButton.forItem(item, isInQueue, true);
            actionButton.configure(holder.butAction, context);

            holder.butAction.setFocusable(false);
            holder.butAction.setTag(item);

        } else {
            convertView.setVisibility(View.GONE);
        }
        return convertView;
    }

    public void notifyCurrentlyPlayingItemChanged(PlaybackPositionEvent event, ListView listView) {
        if (currentlyPlayingItem != -1 && currentlyPlayingItem < getCount()) {
            View view = listView.getChildAt(currentlyPlayingItem
                    - listView.getFirstVisiblePosition() + listView.getHeaderViewsCount());
            if (view == null) {
                return;
            }
            Holder holder = (Holder) view.getTag();
            holder.episodeProgress.setVisibility(View.VISIBLE);
            holder.episodeProgress.setProgress((int) (100.0 * event.getPosition() / event.getDuration()));
            holder.lenSize.setText(Converter.getDurationStringLong(event.getDuration() - event.getPosition()));
        }
    }

    static class Holder {
        LinearLayout container;
        TextView title;
        TextView published;
        TextView lenSize;
        ImageView type;
        ImageView inPlaylist;
        ImageButton butAction;
        View statusUnread;
        ProgressBar episodeProgress;
    }

    public interface ItemAccess {

        int getItemDownloadProgressPercent(FeedItem item);

        int getCount();

        FeedItem getItem(int position);

        LongList getQueueIds();

    }

}
