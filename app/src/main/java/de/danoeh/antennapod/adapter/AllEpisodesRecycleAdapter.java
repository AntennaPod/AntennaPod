package de.danoeh.antennapod.adapter;

import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
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

import com.bumptech.glide.Glide;
import com.joanzapata.iconify.Iconify;

import java.lang.ref.WeakReference;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.DateUtils;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.fragment.ItemFragment;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;

/**
 * List adapter for the list of new episodes
 */
public class AllEpisodesRecycleAdapter extends RecyclerView.Adapter<AllEpisodesRecycleAdapter.Holder> {

    private static final String TAG = AllEpisodesRecycleAdapter.class.getSimpleName();

    private final WeakReference<MainActivity> mainActivityRef;
    private final ItemAccess itemAccess;
    private final ActionButtonCallback actionButtonCallback;
    private final ActionButtonUtils actionButtonUtils;
    private final boolean showOnlyNewEpisodes;

    private int position = -1;

    private final int playingBackGroundColor;
    private final int normalBackGroundColor;

    public AllEpisodesRecycleAdapter(MainActivity mainActivity,
                                     ItemAccess itemAccess,
                                     ActionButtonCallback actionButtonCallback,
                                     boolean showOnlyNewEpisodes) {
        super();
        this.mainActivityRef = new WeakReference<>(mainActivity);
        this.itemAccess = itemAccess;
        this.actionButtonUtils = new ActionButtonUtils(mainActivity);
        this.actionButtonCallback = actionButtonCallback;
        this.showOnlyNewEpisodes = showOnlyNewEpisodes;

        if(UserPreferences.getTheme() == R.style.Theme_AntennaPod_Dark) {
            playingBackGroundColor = ContextCompat.getColor(mainActivity, R.color.highlight_dark);
        } else {
            playingBackGroundColor = ContextCompat.getColor(mainActivity, R.color.highlight_light);
        }
        normalBackGroundColor = ContextCompat.getColor(mainActivity, android.R.color.transparent);
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.new_episodes_listitem, parent, false);
        Holder holder = new Holder(view);
        holder.container = (FrameLayout) view.findViewById(R.id.container);
        holder.content = (LinearLayout) view.findViewById(R.id.content);
        holder.placeholder = (TextView) view.findViewById(R.id.txtvPlaceholder);
        holder.title = (TextView) view.findViewById(R.id.txtvTitle);
        if(Build.VERSION.SDK_INT >= 23) {
            holder.title.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL);
        }
        holder.pubDate = (TextView) view
                .findViewById(R.id.txtvPublished);
        holder.statusUnread = view.findViewById(R.id.statusUnread);
        holder.butSecondary = (ImageButton) view
                .findViewById(R.id.butSecondaryAction);
        holder.queueStatus = (ImageView) view
                .findViewById(R.id.imgvInPlaylist);
        holder.progress = (ProgressBar) view
                .findViewById(R.id.pbar_progress);
        holder.cover = (ImageView) view.findViewById(R.id.imgvCover);
        holder.txtvDuration = (TextView) view.findViewById(R.id.txtvDuration);
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
            this.position = holder.getAdapterPosition();
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
            } else if(NetworkUtils.isDownloadAllowed() && !media.checkedOnSizeButUnknown()) {
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

            if(media.isCurrentlyPlaying()) {
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

        actionButtonUtils.configureActionButton(holder.butSecondary, item, isInQueue);
        holder.butSecondary.setFocusable(false);
        holder.butSecondary.setTag(item);
        holder.butSecondary.setOnClickListener(secondaryActionListener);

        Glide.with(mainActivityRef.get())
                .load(item.getImageLocation())
                .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                .fitCenter()
                .dontAnimate()
                .into(new CoverTarget(item.getFeed().getImageLocation(), holder.placeholder, holder.cover, mainActivityRef.get()));
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

    public FeedItem getItem(int position) {
        return itemAccess.getItem(position);
    }

    public int getPosition() {
        int pos = position;
        position = -1; // reset
        return pos;
    }

    private final View.OnClickListener secondaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            FeedItem item = (FeedItem) v.getTag();
            actionButtonCallback.onActionButtonPressed(item, itemAccess.getQueueIds());
        }
    };

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
                long[] ids = itemAccess.getItemsIds().toArray();
                mainActivity.loadChildFragment(ItemFragment.newInstance(ids, getAdapterPosition()));
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
            inflater.inflate(R.menu.allepisodes_context, menu);

            if (item != null) {
                menu.setHeaderTitle(item.getTitle());
            }

            FeedItemMenuHandler.MenuInterface contextMenuInterface = (id, visible) -> {
                if (menu == null) {
                    return;
                }
                MenuItem item1 = menu.findItem(id);
                if (item1 != null) {
                    item1.setVisible(visible);
                }
            };
            FeedItemMenuHandler.onPrepareMenu(contextMenuInterface, item, true, null);
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
