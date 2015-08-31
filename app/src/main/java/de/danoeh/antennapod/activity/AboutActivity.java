package de.danoeh.antennapod.activity;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Displays the 'about' screen
 */
public class AboutActivity extends ActionBarActivity {

    private static final String TAG = AboutActivity.class.getSimpleName();

    private WebView webview;
    private LinearLayout webviewContainer;

    private Subscription subscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.about);
        webviewContainer = (LinearLayout) findViewById(R.id.webvContainer);
        webview = (WebView) findViewById(R.id.webvAbout);
        if (UserPreferences.getTheme() == R.style.Theme_AntennaPod_Dark) {
            if (Build.VERSION.SDK_INT >= 11
                    && Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                webview.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
            webview.setBackgroundColor(Color.TRANSPARENT);
        }
        webview.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return false;
            }

        });
        subscription = Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        InputStream input = null;
                        try {
                            TypedArray res = AboutActivity.this.getTheme().obtainStyledAttributes(
                                    new int[] { android.R.attr.textColorPrimary });
                            int colorResource = res.getColor(0, 0);
                            String colorString = String.format("#%06X", 0xFFFFFF & colorResource);
                            res.recycle();
                            input = getAssets().open("about.html");
                            String webViewData = IOUtils.toString(input, Charset.defaultCharset());
                            webViewData = String.format(webViewData, colorString);
                            subscriber.onNext(webViewData);
                        } catch (IOException e) {
                            subscriber.onError(e);
                        } finally {
                            IOUtils.closeQuietly(input);
                        }
                        subscriber.onCompleted();
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(webviewData -> {
                    webview.loadDataWithBaseURL(null, webviewData, "text/html", "utf-8", "about:blank");
                }, error -> {
                    Log.e(TAG, Log.getStackTraceString(error));
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(subscription != null) {
            subscription.unsubscribe();
        }
        if (webviewContainer != null && webview != null) {
            webviewContainer.removeAllViews();
            webview.destroy();
        }
    }
}
