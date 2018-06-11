package de.danoeh.antennapod.adapter;

import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.joanzapata.iconify.Iconify;

import de.danoeh.antennapod.core.util.ThemeUtils;
import org.apache.commons.lang3.ArrayUtils;

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
 * List adapter for the queue.
 */
public class QueueRecyclerAdapter extends RecyclerView.Adapter<QueueRecyclerAdapter.ViewHolder> {

    private static final String TAG = QueueRecyclerAdapter.class.getSimpleName();

    private final WeakReference<MainActivity> mainActivity;
    private final ItemAccess itemAccess;
    private final ActionButtonCallback actionButtonCallback;
    private final ActionButtonUtils actionButtonUtils;
    private final ItemTouchHelper itemTouchHelper;

    private boolean locked;

    private FeedItem selectedItem;

    private final int playingBackGroundColor;
    private final int normalBackGroundColor;

    public QueueRecyclerAdapter(MainActivity mainActivity,
                                ItemAccess itemAccess,
                                ActionButtonCallback actionButtonCallback,
                                ItemTouchHelper itemTouchHelper) {
        super();
        this.mainActivity = new WeakReference<>(mainActivity);
        this.itemAccess = itemAccess;
        this.actionButtonUtils = new ActionButtonUtils(mainActivity);
        this.actionButtonCallback = actionButtonCallback;
        this.itemTouchHelper = itemTouchHelper;
        locked = UserPreferences.isQueueLocked();

        playingBackGroundColor = ThemeUtils.getColorFromAttr(mainActivity, R.attr.currently_playing_background);
        normalBackGroundColor = ContextCompat.getColor(mainActivity, android.R.color.transparent);
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        notifyDataSetChanged();
    }

    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.queue_listitem, parent, false);
        return new ViewHolder(view);
    }

    public void onBindViewHolder(ViewHolder holder, int pos) {
        FeedItem item = itemAccess.getItem(pos);
        holder.bind(item);
        holder.itemView.setOnLongClickListener(v -> {
            selectedItem = item;
            return false;
        });
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

    public int getItemCount() {
        return itemAccess.getCount();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener,
                       View.OnCreateContextMenuListener,
                       ItemTouchHelperViewHolder {

        private final FrameLayout container;
        private final ImageView dragHandle;
        private final TextView placeholder;
        private final ImageView cover;
        private final TextView title;
        private final TextView pubDate;
        private final TextView progressLeft;
        private final TextView progressRight;
        private final ProgressBar progressBar;
        private final ImageButton butSecondary;
        
        private FeedItem item;

        public ViewHolder(View v) {
            super(v);
            container = (FrameLayout) v.findViewById(R.id.container);
            dragHandle = (ImageView) v.findViewById(R.id.drag_handle);
            placeholder = (TextView) v.findViewById(R.id.txtvPlaceholder);
            cover = (ImageView) v.findViewById(R.id.imgvCover);
            title = (TextView) v.findViewById(R.id.txtvTitle);
            if(Build.VERSION.SDK_INT >= 23) {
                title.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL);
            }
            pubDate = (TextView) v.findViewById(R.id.txtvPubDate);
            progressLeft = (TextView) v.findViewById(R.id.txtvProgressLeft);
            progressRight = (TextView) v.findViewById(R.id.txtvProgressRight);
            butSecondary = (ImageButton) v.findViewById(R.id.butSecondaryAction);
            progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
            v.setTag(this);
            v.setOnClickListener(this);
            v.setOnCreateContextMenuListener(this);
            dragHandle.setOnTouchListener((v1, event) -> {
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                    Log.d(TAG, "startDrag()");
                    itemTouchHelper.startDrag(ViewHolder.this);
                }
                return false;
            });
        }

        @Override
        public void onClick(View v) {
            MainActivity activity = mainActivity.get();
            if (activity != null) {
                long[] ids = itemAccess.getQueueIds().toArray();
                int position = ArrayUtils.indexOf(ids, item.getId());
                activity.loadChildFragment(ItemFragment.newInstance(ids, position));
            }
        }

        @Override
        public void onCreateContextMenu(final ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            FeedItem item = itemAccess.getItem(getAdapterPosition());

            MenuInflater inflater = mainActivity.get().getMenuInflater();
            inflater.inflate(R.menu.queue_context, menu);

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
            FeedItemMenuHandler.onPrepareMenu(contextMenuInterface, item, true, itemAccess.getQueueIds());
        }

        @Override
        public void onItemSelected() {
            itemView.setAlpha(0.5f);
        }

        @Override
        public void onItemClear() {
            itemView.setAlpha(1.0f);
        }

        public void bind(FeedItem item) {
            this.item = item;
            if(locked) {
                dragHandle.setVisibility(View.GONE);
            } else {
                dragHandle.setVisibility(View.VISIBLE);
            }

            placeholder.setText(item.getFeed().getTitle());

            title.setText(item.getTitle());
            FeedMedia media = item.getMedia();

            title.setText(item.getTitle());
            String pubDateStr = DateUtils.formatAbbrev(mainActivity.get(), item.getPubDate());
            int index = 0;
            if(countMatches(pubDateStr, ' ') == 1 || countMatches(pubDateStr, ' ') == 2) {
                index = pubDateStr.lastIndexOf(' ');
            } else if(countMatches(pubDateStr, '.') == 2) {
                index = pubDateStr.lastIndexOf('.');
            } else if(countMatches(pubDateStr, '-') == 2) {
                index = pubDateStr.lastIndexOf('-');
            } else if(countMatches(pubDateStr, '/') == 2) {
                index = pubDateStr.lastIndexOf('/');
            }
            if(index > 0) {
                pubDateStr = pubDateStr.substring(0, index+1).trim() + "\n" + pubDateStr.substring(index+1);
            }
            pubDate.setText(pubDateStr);

            if (media != null) {
                final boolean isDownloadingMedia = DownloadRequester.getInstance().isDownloadingFile(media);
                FeedItem.State state = item.getState();
                if (isDownloadingMedia) {
                    progressLeft.setText(Converter.byteToString(itemAccess.getItemDownloadedBytes(item)));
                    if(itemAccess.getItemDownloadSize(item) > 0) {
                        progressRight.setText(Converter.byteToString(itemAccess.getItemDownloadSize(item)));
                    } else {
                        progressRight.setText(Converter.byteToString(media.getSize()));
                    }
                    progressBar.setProgress(itemAccess.getItemDownloadProgressPercent(item));
                    progressBar.setVisibility(View.VISIBLE);
                } else if (state == FeedItem.State.PLAYING
                    || state == FeedItem.State.IN_PROGRESS) {
                    if (media.getDuration() > 0) {
                        int progress = (int) (100.0 * media.getPosition() / media.getDuration());
                        progressBar.setProgress(progress);
                        progressBar.setVisibility(View.VISIBLE);
                        progressLeft.setText(Converter
                            .getDurationStringLong(media.getPosition()));
                        progressRight.setText(Converter.getDurationStringLong(media.getDuration()));
                    }
                } else {
                    if(media.getSize() > 0) {
                        progressLeft.setText(Converter.byteToString(media.getSize()));
                    } else if(NetworkUtils.isDownloadAllowed() && !media.checkedOnSizeButUnknown()) {
                        progressLeft.setText("{fa-spinner}");
                        Iconify.addIcons(progressLeft);
                        NetworkUtils.getFeedMediaSizeObservable(media)
                            .subscribe(
                                size -> {
                                    if (size > 0) {
                                        progressLeft.setText(Converter.byteToString(size));
                                    } else {
                                        progressLeft.setText("");
                                    }
                                }, error -> {
                                    progressLeft.setText("");
                                    Log.e(TAG, Log.getStackTraceString(error));
                                });
                    } else {
                        progressLeft.setText("");
                    }
                    progressRight.setText(Converter.getDurationStringLong(media.getDuration()));
                    progressBar.setVisibility(View.INVISIBLE);
                }

                if(media.isCurrentlyPlaying()) {
                    container.setBackgroundColor(playingBackGroundColor);
                } else {
                    container.setBackgroundColor(normalBackGroundColor);
                }
            }

            actionButtonUtils.configureActionButton(butSecondary, item, true);
            butSecondary.setFocusable(false);
            butSecondary.setTag(item);
            butSecondary.setOnClickListener(secondaryActionListener);

            Glide.with(mainActivity.get())
                .load(item.getImageLocation())
                .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                .fitCenter()
                .dontAnimate()
                .into(new CoverTarget(item.getFeed().getImageLocation(), placeholder, cover, mainActivity.get()));
        }

    }

    private final View.OnClickListener secondaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            FeedItem item = (FeedItem) v.getTag();
            actionButtonCallback.onActionButtonPressed(item, itemAccess.getQueueIds());
        }
    };


    public interface ItemAccess {
        FeedItem getItem(int position);
        int getCount();
        long getItemDownloadedBytes(FeedItem item);
        long getItemDownloadSize(FeedItem item);
        int getItemDownloadProgressPercent(FeedItem item);
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

    // Oh Xiaomi, I hate you so much. How did you manage to fuck this up?
    private static int countMatches(final CharSequence str, final char ch) {
        if (TextUtils.isEmpty(str)) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (ch == str.charAt(i)) {
                count++;
            }
        }
        return count;
    }
}
