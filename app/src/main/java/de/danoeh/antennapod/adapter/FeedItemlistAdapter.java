package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.DateUtils;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.core.util.ThemeUtils;

/**
 * List adapter for items of feeds that the user has already subscribed to.
 */
public class FeedItemlistAdapter extends BaseAdapter {

    private final ActionButtonCallback callback;
    private final ItemAccess itemAccess;
    private final Context context;
    private final boolean showFeedtitle;
    private final int selectedItemIndex;
    /** true if played items should be made partially transparent */
    private final boolean makePlayedItemsTransparent;
    private final ActionButtonUtils actionButtonUtils;

    private static final int SELECTION_NONE = -1;

    private final int playingBackGroundColor;
    private final int normalBackGroundColor;

    public FeedItemlistAdapter(Context context,
                               ItemAccess itemAccess,
                               ActionButtonCallback callback,
                               boolean showFeedtitle,
                               boolean makePlayedItemsTransparent) {
        super();
        this.callback = callback;
        this.context = context;
        this.itemAccess = itemAccess;
        this.showFeedtitle = showFeedtitle;
        this.selectedItemIndex = SELECTION_NONE;
        this.actionButtonUtils = new ActionButtonUtils(context);
        this.makePlayedItemsTransparent = makePlayedItemsTransparent;

        if(UserPreferences.getTheme() == R.style.Theme_AntennaPod_Dark) {
            playingBackGroundColor = ContextCompat.getColor(context, R.color.highlight_dark);
        } else {
            playingBackGroundColor = ContextCompat.getColor(context, R.color.highlight_light);
        }
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
            holder.container = (LinearLayout) convertView
                    .findViewById(R.id.container);
            holder.title = (TextView) convertView.findViewById(R.id.txtvItemname);
            if(Build.VERSION.SDK_INT >= 23) {
                holder.title.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL);
            }
            holder.lenSize = (TextView) convertView
                    .findViewById(R.id.txtvLenSize);
            holder.butAction = (ImageButton) convertView
                    .findViewById(R.id.butSecondaryAction);
            holder.published = (TextView) convertView
                    .findViewById(R.id.txtvPublished);
            holder.inPlaylist = (ImageView) convertView
                    .findViewById(R.id.imgvInPlaylist);
            holder.type = (ImageView) convertView.findViewById(R.id.imgvType);
            holder.statusUnread = convertView
                    .findViewById(R.id.statusUnread);
            holder.episodeProgress = (ProgressBar) convertView
                    .findViewById(R.id.pbar_episode_progress);

            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        if (!(getItemViewType(position) == Adapter.IGNORE_ITEM_VIEW_TYPE)) {
            convertView.setVisibility(View.VISIBLE);
            if (position == selectedItemIndex) {
                convertView.setBackgroundColor(ContextCompat.getColor(convertView.getContext(),
                        ThemeUtils.getSelectionBackgroundColor()));
            } else {
                convertView.setBackgroundResource(0);
            }

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

                if(media.isCurrentlyPlaying()) {
                    holder.container.setBackgroundColor(playingBackGroundColor);
                } else {
                    holder.container.setBackgroundColor(normalBackGroundColor);
                }
            }

            actionButtonUtils.configureActionButton(holder.butAction, item, isInQueue);
            holder.butAction.setFocusable(false);
            holder.butAction.setTag(item);
            holder.butAction.setOnClickListener(butActionListener);

        } else {
            convertView.setVisibility(View.GONE);
        }
        return convertView;
    }

    private final OnClickListener butActionListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            FeedItem item = (FeedItem) v.getTag();
            callback.onActionButtonPressed(item, itemAccess.getQueueIds());
        }
    };

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
