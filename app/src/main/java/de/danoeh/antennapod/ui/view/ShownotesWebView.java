package de.danoeh.antennapod.ui.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;

import com.google.android.material.snackbar.Snackbar;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.ui.MenuItemUtils;
import de.danoeh.antennapod.ui.common.Converter;
import de.danoeh.antennapod.ui.common.IntentUtils;
import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.ui.share.ShareUtils;
import de.danoeh.antennapod.ui.cleaner.ShownotesCleaner;
import org.greenrobot.eventbus.EventBus;

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
        getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        getSettings().setUseWideViewPort(false);
        getSettings().setLoadWithOverviewMode(true);
        setOnLongClickListener(this);

        setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (ShownotesCleaner.isTimecodeLink(url) && timecodeSelectedListener != null) {
                    timecodeSelectedListener.accept(ShownotesCleaner.getTimecodeLinkTime(url));
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

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                ViewGroup parent = (ViewGroup) view.getParent();
                if (parent == null) {
                    return true;
                }
                ViewGroup.LayoutParams params = parent.getLayoutParams();
                TextView errorText = new TextView(getContext());
                int position = parent.indexOfChild(view);
                parent.removeView(view);
                parent.addView(errorText, position, params);
                int padding = (int) (40 * getContext().getResources().getDisplayMetrics().density);
                errorText.setPadding(padding, padding, padding, padding);
                errorText.setGravity(Gravity.CENTER);
                errorText.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                errorText.setText("Your Android System WebView crashed. Try restarting the phone. If this happens "
                        + "repeatedly, contact the phone manufacturer or the creator of your custom ROM.");
                return true;
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
        } else if (r != null && r.getType() == HitTestResult.EMAIL_TYPE) {
            Log.d(TAG, "E-Mail of webview was long-pressed. Extra: " + r.getExtra());
            ClipboardManager clipboardManager = ContextCompat.getSystemService(this.getContext(),
                                                                               ClipboardManager.class);
            if (clipboardManager != null) {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("AntennaPod", r.getExtra()));
            }
            if (Build.VERSION.SDK_INT <= 32) {
                EventBus.getDefault().post(new MessageEvent(
                        getContext().getResources().getString(R.string.copied_to_clipboard)));
            }
            return true;
        }
        selectedUrl = null;
        return false;
    }

    public boolean onContextItemSelected(MenuItem item) {
        if (selectedUrl == null) {
            return false;
        }

        final int itemId = item.getItemId();
        if (itemId == R.id.open_in_browser_item) {
            IntentUtils.openInBrowser(getContext(), selectedUrl);
        } else if (itemId == R.id.share_url_item) {
            ShareUtils.shareLink(getContext(), selectedUrl);
        } else if (itemId == R.id.copy_url_item) {
            ClipData clipData = ClipData.newPlainText(selectedUrl, selectedUrl);
            ClipboardManager cm = (ClipboardManager) getContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(clipData);
            if (Build.VERSION.SDK_INT < 32) {
                Snackbar s = Snackbar.make(this, R.string.copied_to_clipboard, Snackbar.LENGTH_LONG);
                s.getView().setElevation(100);
                s.show();
            }
        } else if (itemId == R.id.go_to_position_item) {
            if (ShownotesCleaner.isTimecodeLink(selectedUrl) && timecodeSelectedListener != null) {
                timecodeSelectedListener.accept(ShownotesCleaner.getTimecodeLinkTime(selectedUrl));
            } else {
                Log.e(TAG, "Selected go_to_position_item, but URL was no timecode link: " + selectedUrl);
            }
        } else {
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

        if (ShownotesCleaner.isTimecodeLink(selectedUrl)) {
            menu.add(Menu.NONE, R.id.go_to_position_item, Menu.NONE, R.string.go_to_position_label);
            menu.setHeaderTitle(Converter.getDurationStringLong(ShownotesCleaner.getTimecodeLinkTime(selectedUrl)));
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
        MenuItemUtils.setOnClickListeners(menu, this::onContextItemSelected);
    }

    public void setTimecodeSelectedListener(Consumer<Integer> timecodeSelectedListener) {
        this.timecodeSelectedListener = timecodeSelectedListener;
    }

    public void setPageFinishedListener(Runnable pageFinishedListener) {
        this.pageFinishedListener = pageFinishedListener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(Math.max(getMeasuredWidth(), getMinimumWidth()),
                Math.max(getMeasuredHeight(), getMinimumHeight()));
    }
}
