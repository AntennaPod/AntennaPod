package de.danoeh.antennapod.activity;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.SherlockActivity;

import de.danoeh.antennapod.R;

/** Displays the 'about' screen */
public class AboutActivity extends SherlockActivity {

	private WebView webview;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().hide();
		setContentView(R.layout.about);		
		webview = (WebView) findViewById(R.id.webvAbout);
		webview.setWebViewClient(new WebViewClient() {

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return false;
			}
			
		});
		webview.loadUrl("file:///android_asset/about.html");
	}

}
