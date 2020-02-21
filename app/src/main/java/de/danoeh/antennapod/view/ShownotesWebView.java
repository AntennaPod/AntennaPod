package de.danoeh.antennapod.view;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.content.ContextCompat;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.Consumer;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.core.util.ShareUtils;
import de.danoeh.antennapod.core.util.playback.Timeline;

public class ShownotesWebView extends WebView implements View.OnLongClickListener {
    private static final String TAG = "ShownotesWebView";

    /**
     * URL that was selected via long-press.
     */
    private String selectedUrl;
    private Consumer<Integer> timecodeSelectedListener;
    private Runnable pageFinishedListener;

    public ShownotesWebView(Context context) {
        super(context);
        setup();
    }

    public ShownotesWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public ShownotesWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    private void setup() {
        setBackgroundColor(Color.TRANSPARENT);
        if (!NetworkUtils.networkAvailable()) {
            getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            // Use cached resources, even if they have expired
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        getSettings().setUseWideViewPort(false);
        getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        getSettings().setLoadWithOverviewMode(true);
        setOnLongClickListener(this);

        setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (Timeline.isTimecodeLink(url) && timecodeSelectedListener != null) {
                    timecodeSelectedListener.accept(Timeline.getTimecodeLinkTime(selectedUrl));
                } else {
                    IntentUtils.openInBrowser(getContext(), url);
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Page finished");
                if (pageFinishedListener != null) {
                    pageFinishedListener.run();
                }
            }
        });
    }

    @Override
    public boolean onLongClick(View v) {
        WebView.HitTestResult r = getHitTestResult();
        if (r != null && r.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
            Log.d(TAG, "Link of webview was long-pressed. Extra: " + r.getExtra());
            selectedUrl = r.getExtra();
            showContextMenu();
            return true;
        }
        selectedUrl = null;
        return false;
    }

    public boolean onContextItemSelected(MenuItem item) {
        if (selectedUrl == null) {
            return false;
        }

        switch (item.getItemId()) {
            case R.id.open_in_browser_item:
                IntentUtils.openInBrowser(getContext(), selectedUrl);
                break;
            case R.id.share_url_item:
                ShareUtils.shareLink(getContext(), selectedUrl);
                break;
            case R.id.copy_url_item:
                ClipData clipData = ClipData.newPlainText(selectedUrl, selectedUrl);
                android.content.ClipboardManager cm = (android.content.ClipboardManager) getContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(clipData);
                Snackbar.make(this, R.string.copied_url_msg, Snackbar.LENGTH_LONG).show();
                break;
            case R.id.go_to_position_item:
                if (Timeline.isTimecodeLink(selectedUrl) && timecodeSelectedListener != null) {
                    timecodeSelectedListener.accept(Timeline.getTimecodeLinkTime(selectedUrl));
                } else {
                    Log.e(TAG, "Selected go_to_position_item, but URL was no timecode link: " + selectedUrl);
                }
                break;
            default:
                selectedUrl = null;
                return false;

        }
        selectedUrl = null;
        return true;
    }

    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        super.onCreateContextMenu(menu);
        if (selectedUrl == null) {
            return;
        }

        if (Timeline.isTimecodeLink(selectedUrl)) {
            menu.add(Menu.NONE, R.id.go_to_position_item, Menu.NONE, R.string.go_to_position_label);
            menu.setHeaderTitle(Converter.getDurationStringLong(Timeline.getTimecodeLinkTime(selectedUrl)));
        } else {
            Uri uri = Uri.parse(selectedUrl);
            final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (IntentUtils.isCallable(getContext(), intent)) {
                menu.add(Menu.NONE, R.id.open_in_browser_item, Menu.NONE, R.string.open_in_browser_label);
            }
            menu.add(Menu.NONE, R.id.copy_url_item, Menu.NONE, R.string.copy_url_label);
            menu.add(Menu.NONE, R.id.share_url_item, Menu.NONE, R.string.share_url_label);
            menu.setHeaderTitle(selectedUrl);
        }
    }

    public void setTimecodeSelectedListener(Consumer<Integer> timecodeSelectedListener) {
        this.timecodeSelectedListener = timecodeSelectedListener;
    }

    public void setPageFinishedListener(Runnable pageFinishedListener) {
        this.pageFinishedListener = pageFinishedListener;
    }
}
