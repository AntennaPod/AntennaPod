package de.danoeh.antennapod.adapter;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.joanzapata.iconify.Iconify;

import java.lang.ref.WeakReference;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.fragment.ItemFragment;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;

/**
 * List adapter for the queue.
 */
public class QueueRecyclerAdapter extends RecyclerView.Adapter<QueueRecyclerAdapter.ViewHolder> {

    private static final String TAG = QueueRecyclerAdapter.class.getSimpleName();

    private WeakReference<MainActivity> mainActivity;
    private final ItemAccess itemAccess;
    private final ActionButtonCallback actionButtonCallback;
    private final ActionButtonUtils actionButtonUtils;
    private final ItemTouchHelper itemTouchHelper;

    private boolean locked;

    private int position = -1;

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
            position = pos;
            return false;
        });
    }

    public int getItemCount() {
        return itemAccess.getCount();
    }

    public int getPosition() {
        return position;
    }

    public class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnCreateContextMenuListener {

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
            dragHandle = (ImageView) v.findViewById(R.id.drag_handle);
            placeholder = (TextView) v.findViewById(R.id.txtvPlaceholder);
            cover = (ImageView) v.findViewById(R.id.imgvCover);
            title = (TextView) v.findViewById(R.id.txtvTitle);
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
                activity.loadChildFragment(ItemFragment.newInstance(item.getId()));
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
            FeedItemMenuHandler.onPrepareMenu(mainActivity.get(), contextMenuInterface, item, true,
                    itemAccess.getQueueIds());
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
            String pubDateStr = DateUtils.formatDateTime(mainActivity.get(),
                item.getPubDate().getTime(), DateUtils.FORMAT_ABBREV_ALL);
            pubDate.setText(pubDateStr.replace(" ", "\n"));

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
                    } else if(false == media.checkedOnSizeButUnknown()) {
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
                    progressBar.setVisibility(View.GONE);
                }
            }

            actionButtonUtils.configureActionButton(butSecondary, item);
            butSecondary.setFocusable(false);
            butSecondary.setTag(item);
            butSecondary.setOnClickListener(secondaryActionListener);

            Glide.with(mainActivity.get())
                .load(item.getImageUri())
                .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                .fitCenter()
                .dontAnimate()
                .into(new CoverTarget(item.getFeed().getImageUri(), placeholder, cover));
        }
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
                Glide.with(mainActivity.get())
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


    public interface ItemAccess {
        FeedItem getItem(int position);
        int getCount();
        long getItemDownloadedBytes(FeedItem item);
        long getItemDownloadSize(FeedItem item);
        int getItemDownloadProgressPercent(FeedItem item);
        LongList getQueueIds();
    }
}
