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
import android.support.annotation.NonNull;
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
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.core.util.ShareUtils;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.core.util.playback.Timeline;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Displays the description of a Playable object in a Webview.
 */
public class ItemDescriptionFragment extends Fragment {

    private static final String TAG = "ItemDescriptionFragment";

    private static final String PREF = "ItemDescriptionFragmentPrefs";
    private static final String PREF_SCROLL_Y = "prefScrollY";
    private static final String PREF_PLAYABLE_ID = "prefPlayableId";

    private WebView webvDescription;
    private Disposable webViewLoader;
    private PlaybackController controller;

    /**
     * URL that was selected via long-press.
     */
    private String selectedURL;


    @SuppressLint("NewApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "Creating view");
        webvDescription = new WebView(getActivity().getApplicationContext());
        webvDescription.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        TypedArray ta = getActivity().getTheme().obtainStyledAttributes(new int[]
                {android.R.attr.colorBackground});
        boolean black = UserPreferences.getTheme() == R.style.Theme_AntennaPod_Dark
                || UserPreferences.getTheme() == R.style.Theme_AntennaPod_TrueBlack;
        int backgroundColor = ta.getColor(0, black ? Color.BLACK : Color.WHITE);

        ta.recycle();
        webvDescription.setBackgroundColor(backgroundColor);
        if (!NetworkUtils.networkAvailable()) {
            webvDescription.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            // Use cached resources, even if they have expired
        }
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
            webViewLoader.dispose();
        }
        if (webvDescription != null) {
            webvDescription.removeAllViews();
            webvDescription.destroy();
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
            webViewLoader.dispose();
        }
        webViewLoader = Observable.fromCallable(this::loadData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    webvDescription.loadDataWithBaseURL("https://127.0.0.1", data, "text/html",
                            "utf-8", "about:blank");
                    Log.d(TAG, "Webview loaded");
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @NonNull
    private String loadData() {
        Timeline timeline = new Timeline(getActivity(), controller.getMedia());
        return timeline.processShownotes(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        savePreference();
    }

    private void savePreference() {
        Log.d(TAG, "Saving preferences");
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF,
                Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (controller.getMedia() != null && webvDescription != null) {
            Log.d(TAG, "Saving scroll position: " + webvDescription.getScrollY());
            editor.putInt(PREF_SCROLL_Y, webvDescription.getScrollY());
            editor.putString(PREF_PLAYABLE_ID, controller.getMedia().getIdentifier()
                    .toString());
        } else {
            Log.d(TAG, "savePreferences was called while media or webview was null");
            editor.putInt(PREF_SCROLL_Y, -1);
            editor.putString(PREF_PLAYABLE_ID, "");
        }
        editor.commit();
    }

    private boolean restoreFromPreference() {
        Log.d(TAG, "Restoring from preferences");
        Activity activity = getActivity();
        if (activity != null) {
            SharedPreferences prefs = activity.getSharedPreferences(
                    PREF, Activity.MODE_PRIVATE);
            String id = prefs.getString(PREF_PLAYABLE_ID, "");
            int scrollY = prefs.getInt(PREF_SCROLL_Y, -1);
            if (scrollY != -1 && controller.getMedia() != null
                    && id.equals(controller.getMedia().getIdentifier().toString())
                    && webvDescription != null) {
                Log.d(TAG, "Restored scroll Position: " + scrollY);
                webvDescription.scrollTo(webvDescription.getScrollX(),
                        scrollY);
                return true;
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
    public void onStart() {
        super.onStart();
        controller = new PlaybackController(getActivity(), false) {
            @Override
            public boolean loadMediaInfo() {
                if (getMedia() == null) {
                    return false;
                }
                load();
                return true;
            }

        };
        controller.init();
        load();
    }

    @Override
    public void onStop() {
        super.onStop();
        controller.release();
        controller = null;
    }
}
