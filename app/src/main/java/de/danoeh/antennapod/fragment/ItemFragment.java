package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.actionbutton.ItemActionButton;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.DownloaderUpdate;
import de.danoeh.antennapod.core.event.FeedItemEvent;
import de.danoeh.antennapod.core.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.DateUtils;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.core.util.playback.Timeline;
import de.danoeh.antennapod.view.ShownotesWebView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.ArrayUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

/**
 * Displays information about a FeedItem and actions.
 */
public class ItemFragment extends Fragment {

    private static final String TAG = "ItemFragment";
    private static final String ARG_FEEDITEM = "feeditem";

    /**
     * Creates a new instance of an ItemFragment
     *
     * @param feeditem The ID of the FeedItem to show
     * @return The ItemFragment instance
     */
    public static ItemFragment newInstance(long feeditem) {
        ItemFragment fragment = new ItemFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_FEEDITEM, feeditem);
        fragment.setArguments(args);
        return fragment;
    }

    private boolean itemsLoaded = false;
    private long itemId;
    private FeedItem item;
    private String webviewData;
    private List<Downloader> downloaderList;

    private ViewGroup root;
    private ShownotesWebView webvDescription;
    private TextView txtvPodcast;
    private TextView txtvTitle;
    private TextView txtvDuration;
    private TextView txtvPublished;
    private ImageView imgvCover;
    private ProgressBar progbarDownload;
    private ProgressBar progbarLoading;
    private TextView butAction1Text;
    private TextView butAction2Text;
    private ImageView butAction1Icon;
    private ImageView butAction2Icon;
    private View butAction1;
    private View butAction2;

    private Disposable disposable;
    private PlaybackController controller;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        itemId = getArguments().getLong(ARG_FEEDITEM);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View layout = inflater.inflate(R.layout.feeditem_fragment, container, false);

        root = layout.findViewById(R.id.content_root);

        txtvPodcast = layout.findViewById(R.id.txtvPodcast);
        txtvPodcast.setOnClickListener(v -> openPodcast());
        txtvTitle = layout.findViewById(R.id.txtvTitle);
        if (Build.VERSION.SDK_INT >= 23) {
            txtvTitle.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL);
        }
        txtvDuration = layout.findViewById(R.id.txtvDuration);
        txtvPublished = layout.findViewById(R.id.txtvPublished);
        if (Build.VERSION.SDK_INT >= 14) { // ellipsize is causing problems on old versions, see #448
            txtvTitle.setEllipsize(TextUtils.TruncateAt.END);
        }
        webvDescription = layout.findViewById(R.id.webvDescription);
        webvDescription.setTimecodeSelectedListener(time -> {
            if (controller != null && item.getMedia().getIdentifier().equals(controller.getMedia().getIdentifier())) {
                controller.seekTo(time);
            } else {
                Snackbar.make(getView(), R.string.play_this_to_seek_position, Snackbar.LENGTH_LONG).show();
            }
        });
        registerForContextMenu(webvDescription);

        imgvCover = layout.findViewById(R.id.imgvCover);
        imgvCover.setOnClickListener(v -> openPodcast());
        progbarDownload = layout.findViewById(R.id.progbarDownload);
        progbarLoading = layout.findViewById(R.id.progbarLoading);
        butAction1 = layout.findViewById(R.id.butAction1);
        butAction2 = layout.findViewById(R.id.butAction2);
        butAction1Icon = layout.findViewById(R.id.butAction1Icon);
        butAction2Icon = layout.findViewById(R.id.butAction2Icon);
        butAction1Text = layout.findViewById(R.id.butAction1Text);
        butAction2Text = layout.findViewById(R.id.butAction2Text);

        butAction1.setOnClickListener(v -> {
            if (item == null) {
                return;
            }
            ItemActionButton actionButton = ItemActionButton.forItem(item, item.isTagged(FeedItem.TAG_QUEUE), false);
            actionButton.onClick(getActivity());

            FeedMedia media = item.getMedia();
            if (media != null && media.isDownloaded()) {
                // playback was started, dialog should close itself
                ((MainActivity) getActivity()).dismissChildFragment();
            }
        });

        butAction2.setOnClickListener(v -> {
            if (item == null) {
                return;
            }

            if (item.hasMedia()) {
                FeedMedia media = item.getMedia();
                if (!media.isDownloaded()) {
                    DBTasks.playMedia(getActivity(), media, true, true, true);
                    ((MainActivity) getActivity()).dismissChildFragment();
                } else {
                    DBWriter.deleteFeedMediaOfItem(getActivity(), media.getId());
                }
            } else if (item.getLink() != null) {
                Uri uri = Uri.parse(item.getLink());
                getActivity().startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        });

        return layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        load();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        controller = new PlaybackController(getActivity(), false);
        controller.init();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (itemsLoaded) {
            progbarLoading.setVisibility(View.GONE);
            updateAppearance();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        controller.release();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (disposable != null) {
            disposable.dispose();
        }
        if (webvDescription != null && root != null) {
            root.removeView(webvDescription);
            webvDescription.destroy();
        }
    }

    private void onFragmentLoaded() {
        if (webviewData != null) {
            webvDescription.loadDataWithBaseURL("https://127.0.0.1", webviewData, "text/html", "utf-8", "about:blank");
        }
        updateAppearance();
    }

    private void updateAppearance() {
        if (item == null) {
            Log.d(TAG, "updateAppearance item is null");
            return;
        }
        txtvPodcast.setText(item.getFeed().getTitle());
        txtvTitle.setText(item.getTitle());

        if (item.getPubDate() != null) {
            String pubDateStr = DateUtils.formatAbbrev(getActivity(), item.getPubDate());
            txtvPublished.setText(pubDateStr);
        }

        Glide.with(getActivity())
                .load(ImageResourceUtils.getImageLocation(item))
                .apply(new RequestOptions()
                    .placeholder(R.color.light_gray)
                    .error(R.color.light_gray)
                    .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                    .fitCenter()
                    .dontAnimate())
                .into(imgvCover);

        progbarDownload.setVisibility(View.GONE);
        if (item.hasMedia() && downloaderList != null) {
            for (Downloader downloader : downloaderList) {
                if (downloader.getDownloadRequest().getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA
                        && downloader.getDownloadRequest().getFeedfileId() == item.getMedia().getId()) {
                    progbarDownload.setVisibility(View.VISIBLE);
                    progbarDownload.setProgress(downloader.getDownloadRequest().getProgressPercent());
                }
            }
        }

        FeedMedia media = item.getMedia();
        @AttrRes int butAction1IconRes = 0;
        @StringRes int butAction1TextRes = 0;
        @AttrRes int butAction2IconRes = 0;
        @StringRes int butAction2TextRes = 0;
        if (media == null) {
            if (!item.isPlayed()) {
                butAction1IconRes = R.attr.navigation_accept;
                if (item.hasMedia()) {
                    butAction1TextRes = R.string.mark_read_label;
                } else {
                    butAction1TextRes = R.string.mark_read_no_media_label;
                }
            }
            if (item.getLink() != null) {
                butAction2IconRes = R.attr.location_web_site;
                butAction2TextRes = R.string.visit_website_label;
            }
        } else {
            if (media.getDuration() > 0) {
                txtvDuration.setText(Converter.getDurationStringLong(media.getDuration()));
            }
            boolean isDownloading = DownloadRequester.getInstance().isDownloadingFile(media);
            if (!media.isDownloaded()) {
                butAction2IconRes = R.attr.action_stream;
                butAction2TextRes = R.string.stream_label;
            } else {
                butAction2IconRes = R.attr.content_discard;
                butAction2TextRes = R.string.delete_label;
            }
            if (isDownloading) {
                butAction1IconRes = R.attr.navigation_cancel;
                butAction1TextRes = R.string.cancel_label;
            } else if (media.isDownloaded()) {
                butAction1IconRes = R.attr.av_play;
                butAction1TextRes = R.string.play_label;
            } else {
                butAction1IconRes = R.attr.av_download;
                butAction1TextRes = R.string.download_label;
            }
        }

        if (butAction1IconRes != 0 && butAction1TextRes != 0) {
            butAction1Text.setText(butAction1TextRes);
            butAction1Text.setTransformationMethod(null);
            TypedValue typedValue = new TypedValue();
            getContext().getTheme().resolveAttribute(butAction1IconRes, typedValue, true);
            butAction1Icon.setImageResource(typedValue.resourceId);
            butAction1.setVisibility(View.VISIBLE);
        } else {
            butAction1.setVisibility(View.INVISIBLE);
        }
        if (butAction2IconRes != 0 && butAction2TextRes != 0) {
            butAction2Text.setText(butAction2TextRes);
            butAction2Text.setTransformationMethod(null);
            TypedValue typedValue = new TypedValue();
            getContext().getTheme().resolveAttribute(butAction2IconRes, typedValue, true);
            butAction2Icon.setImageResource(typedValue.resourceId);
            butAction2.setVisibility(View.VISIBLE);
        } else {
            butAction2.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return webvDescription.onContextItemSelected(item);
    }

    private void openPodcast() {
        Fragment fragment = FeedItemlistFragment.newInstance(item.getFeedId());
        ((MainActivity) getActivity()).loadChildFragment(fragment);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        for (FeedItem item : event.items) {
            if (this.item.getId() == item.getId()) {
                load();
                return;
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        DownloaderUpdate update = event.update;
        downloaderList = update.downloaders;
        if (item == null || item.getMedia() == null) {
            return;
        }
        long mediaId = item.getMedia().getId();
        if (ArrayUtils.contains(update.mediaIds, mediaId)) {
            if (itemsLoaded && getActivity() != null) {
                updateAppearance();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        load();
    }

    private void load() {
        if (disposable != null) {
            disposable.dispose();
        }
        progbarLoading.setVisibility(View.VISIBLE);
        disposable = Observable.fromCallable(this::loadInBackground)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(result -> {
                progbarLoading.setVisibility(View.GONE);
                item = result;
                itemsLoaded = true;
                onFragmentLoaded();
            }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @Nullable
    private FeedItem loadInBackground() {
        FeedItem feedItem = DBReader.getFeedItem(itemId);
        Context context = getContext();
        if (feedItem != null && context != null) {
            Timeline t = new Timeline(context, feedItem);
            webviewData = t.processShownotes();
        }
        return feedItem;
    }

}
