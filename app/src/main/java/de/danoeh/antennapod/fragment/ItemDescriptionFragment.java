package de.danoeh.antennapod.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.ShareUtils;
import de.danoeh.antennapod.core.util.ShownotesProvider;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.core.util.playback.Timeline;

/**
 * Displays the description of a Playable object in a Webview.
 */
public class ItemDescriptionFragment extends Fragment {

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


    private AsyncTask<Void, Void, Void> webViewLoader;

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
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Creating view");
        webvDescription = new WebView(getActivity());
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
                LayoutAlgorithm.NARROW_COLUMNS);
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
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Page finished");
                // Restoring the scroll position might not always work
                view.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        restoreFromPreference();
                    }

                }, 50);
            }

        });

        registerForContextMenu(webvDescription);
        return webvDescription;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Fragment attached");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Fragment detached");
        if (webViewLoader != null) {
            webViewLoader.cancel(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Fragment destroyed");
        if (webViewLoader != null) {
            webViewLoader.cancel(true);
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
        if (BuildConfig.DEBUG)
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
            media = args.getParcelable(ARG_PLAYABLE);
            shownotesProvider = media;
            startLoader();
        } else if (args.containsKey(ARG_FEEDITEM_ID)) {
            AsyncTask<Void, Void, FeedItem> itemLoadTask = new AsyncTask<Void, Void, FeedItem>() {

                @Override
                protected FeedItem doInBackground(Void... voids) {
                    return DBReader.getFeedItem(getActivity(), getArguments().getLong(ARG_FEEDITEM_ID));
                }

                @Override
                protected void onPostExecute(FeedItem feedItem) {
                    super.onPostExecute(feedItem);
                    shownotesProvider = feedItem;
                    startLoader();
                }
            };
            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
                itemLoadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                itemLoadTask.execute();
            }
        }


    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @SuppressLint("NewApi")
    private void startLoader() {
        webViewLoader = createLoader();
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
            webViewLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            webViewLoader.execute();
        }
    }

    private View.OnLongClickListener webViewLongClickListener = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            WebView.HitTestResult r = webvDescription.getHitTestResult();
            if (r != null
                    && r.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG,
                            "Link of webview was long-pressed. Extra: "
                                    + r.getExtra()
                    );
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
                    getActivity()
                            .startActivity(new Intent(Intent.ACTION_VIEW, uri));
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
                menu.add(Menu.NONE, R.id.open_in_browser_item, Menu.NONE,
                        R.string.open_in_browser_label);
                menu.add(Menu.NONE, R.id.copy_url_item, Menu.NONE,
                        R.string.copy_url_label);
                menu.add(Menu.NONE, R.id.share_url_item, Menu.NONE,
                        R.string.share_url_label);
                menu.setHeaderTitle(selectedURL);
            }
        }
    }

    private AsyncTask<Void, Void, Void> createLoader() {
        return new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onCancelled() {
                super.onCancelled();
                webViewLoader = null;
            }

            String data;

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                // /webvDescription.loadData(url, "text/html", "utf-8");
                webvDescription.loadDataWithBaseURL(null, data, "text/html",
                        "utf-8", "about:blank");
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Webview loaded");
                webViewLoader = null;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Loading Webview");
                try {
                    Activity activity = getActivity();
                    if (activity != null) {
                        Timeline timeline = new Timeline(activity, shownotesProvider);
                        data = timeline.processShownotes(highlightTimecodes);
                    } else {
                        cancel(true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

        };
    }

    @Override
    public void onPause() {
        super.onPause();
        savePreference();
    }

    private void savePreference() {
        if (saveState) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Saving preferences");
            SharedPreferences prefs = getActivity().getSharedPreferences(PREF,
                    Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            if (media != null && webvDescription != null) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG,
                            "Saving scroll position: "
                                    + webvDescription.getScrollY()
                    );
                editor.putInt(PREF_SCROLL_Y, webvDescription.getScrollY());
                editor.putString(PREF_PLAYABLE_ID, media.getIdentifier()
                        .toString());
            } else {
                if (BuildConfig.DEBUG)
                    Log.d(TAG,
                            "savePreferences was called while media or webview was null");
                editor.putInt(PREF_SCROLL_Y, -1);
                editor.putString(PREF_PLAYABLE_ID, "");
            }
            editor.commit();
        }
    }

    private boolean restoreFromPreference() {
        if (saveState) {
            if (BuildConfig.DEBUG)
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
                    if (BuildConfig.DEBUG)
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
        if (getActivity() != null && getActivity() instanceof ItemDescriptionFragmentCallback) {
            PlaybackController pc = ((ItemDescriptionFragmentCallback) getActivity()).getPlaybackController();
            if (pc != null) {
                pc.seekTo(time);
            }
        }
    }

    public interface ItemDescriptionFragmentCallback {
        public PlaybackController getPlaybackController();
    }
}
