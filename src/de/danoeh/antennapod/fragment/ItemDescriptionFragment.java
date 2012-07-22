package de.danoeh.antennapod.fragment;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang3.StringEscapeUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockFragment;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;

/** Displays the description of a FeedItem in a Webview. */
public class ItemDescriptionFragment extends SherlockFragment {

	private static final String TAG = "ItemDescriptionFragment";
	private static final String ARG_FEED_ID = "arg.feedId";
	private static final String ARG_FEEDITEM_ID = "arg.feedItemId";
	private static final String ARG_SCROLLBAR_ENABLED = "arg.scrollbarEnabled";

	private static final String WEBVIEW_STYLE = "<head><style type=\"text/css\"> * { font-family: Helvetica; line-height: 1.5em; font-size: 12pt; } a { font-style: normal; text-decoration: none; font-weight: normal; color: #00A8DF; }</style></head>";

	private boolean scrollbarEnabled;
	private WebView webvDescription;
	private FeedItem item;

	private AsyncTask<Void, Void, Void> webViewLoader;

	public static ItemDescriptionFragment newInstance(FeedItem item,
			boolean scrollbarEnabled) {
		ItemDescriptionFragment f = new ItemDescriptionFragment();
		Bundle args = new Bundle();
		args.putLong(ARG_FEED_ID, item.getFeed().getId());
		args.putLong(ARG_FEEDITEM_ID, item.getId());
		args.putBoolean(ARG_SCROLLBAR_ENABLED, scrollbarEnabled);
		f.setArguments(args);
		return f;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		webvDescription = new WebView(getActivity());
		webvDescription.setHorizontalScrollBarEnabled(scrollbarEnabled);
		return webvDescription;
	}

	@SuppressLint("NewApi")
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (webViewLoader == null && item != null) {
			webViewLoader = createLoader();
			if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
				webViewLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				webViewLoader.execute();
			}
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		if (webViewLoader != null) {
			webViewLoader.cancel(true);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (webViewLoader != null) {
			webViewLoader.cancel(true);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FeedManager manager = FeedManager.getInstance();
		Bundle args = getArguments();
		long feedId = args.getLong(ARG_FEED_ID, -1);
		long itemId = args.getLong(ARG_FEEDITEM_ID, -1);
		scrollbarEnabled = args.getBoolean(ARG_SCROLLBAR_ENABLED, true);
		if (feedId != -1 && itemId != -1) {
			Feed feed = manager.getFeed(feedId);
			item = manager.getFeedItem(itemId, feed);
			webViewLoader = createLoader();
			if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
				webViewLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				webViewLoader.execute();
			}
		} else {
			Log.e(TAG, TAG + " was called with invalid arguments");
		}

	}

	private AsyncTask<Void, Void, Void> createLoader() {
		return new AsyncTask<Void, Void, Void>() {
			@Override
			protected void onCancelled() {
				super.onCancelled();
				if (getSherlockActivity() != null) {
					getSherlockActivity()
							.setSupportProgressBarIndeterminateVisibility(false);
				}
				webViewLoader = null;
			}

			String data;

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				// /webvDescription.loadData(url, "text/html", "utf-8");
				webvDescription.loadDataWithBaseURL(null, data, "text/html",
						"utf-8", "about:blank");
				getSherlockActivity()
						.setSupportProgressBarIndeterminateVisibility(false);
				if (BuildConfig.DEBUG) Log.d(TAG, "Webview loaded");
				webViewLoader = null;
			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				getSherlockActivity()
						.setSupportProgressBarIndeterminateVisibility(true);
			}

			@Override
			protected Void doInBackground(Void... params) {
				if (BuildConfig.DEBUG) Log.d(TAG, "Loading Webview");
				data = "";
				if (item.getContentEncoded() == null
						&& item.getDescription() != null) {
					data = item.getDescription();
				} else {
					data = StringEscapeUtils.unescapeHtml4(item
							.getContentEncoded());
				}

				data = WEBVIEW_STYLE + data;

				return null;
			}

		};
	}
}
