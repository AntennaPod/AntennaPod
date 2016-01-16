package de.danoeh.antennapod.fragment;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.widget.IconButton;

import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.DefaultActionButtonCallback;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.DownloaderUpdate;
import de.danoeh.antennapod.core.event.FeedItemEvent;
import de.danoeh.antennapod.core.event.QueueEvent;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.DateUtils;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.core.util.ShareUtils;
import de.danoeh.antennapod.core.util.playback.Timeline;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Displays information about a FeedItem and actions.
 */
public class ItemFragment extends Fragment {

    private static final String TAG = "ItemFragment";

    private static final int EVENTS = EventDistributor.UNREAD_ITEMS_UPDATE;

    private static final String ARG_FEEDITEM = "feeditem";

    /**
     * Creates a new instance of an ItemFragment
     *
     * @param feeditem The ID of the FeedItem that should be displayed.
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
    private long itemID;
    private FeedItem item;
    private LongList queue;
    private String webviewData;
    private List<Downloader> downloaderList;

    private ViewGroup root;
    private WebView webvDescription;
    private TextView txtvTitle;
    private TextView txtvDuration;
    private TextView txtvPublished;
    private ImageView imgvCover;
    private ProgressBar progbarDownload;
    private ProgressBar progbarLoading;
    private IconButton butAction1;
    private IconButton butAction2;
    private Menu popupMenu;

    private Subscription subscription;

    /**
     * URL that was selected via long-press.
     */
    private String selectedURL;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        itemID = getArguments().getLong(ARG_FEEDITEM, -1);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View layout = inflater.inflate(R.layout.feeditem_fragment, container, false);

        root = (ViewGroup) layout.findViewById(R.id.content_root);
        txtvTitle = (TextView) layout.findViewById(R.id.txtvTitle);
        txtvDuration = (TextView) layout.findViewById(R.id.txtvDuration);
        txtvPublished = (TextView) layout.findViewById(R.id.txtvPublished);
        if (Build.VERSION.SDK_INT >= 14) { // ellipsize is causing problems on old versions, see #448
            txtvTitle.setEllipsize(TextUtils.TruncateAt.END);
        }
        webvDescription = (WebView) layout.findViewById(R.id.webvDescription);
        if (UserPreferences.getTheme() == R.style.Theme_AntennaPod_Dark) {
            if (Build.VERSION.SDK_INT >= 11
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                webvDescription.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
            webvDescription.setBackgroundColor(getResources().getColor(
                R.color.black));
        }
        webvDescription.getSettings().setUseWideViewPort(false);
        webvDescription.getSettings().setLayoutAlgorithm(
            WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        webvDescription.getSettings().setLoadWithOverviewMode(true);
        webvDescription.setOnLongClickListener(webViewLongClickListener);
        webvDescription.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                if(IntentUtils.isCallable(getActivity(), intent)) {
                    startActivity(intent);
                }
                return true;
            }
        });
        registerForContextMenu(webvDescription);

        imgvCover = (ImageView) layout.findViewById(R.id.imgvCover);
        progbarDownload = (ProgressBar) layout.findViewById(R.id.progbarDownload);
        progbarLoading = (ProgressBar) layout.findViewById(R.id.progbarLoading);
        butAction1 = (IconButton) layout.findViewById(R.id.butAction1);
        butAction2 = (IconButton) layout.findViewById(R.id.butAction2);

        butAction1.setOnClickListener(v -> {
            if (item == null) {
                return;
            }
            DefaultActionButtonCallback actionButtonCallback = new DefaultActionButtonCallback(getActivity());
            actionButtonCallback.onActionButtonPressed(item);
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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        load();
    }

    @Override
    public void onResume() {
        super.onResume();
        EventDistributor.getInstance().register(contentUpdate);
        EventBus.getDefault().registerSticky(this);
        if(itemsLoaded) {
            updateAppearance();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        EventDistributor.getInstance().unregister(contentUpdate);
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(subscription != null) {
            subscription.unsubscribe();
        }
        if (webvDescription != null && root != null) {
            root.removeView(webvDescription);
            webvDescription.destroy();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(item == null) {
            return;
        }
        inflater.inflate(R.menu.feeditem_options, menu);
        popupMenu = menu;
        if (item.hasMedia()) {
            FeedItemMenuHandler.onPrepareMenu(getActivity(), popupMenuInterface, item, true, queue);
        } else {
            // these are already available via button1 and button2
            FeedItemMenuHandler.onPrepareMenu(getActivity(), popupMenuInterface, item, true, queue,
                    R.id.mark_read_item, R.id.visit_website_item);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        try {
            return FeedItemMenuHandler.onMenuItemClicked(getActivity(), menuItem.getItemId(), item);
        } catch (DownloadRequestException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
            return true;
        }
    }

    private final FeedItemMenuHandler.MenuInterface popupMenuInterface = new FeedItemMenuHandler.MenuInterface() {
        @Override
        public void setItemVisibility(int id, boolean visible) {
            MenuItem item = popupMenu.findItem(id);
            if (item != null) {
                item.setVisible(visible);
            }
        }
    };


    private void onFragmentLoaded() {
        progbarLoading.setVisibility(View.INVISIBLE);
        if (webviewData != null) {
            webvDescription.loadDataWithBaseURL(null, webviewData, "text/html",
                    "utf-8", "about:blank");
        }
        updateAppearance();
    }

    private void updateAppearance() {
        if (item == null) {
            Log.d(TAG, "updateAppearance item is null");
            return;
        }
        getActivity().supportInvalidateOptionsMenu();
        txtvTitle.setText(item.getTitle());

        if (item.getPubDate() != null) {
            String pubDateStr = DateUtils.formatAbbrev(getActivity(), item.getPubDate());
            txtvPublished.setText(pubDateStr);
        }

        Glide.with(getActivity())
                .load(item.getImageUri())
                .placeholder(R.color.light_gray)
                .error(R.color.light_gray)
                .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                .fitCenter()
                .dontAnimate()
                .into(imgvCover);

        progbarDownload.setVisibility(View.INVISIBLE);
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
        String butAction1Icon = null;
        int butAction1Text = 0;
        String butAction2Icon = null;
        int butAction2Text = 0;
        if (media == null) {
            if (!item.isPlayed()) {
                butAction1Icon = "{fa-check 24sp}";
                butAction1Text = R.string.mark_read_label;
            }
            if (item.getLink() != null) {
                butAction2Icon = "{ma-web 24sp}";
                butAction2Text = R.string.visit_website_label;
            }
        } else {
            if(media.getDuration() > 0) {
                txtvDuration.setText(Converter.getDurationStringLong(media.getDuration()));
            }
            boolean isDownloading = DownloadRequester.getInstance().isDownloadingFile(media);
            if (!media.isDownloaded()) {
                butAction2Icon = "{md-settings-input-antenna 24sp}";
                butAction2Text = R.string.stream_label;
            } else {
                butAction2Icon = "{md-delete 24sp}";
                butAction2Text = R.string.remove_label;
            }
            if (isDownloading) {
                butAction1Icon = "{md-cancel 24sp}";
                butAction1Text = R.string.cancel_label;
            } else if (media.isDownloaded()) {
                butAction1Icon = "{md-play-arrow 24sp}";
                butAction1Text = R.string.play_label;
            } else {
                butAction1Icon = "{md-file-download 24sp}";
                butAction1Text = R.string.download_label;
            }
        }
        if(butAction1Icon != null && butAction1Text != 0) {
            butAction1.setText(butAction1Icon +"\u0020\u0020" + getActivity().getString(butAction1Text));
            Iconify.addIcons(butAction1);
            butAction1.setVisibility(View.VISIBLE);
        } else {
            butAction1.setVisibility(View.INVISIBLE);
        }
        if(butAction2Icon != null && butAction2Text != 0) {
            butAction2.setText(butAction2Icon +"\u0020\u0020" + getActivity().getString(butAction2Text));
            Iconify.addIcons(butAction2);
            butAction2.setVisibility(View.VISIBLE);
        } else {
            butAction2.setVisibility(View.INVISIBLE);
        }
    }

    private View.OnLongClickListener webViewLongClickListener = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            WebView.HitTestResult r = webvDescription.getHitTestResult();
            if (r != null
                    && r.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                Log.d(TAG, "Link of webview was long-pressed. Extra: " + r.getExtra());
                selectedURL = r.getExtra();
                webvDescription.showContextMenu();
                return true;
            }
            selectedURL = null;
            return false;
        }
    };

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        boolean handled = selectedURL != null;
        if (selectedURL != null) {
            switch (item.getItemId()) {
                case R.id.open_in_browser_item:
                    Uri uri = Uri.parse(selectedURL);
                    final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    if(IntentUtils.isCallable(getActivity(), intent)) {
                        getActivity().startActivity(intent);
                    }
                    break;
                case R.id.share_url_item:
                    ShareUtils.shareLink(getActivity(), selectedURL);
                    break;
                case R.id.copy_url_item:
                    if (android.os.Build.VERSION.SDK_INT >= 11) {
                        ClipData clipData = ClipData.newPlainText(selectedURL,
                                selectedURL);
                        android.content.ClipboardManager cm = (android.content.ClipboardManager) getActivity()
                                .getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(clipData);
                    } else {
                        android.text.ClipboardManager cm = (android.text.ClipboardManager) getActivity()
                                .getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setText(selectedURL);
                    }
                    Toast t = Toast.makeText(getActivity(),
                            R.string.copied_url_msg, Toast.LENGTH_SHORT);
                    t.show();
                    break;
                default:
                    handled = false;
                    break;

            }
            selectedURL = null;
        }
        return handled;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (selectedURL != null) {
            super.onCreateContextMenu(menu, v, menuInfo);
                Uri uri = Uri.parse(selectedURL);
                final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                if(IntentUtils.isCallable(getActivity(), intent)) {
                    menu.add(Menu.NONE, R.id.open_in_browser_item, Menu.NONE,
                            R.string.open_in_browser_label);
                }
                menu.add(Menu.NONE, R.id.copy_url_item, Menu.NONE,
                        R.string.copy_url_label);
                menu.add(Menu.NONE, R.id.share_url_item, Menu.NONE,
                        R.string.share_url_label);
                menu.setHeaderTitle(selectedURL);
        }
    }

    public void onEventMainThread(QueueEvent event) {
        if(event.contains(itemID)) {
            load();
        }
    }

    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        for(FeedItem item : event.items) {
            if(itemID == item.getId()) {
                load();
                return;
            }
        }
    }

    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        DownloaderUpdate update = event.update;
        downloaderList = update.downloaders;
        if(item == null || item.getMedia() == null) {
            return;
        }
        long mediaId = item.getMedia().getId();
        if(ArrayUtils.contains(update.mediaIds, mediaId)) {
            if (itemsLoaded && getActivity() != null) {
                updateAppearance();
            }
        }
    }


    private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {
        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((arg & EVENTS) != 0) {
                load();
            }
        }
    };

    private void load() {
        if(subscription != null) {
            subscription.unsubscribe();
        }
        subscription = Observable.fromCallable(() -> loadInBackground())
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(result -> {
                item = result.first;
                queue = result.second;
                if (!itemsLoaded) {
                    itemsLoaded = true;
                    onFragmentLoaded();
                } else {
                    updateAppearance();
                }
            }, error -> {
                Log.e(TAG, Log.getStackTraceString(error));
            });
    }

    private Pair<FeedItem,LongList> loadInBackground() {
        FeedItem data1 = DBReader.getFeedItem(itemID);
        if (data1 != null) {
            Timeline t = new Timeline(getActivity(), data1);
            webviewData = t.processShownotes(false);
        }
        LongList data2 = DBReader.getQueueIDList();
        return Pair.create(data1, data2);
    }

}
