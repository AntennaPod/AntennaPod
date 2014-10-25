package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.ThemeUtils;

/**
 * List adapter for items of feeds that the user has already subscribed to.
 */
public class FeedItemlistAdapter extends BaseAdapter {

    private ActionButtonCallback callback;
    private final ItemAccess itemAccess;
    private final Context context;
    private boolean showFeedtitle;
    private int selectedItemIndex;
    private final ActionButtonUtils actionButtonUtils;

    public static final int SELECTION_NONE = -1;

    public FeedItemlistAdapter(Context context,
                               ItemAccess itemAccess,
                               ActionButtonCallback callback, boolean showFeedtitle) {
        super();
        this.callback = callback;
        this.context = context;
        this.itemAccess = itemAccess;
        this.showFeedtitle = showFeedtitle;
        this.selectedItemIndex = SELECTION_NONE;
        this.actionButtonUtils = new ActionButtonUtils(context);
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        Holder holder;
        final FeedItem item = getItem(position);

        if (convertView == null) {
            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.feeditemlist_item, parent, false);
            holder.title = (TextView) convertView
                    .findViewById(R.id.txtvItemname);
            holder.lenSize = (TextView) convertView
                    .findViewById(R.id.txtvLenSize);
            holder.butAction = (ImageButton) convertView
                    .findViewById(R.id.butSecondaryAction);
            holder.published = (TextView) convertView
                    .findViewById(R.id.txtvPublished);
            holder.inPlaylist = (ImageView) convertView
                    .findViewById(R.id.imgvInPlaylist);
            holder.type = (ImageView) convertView.findViewById(R.id.imgvType);
            holder.statusUnread = (View) convertView
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
                convertView.setBackgroundColor(convertView.getResources()
                        .getColor(ThemeUtils.getSelectionBackgroundColor()));
            } else {
                convertView.setBackgroundResource(0);
            }

            StringBuilder buffer = new StringBuilder(item.getTitle());
            if (showFeedtitle) {
                buffer.append("(");
                buffer.append(item.getFeed().getTitle());
                buffer.append(")");
            }
            holder.title.setText(buffer.toString());

            FeedItem.State state = item.getState();
            switch (state) {
                case PLAYING:
                    holder.statusUnread.setVisibility(View.INVISIBLE);
                    holder.episodeProgress.setVisibility(View.VISIBLE);
                    break;
                case IN_PROGRESS:
                    holder.statusUnread.setVisibility(View.INVISIBLE);
                    holder.episodeProgress.setVisibility(View.VISIBLE);
                    break;
                case NEW:
                    holder.statusUnread.setVisibility(View.VISIBLE);
                    break;
                default:
                    holder.statusUnread.setVisibility(View.INVISIBLE);
                    break;
            }

            holder.published.setText(DateUtils.formatDateTime(context, item.getPubDate().getTime(), DateUtils.FORMAT_ABBREV_ALL));


            FeedMedia media = item.getMedia();
            if (media == null) {
                holder.episodeProgress.setVisibility(View.GONE);
                holder.inPlaylist.setVisibility(View.INVISIBLE);
                holder.type.setVisibility(View.INVISIBLE);
                holder.lenSize.setVisibility(View.INVISIBLE);
            } else {

                AdapterUtils.updateEpisodePlaybackProgress(item, context.getResources(), holder.lenSize, holder.episodeProgress);

                if (((ItemAccess) itemAccess).isInQueue(item)) {
                    holder.inPlaylist.setVisibility(View.VISIBLE);
                } else {
                    holder.inPlaylist.setVisibility(View.INVISIBLE);
                }

                if (DownloadRequester.getInstance().isDownloadingFile(
                        item.getMedia())) {
                    holder.episodeProgress.setVisibility(View.VISIBLE);
                    holder.episodeProgress.setProgress(((ItemAccess) itemAccess).getItemDownloadProgressPercent(item));
                    holder.published.setVisibility(View.GONE);
                } else {
                    holder.episodeProgress.setVisibility(View.GONE);
                    holder.published.setVisibility(View.VISIBLE);
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
            }

            actionButtonUtils.configureActionButton(holder.butAction, item);
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
            callback.onActionButtonPressed(item);
        }
    };

    static class Holder {
        TextView title;
        TextView published;
        TextView lenSize;
        ImageView type;
        ImageView inPlaylist;
        ImageButton butAction;
        View statusUnread;
        ProgressBar episodeProgress;
    }

    public int getSelectedItemIndex() {
        return selectedItemIndex;
    }

    public void setSelectedItemIndex(int selectedItemIndex) {
        this.selectedItemIndex = selectedItemIndex;
        notifyDataSetChanged();
    }

    public static interface ItemAccess {
        public boolean isInQueue(FeedItem item);

        int getItemDownloadProgressPercent(FeedItem item);

        int getCount();

        FeedItem getItem(int position);
    }

}
