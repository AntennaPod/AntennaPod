package de.danoeh.antennapod.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MediaplayerInfoActivity;
import de.danoeh.antennapod.activity.MediaplayerInfoActivity.MediaplayerInfoContentFragment;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.ShareUtils;
import de.danoeh.antennapod.core.util.ShownotesProvider;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.core.util.playback.Timeline;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Displays the description of a Playable object in a Webview.
 */
public class ItemDescriptionFragment extends Fragment implements MediaplayerInfoContentFragment {

    private static final String TAG = "ItemDescriptionFragment";

    private static final String PREF = "ItemDescriptionFragmentPrefs";
    private static final String PREF_SCROLL_Y = "prefScrollY";
    private static final String PREF_PLAYABLE_ID = "prefPlayableId";

    private static final String ARG_PLAYABLE = "arg.playable";
    private static final String ARG_FEEDITEM_ID = "arg.feeditem";

    private static final String ARG_SAVE_STATE = "arg.saveState";
    private static final String ARG_HIGHLIGHT_TIMECODES = "arg.highlightTimecodes";

    private WebView webvDescription;

    private ShownotesProvider shownotesProvider;
    private Playable media;

    private Subscription webViewLoader;

    /**
     * URL that was selected via long-press.
     */
    private String selectedURL;

    /**
     * True if Fragment should save its state (e.g. scrolling position) in a
     * shared preference.
     */
    private boolean saveState;

    /**
     * True if Fragment should highlight timecodes (e.g. time codes in the HH:MM:SS format).
     */
    private boolean highlightTimecodes;

    public static ItemDescriptionFragment newInstance(Playable media,
                                                      boolean saveState,
                                                      boolean highlightTimecodes) {
        ItemDescriptionFragment f = new ItemDescriptionFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PLAYABLE, media);
        args.putBoolean(ARG_SAVE_STATE, saveState);
        args.putBoolean(ARG_HIGHLIGHT_TIMECODES, highlightTimecodes);
        f.setArguments(args);
        return f;
    }

    public static ItemDescriptionFragment newInstance(FeedItem item, boolean saveState, boolean highlightTimecodes) {
        ItemDescriptionFragment f = new ItemDescriptionFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_FEEDITEM_ID, item.getId());
        args.putBoolean(ARG_SAVE_STATE, saveState);
        args.putBoolean(ARG_HIGHLIGHT_TIMECODES, highlightTimecodes);
        f.setArguments(args);
        return f;
    }

    @SuppressLint("NewApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "Creating view");
        webvDescription = new WebView(getActivity().getApplicationContext());
        webvDescription.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        TypedArray ta = getActivity().getTheme().obtainStyledAttributes(new int[]
                {android.R.attr.colorBackground});
        int backgroundColor = ta.getColor(0, UserPreferences.getTheme() ==
                R.style.Theme_AntennaPod_Dark ? Color.BLACK : Color.WHITE);
        ta.recycle();
        webvDescription.setBackgroundColor(backgroundColor);
        webvDescription.getSettings().setUseWideViewPort(false);
        webvDescription.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webvDescription.getSettings().setLoadWithOverviewMode(true);
        webvDescription.setOnLongClickListener(webViewLongClickListener);
        webvDescription.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (Timeline.isTimecodeLink(url)) {
                    onTimecodeLinkSelected(url);
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                        return true;
                    }
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Page finished");
                // Restoring the scroll position might not always work
                view.postDelayed(ItemDescriptionFragment.this::restoreFromPreference, 50);
            }

        });

        registerForContextMenu(webvDescription);
        return webvDescription;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Fragment destroyed");
        if (webViewLoader != null) {
            webViewLoader.unsubscribe();
        }
        if (webvDescription != null) {
            webvDescription.removeAllViews();
            webvDescription.destroy();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Creating fragment");
        Bundle args = getArguments();
        saveState = args.getBoolean(ARG_SAVE_STATE, false);
        highlightTimecodes = args.getBoolean(ARG_HIGHLIGHT_TIMECODES, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args.containsKey(ARG_PLAYABLE)) {
            if (media == null) {
                media = args.getParcelable(ARG_PLAYABLE);
                shownotesProvider = media;
            }
            load();
        } else if (args.containsKey(ARG_FEEDITEM_ID)) {
            long id = getArguments().getLong(ARG_FEEDITEM_ID);
            Observable.defer(() -> Observable.just(DBReader.getFeedItem(id)))
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(feedItem -> {
                        shownotesProvider = feedItem;
                        load();
                    }, error -> Log.e(TAG, Log.getStackTraceString(error)));
        }
    }


    private final View.OnLongClickListener webViewLongClickListener = new View.OnLongClickListener() {

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

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
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
                    ClipData clipData = ClipData.newPlainText(selectedURL,
                            selectedURL);
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) getActivity()
                            .getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(clipData);
                    Toast t = Toast.makeText(getActivity(),
                            R.string.copied_url_msg, Toast.LENGTH_SHORT);
                    t.show();
                    break;
                case R.id.go_to_position_item:
                    if (Timeline.isTimecodeLink(selectedURL)) {
                        onTimecodeLinkSelected(selectedURL);
                    } else {
                        Log.e(TAG, "Selected go_to_position_item, but URL was no timecode link: " + selectedURL);
                    }
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
                                    ContextMenuInfo menuInfo) {
        if (selectedURL != null) {
            super.onCreateContextMenu(menu, v, menuInfo);
            if (Timeline.isTimecodeLink(selectedURL)) {
                menu.add(Menu.NONE, R.id.go_to_position_item, Menu.NONE,
                        R.string.go_to_position_label);
                menu.setHeaderTitle(Converter.getDurationStringLong(Timeline.getTimecodeLinkTime(selectedURL)));
            } else {
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
    }

    private void load() {
        Log.d(TAG, "load()");
        if(webViewLoader != null) {
            webViewLoader.unsubscribe();
        }
        if(shownotesProvider == null) {
            return;
        }
        webViewLoader = Observable.defer(() -> Observable.just(loadData()))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    webvDescription.loadDataWithBaseURL(null, data, "text/html",
                            "utf-8", "about:blank");
                    Log.d(TAG, "Webview loaded");
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private String loadData() {
        Timeline timeline = new Timeline(getActivity(), shownotesProvider);
        return timeline.processShownotes(highlightTimecodes);
    }

    @Override
    public void onPause() {
        super.onPause();
        savePreference();
    }

    private void savePreference() {
        if (saveState) {
            Log.d(TAG, "Saving preferences");
            SharedPreferences prefs = getActivity().getSharedPreferences(PREF,
                    Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            if (media != null && webvDescription != null) {
                Log.d(TAG, "Saving scroll position: " + webvDescription.getScrollY());
                editor.putInt(PREF_SCROLL_Y, webvDescription.getScrollY());
                editor.putString(PREF_PLAYABLE_ID, media.getIdentifier()
                        .toString());
            } else {
                Log.d(TAG, "savePreferences was called while media or webview was null");
                editor.putInt(PREF_SCROLL_Y, -1);
                editor.putString(PREF_PLAYABLE_ID, "");
            }
            editor.commit();
        }
    }

    private boolean restoreFromPreference() {
        if (saveState) {
            Log.d(TAG, "Restoring from preferences");
            Activity activity = getActivity();
            if (activity != null) {
                SharedPreferences prefs = activity.getSharedPreferences(
                        PREF, Activity.MODE_PRIVATE);
                String id = prefs.getString(PREF_PLAYABLE_ID, "");
                int scrollY = prefs.getInt(PREF_SCROLL_Y, -1);
                if (scrollY != -1 && media != null
                        && id.equals(media.getIdentifier().toString())
                        && webvDescription != null) {
                    Log.d(TAG, "Restored scroll Position: " + scrollY);
                    webvDescription.scrollTo(webvDescription.getScrollX(),
                            scrollY);
                    return true;
                }
            }
        }
        return false;
    }

    private void onTimecodeLinkSelected(String link) {
        int time = Timeline.getTimecodeLinkTime(link);
        if (getActivity() != null && getActivity() instanceof MediaplayerInfoActivity) {
            PlaybackController pc = ((MediaplayerInfoActivity) getActivity()).getPlaybackController();
            if (pc != null) {
                pc.seekTo(time);
            }
        }
    }

    @Override
    public void onMediaChanged(Playable media) {
        if(this.media == media) {
            return;
        }
        this.media = media;
        this.shownotesProvider = media;
        if (webvDescription != null) {
            load();
        }
    }

}
