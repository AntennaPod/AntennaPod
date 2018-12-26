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
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Displays the 'about' screen
 */
public class AboutActivity extends AppCompatActivity {

    private static final String TAG = AboutActivity.class.getSimpleName();

    private WebView webView;
    private LinearLayout webViewContainer;
    private Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.about);
        webViewContainer = findViewById(R.id.webViewContainer);
        webView = findViewById(R.id.webViewAbout);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        if (UserPreferences.getTheme() == R.style.Theme_AntennaPod_Dark) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
            webView.setBackgroundColor(Color.TRANSPARENT);
        }
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!url.startsWith("http")) {
                    url = url.replace("file:///android_asset/", "");
                    loadAsset(url);
                    return true;
                }
                return false;
            }

        });
        loadAsset("about.html");
    }

    private void loadAsset(String filename) {
        disposable = Single.create(subscriber -> {
            InputStream input = null;
            try {
                TypedArray res = AboutActivity.this.getTheme().obtainStyledAttributes(
                        new int[] { R.attr.about_screen_font_color, R.attr.about_screen_background,
                        R.attr.about_screen_card_background, R.attr.about_screen_card_border});
                String fontColor = String.format("#%06X", 0xFFFFFF & res.getColor(0, 0));
                String backgroundColor = String.format("#%06X", 0xFFFFFF & res.getColor(1, 0));
                String cardBackground = String.format("#%06X", 0xFFFFFF & res.getColor(2, 0));
                String cardBorder = String.format("#%06X", 0xFFFFFF & res.getColor(3, 0));
                res.recycle();
                input = getAssets().open(filename);
                String webViewData = IOUtils.toString(input, Charset.defaultCharset());
                if (!webViewData.startsWith("<!DOCTYPE html>")) {
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
                            "           color: @fontcolor@;" +
                            "           font-family: roboto-Light;" +
                            "           font-size: 8pt;" +
                            "        }" +
                            "    </style>" +
                            "</head><body><p>" + webViewData + "</p></body></html>";
                    webViewData = webViewData.replace("\n", "<br/>");
                }
                webViewData = webViewData.replace("@fontcolor@", fontColor);
                webViewData = webViewData.replace("@background@", backgroundColor);
                webViewData = webViewData.replace("@card_background@", cardBackground);
                webViewData = webViewData.replace("@card_border@", cardBorder);
                subscriber.onSuccess(webViewData);
            } catch (IOException e) {
                Log.e(TAG, Log.getStackTraceString(e));
                subscriber.onError(e);
            } finally {
                IOUtils.closeQuietly(input);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        webViewData ->
                                webView.loadDataWithBaseURL("file:///android_asset/", webViewData.toString(), "text/html", "utf-8", "file:///android_asset/" + filename.toString()),
                        error -> Log.e(TAG, Log.getStackTraceString(error))
                );
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
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
        if(disposable != null) {
            disposable.dispose();
        }
        if (webViewContainer != null && webView != null) {
            webViewContainer.removeAllViews();
            webView.destroy();
        }
    }
}
