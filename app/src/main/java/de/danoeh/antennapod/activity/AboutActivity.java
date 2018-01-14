package de.danoeh.antennapod.activity;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
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
public class AboutActivity extends AppCompatActivity {

    private static final String TAG = AboutActivity.class.getSimpleName();

    private WebView webview;
    private LinearLayout webviewContainer;
    private int depth = 0;

    private Subscription subscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.about);
        webviewContainer = (LinearLayout) findViewById(R.id.webvContainer);
        webview = (WebView) findViewById(R.id.webvAbout);
        webview.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
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
                if(url.startsWith("http")) {
                    depth++;
                    return false;
                } else {
                    url = url.replace("file:///android_asset/", "");
                    loadAsset(url);
                    return true;
                }
            }

        });
        loadAsset("about.html");
    }

    private void loadAsset(String filename) {
        subscription = Observable.create((Observable.OnSubscribe<String>) subscriber -> {
            InputStream input = null;
            try {
                TypedArray res = AboutActivity.this.getTheme().obtainStyledAttributes(
                        new int[] { android.R.attr.textColorPrimary });
                int colorResource = res.getColor(0, 0);
                String colorString = String.format("#%06X", 0xFFFFFF & colorResource);
                res.recycle();
                input = getAssets().open(filename);
                String webViewData = IOUtils.toString(input, Charset.defaultCharset());
                if(!webViewData.startsWith("<!DOCTYPE html>")) {
                    //webViewData = webViewData.replace("\n\n", "</p><p>");
                    webViewData = webViewData.replace("%", "&#37;");
                    webViewData =
                            "<!DOCTYPE html>" +
                            "<html>" +
                            "<head>" +
                            "    <meta http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\">" +
                            "    <style type=\"text/css\">" +
                            "        @font-face {" +
                            "        font-family: 'Roboto-Light';" +
                            "           src: url('file:///android_asset/Roboto-Light.ttf');" +
                            "        }" +
                            "        * {" +
                            "           color: %s;" +
                            "           font-family: roboto-Light;" +
                            "           font-size: 8pt;" +
                            "        }" +
                            "    </style>" +
                            "</head><body><p>" + webViewData + "</p></body></html>";
                    webViewData = webViewData.replace("\n", "<br/>");
                    depth++;
                } else {
                    depth = 0;
                }
                webViewData = String.format(webViewData, colorString);
                subscriber.onNext(webViewData);
            } catch (IOException e) {
                subscriber.onError(e);
            } finally {
                IOUtils.closeQuietly(input);
            }
            subscriber.onCompleted();
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        webviewData ->
                                webview.loadDataWithBaseURL("file:///android_asset/", webviewData, "text/html", "utf-8", "about:blank"),
                        error -> Log.e(TAG, Log.getStackTraceString(error))
                );
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "depth: " + depth);
        if(depth == 1) {
            loadAsset("about.html");
        } else if(depth > 1) {
            webview.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
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
