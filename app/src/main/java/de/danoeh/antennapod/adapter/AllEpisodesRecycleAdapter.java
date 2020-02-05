package de.danoeh.antennapod.adapter;

import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.text.Layout;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.joanzapata.iconify.Iconify;

import java.lang.ref.WeakReference;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.actionbutton.ItemActionButton;
import de.danoeh.antennapod.core.event.PlaybackPositionEvent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.DateUtils;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.core.util.ThemeUtils;
import de.danoeh.antennapod.fragment.ItemFragment;
import de.danoeh.antennapod.fragment.ItemPagerFragment;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;

/**
 * List adapter for the list of new episodes
 */
public class AllEpisodesRecycleAdapter extends RecyclerView.Adapter<AllEpisodesRecycleAdapter.Holder> {

    private static final String TAG = AllEpisodesRecycleAdapter.class.getSimpleName();

    private final WeakReference<MainActivity> mainActivityRef;
    private final ItemAccess itemAccess;
    private final boolean showOnlyNewEpisodes;

    private FeedItem selectedItem;

    private final int playingBackGroundColor;
    private final int normalBackGroundColor;

    public AllEpisodesRecycleAdapter(MainActivity mainActivity,
                                     ItemAccess itemAccess,
                                     boolean showOnlyNewEpisodes) {
        super();
        this.mainActivityRef = new WeakReference<>(mainActivity);
        this.itemAccess = itemAccess;
        this.showOnlyNewEpisodes = showOnlyNewEpisodes;

        playingBackGroundColor = ThemeUtils.getColorFromAttr(mainActivity, R.attr.currently_playing_background);
        normalBackGroundColor = ContextCompat.getColor(mainActivity, android.R.color.transparent);
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.new_episodes_listitem, parent, false);
        Holder holder = new Holder(view);
        holder.container = view.findViewById(R.id.container);
        holder.content = view.findViewById(R.id.content);
        holder.placeholder = view.findViewById(R.id.txtvPlaceholder);
        holder.title = view.findViewById(R.id.txtvTitle);
        if(Build.VERSION.SDK_INT >= 23) {
            holder.title.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL);
        }
        holder.pubDate = view
                .findViewById(R.id.txtvPublished);
        holder.statusUnread = view.findViewById(R.id.statusUnread);
        holder.butSecondary = view
                .findViewById(R.id.butSecondaryAction);
        holder.queueStatus = view
                .findViewById(R.id.imgvInPlaylist);
        holder.progress = view
                .findViewById(R.id.pbar_progress);
        holder.cover = view.findViewById(R.id.imgvCover);
        holder.txtvDuration = view.findViewById(R.id.txtvDuration);
        holder.item = null;
        holder.mainActivityRef = mainActivityRef;
        // so we can grab this later
        view.setTag(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(final Holder holder, int position) {
        final FeedItem item = itemAccess.getItem(position);
        if (item == null) return;
        holder.itemView.setOnLongClickListener(v -> {
            this.selectedItem = item;
            return false;
        });
        holder.item = item;
        holder.placeholder.setVisibility(View.VISIBLE);
        holder.placeholder.setText(item.getFeed().getTitle());
        holder.title.setText(item.getTitle());
        String pubDateStr = DateUtils.formatAbbrev(mainActivityRef.get(), item.getPubDate());
        holder.pubDate.setText(pubDateStr);
        if (showOnlyNewEpisodes || !item.isNew()) {
            holder.statusUnread.setVisibility(View.INVISIBLE);
        } else {
            holder.statusUnread.setVisibility(View.VISIBLE);
        }
        if(item.isPlayed()) {
            holder.content.setAlpha(0.5f);
        } else {
            holder.content.setAlpha(1.0f);
        }

        FeedMedia media = item.getMedia();
        if (media != null) {
            final boolean isDownloadingMedia = DownloadRequester.getInstance().isDownloadingFile(media);

            if (media.getDuration() > 0) {
                holder.txtvDuration.setText(Converter.getDurationStringLong(media.getDuration()));
            } else if (media.getSize() > 0) {
                holder.txtvDuration.setText(Converter.byteToString(media.getSize()));
            } else if(NetworkUtils.isEpisodeHeadDownloadAllowed() && !media.checkedOnSizeButUnknown()) {
                holder.txtvDuration.setText("{fa-spinner}");
                Iconify.addIcons(holder.txtvDuration);
                NetworkUtils.getFeedMediaSizeObservable(media)
                        .subscribe(
                                size -> {
                                    if (size > 0) {
                                        holder.txtvDuration.setText(Converter.byteToString(size));
                                    } else {
                                        holder.txtvDuration.setText("");
                                    }
                                }, error -> {
                                    holder.txtvDuration.setText("");
                                    Log.e(TAG, Log.getStackTraceString(error));
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
                holder.progress.setVisibility(View.INVISIBLE);
            }

            if (media.isCurrentlyPlaying()) {
                holder.container.setBackgroundColor(playingBackGroundColor);
            } else {
                holder.container.setBackgroundColor(normalBackGroundColor);
            }
        } else {
            holder.progress.setVisibility(View.INVISIBLE);
            holder.txtvDuration.setVisibility(View.GONE);
        }

        boolean isInQueue = itemAccess.isInQueue(item);
        if (isInQueue) {
            holder.queueStatus.setVisibility(View.VISIBLE);
        } else {
            holder.queueStatus.setVisibility(View.INVISIBLE);
        }

        ItemActionButton actionButton = ItemActionButton.forItem(item, isInQueue, true);
        actionButton.configure(holder.butSecondary, mainActivityRef.get());

        holder.butSecondary.setFocusable(false);
        holder.butSecondary.setTag(item);

        new CoverLoader(mainActivityRef.get())
                .withUri(ImageResourceUtils.getImageLocation(item))
                .withFallbackUri(item.getFeed().getImageLocation())
                .withPlaceholderView(holder.placeholder)
                .withCoverView(holder.cover)
                .load();
    }

    @Nullable
    public FeedItem getSelectedItem() {
        return selectedItem;
    }

    @Override
    public long getItemId(int position) {
        FeedItem item = itemAccess.getItem(position);
        return item != null ? item.getId() : RecyclerView.NO_POSITION;
    }

    @Override
    public int getItemCount() {
        return itemAccess.getCount();
    }

    public class Holder extends RecyclerView.ViewHolder
            implements View.OnClickListener,
                       View.OnCreateContextMenuListener,
                       ItemTouchHelperViewHolder {
        LinearLayout content;
        FrameLayout container;
        TextView placeholder;
        TextView title;
        TextView pubDate;
        View statusUnread;
        ImageView queueStatus;
        ImageView cover;
        ProgressBar progress;
        TextView txtvDuration;
        ImageButton butSecondary;
        FeedItem item;
        WeakReference<MainActivity> mainActivityRef;

        public Holder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onClick(View v) {
            MainActivity mainActivity = mainActivityRef.get();
            if (mainActivity != null) {
                LongList itemIds = itemAccess.getItemsIds();
                long[] ids = itemIds.toArray();
                mainActivity.loadChildFragment(ItemPagerFragment.newInstance(ids, itemIds.indexOf(item.getId())));
            }
        }

        @Override
        public void onItemSelected() {
            itemView.setAlpha(0.5f);
        }

        @Override
        public void onItemClear() {
            itemView.setAlpha(1.0f);
        }

        public FeedItem getFeedItem() { return item; }

        @Override
        public void onCreateContextMenu(final ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            FeedItem item = itemAccess.getItem(getAdapterPosition());

            MenuInflater inflater = mainActivityRef.get().getMenuInflater();
            inflater.inflate(R.menu.feeditemlist_context, menu);

            if (item != null) {
                menu.setHeaderTitle(item.getTitle());
            }
            FeedItemMenuHandler.onPrepareMenu(menu, item);
        }

        public boolean isCurrentlyPlayingItem() {
            return item.getMedia() != null && item.getMedia().isCurrentlyPlaying();
        }

        public void notifyPlaybackPositionUpdated(PlaybackPositionEvent event) {
            progress.setProgress((int) (100.0 * event.getPosition() / event.getDuration()));
        }

    }

    public interface ItemAccess {

        int getCount();

        FeedItem getItem(int position);

        LongList getItemsIds();

        int getItemDownloadProgressPercent(FeedItem item);

        boolean isInQueue(FeedItem item);

        LongList getQueueIds();

    }

    /**
     * Notifies a View Holder of relevant callbacks from
     * {@link ItemTouchHelper.Callback}.
     */
    public interface ItemTouchHelperViewHolder {

        /**
         * Called when the {@link ItemTouchHelper} first registers an
         * item as being moved or swiped.
         * Implementations should update the item view to indicate
         * it's active state.
         */
        void onItemSelected();


        /**
         * Called when the {@link ItemTouchHelper} has completed the
         * move or swipe, and the active item state should be cleared.
         */
        void onItemClear();
    }
}
