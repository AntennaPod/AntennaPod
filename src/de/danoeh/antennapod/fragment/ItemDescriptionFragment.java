package de.danoeh.antennapod.fragment;

import org.apache.commons.lang3.StringEscapeUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockFragment;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;

/** Displays the description of a FeedItem in a Webview. */
public class ItemDescriptionFragment extends SherlockFragment {

	private static final String TAG = "ItemDescriptionFragment";
	private static final String ARG_FEED_ID = "arg.feedId";
	private static final String ARG_FEEDITEM_ID = "arg.feedItemId";

	private WebView webvDescription;
	private FeedItem item;

	private AsyncTask<Void, Void, Void> webViewLoader;

	private String descriptionRef;
	private String contentEncodedRef;

	public static ItemDescriptionFragment newInstance(FeedItem item) {
		ItemDescriptionFragment f = new ItemDescriptionFragment();
		Bundle args = new Bundle();
		args.putLong(ARG_FEED_ID, item.getFeed().getId());
		args.putLong(ARG_FEEDITEM_ID, item.getId());
		f.setArguments(args);
		return f;
	}

	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Creating view");
		webvDescription = new WebView(getActivity());

		if (PodcastApp.getThemeResourceId() == R.style.Theme_AntennaPod_Dark) {
			if (Build.VERSION.SDK_INT >= 11
					&& Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
				webvDescription.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
			}
			webvDescription.setBackgroundColor(0);
		}
		webvDescription.getSettings().setUseWideViewPort(false);
		return webvDescription;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (AppConfig.DEBUG)
			Log.d(TAG, "Fragment attached");
	}

	@Override
	public void onDetach() {
		super.onDetach();
		if (AppConfig.DEBUG)
			Log.d(TAG, "Fragment detached");
		if (webViewLoader != null) {
			webViewLoader.cancel(true);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (AppConfig.DEBUG)
			Log.d(TAG, "Fragment destroyed");
		if (webViewLoader != null) {
			webViewLoader.cancel(true);
		}
	}

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (AppConfig.DEBUG)
			Log.d(TAG, "Creating fragment");
		FeedManager manager = FeedManager.getInstance();
		Bundle args = getArguments();
		long feedId = args.getLong(ARG_FEED_ID, -1);
		long itemId = args.getLong(ARG_FEEDITEM_ID, -1);
		if (feedId != -1 && itemId != -1) {
			Feed feed = manager.getFeed(feedId);
			item = manager.getFeedItem(itemId, feed);

		} else {
			Log.e(TAG, TAG + " was called with invalid arguments");
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (item != null) {
			if (item.getDescription() == null
					|| item.getContentEncoded() == null) {
				Log.i(TAG, "Loading data");
				FeedManager.getInstance().loadExtraInformationOfItem(
						getActivity(), item,
						new FeedManager.TaskCallback<String[]>() {
							@Override
							public void onCompletion(String[] result) {
								if (result == null || result.length != 2) {
									Log.e(TAG, "No description found");
								} else {
									descriptionRef = result[0];
									contentEncodedRef = result[1];
								}

								startLoader();
							}
						});
			} else {
				contentEncodedRef = item.getContentEncoded();
				descriptionRef = item.getDescription();
				if (AppConfig.DEBUG)
					Log.d(TAG, "Using cached data");
				startLoader();
			}
		} else {
			Log.e(TAG, "Error in onViewCreated: Item was null");
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

	/**
	 * Return the CSS style of the Webview.
	 * 
	 * @param textColor
	 *            the default color to use for the text in the webview. This
	 *            value is inserted directly into the CSS String.
	 * */
	private String getWebViewStyle(String textColor) {
		final String WEBVIEW_STYLE = "<head><style type=\"text/css\"> * { color: %s; font-family: Helvetica; line-height: 1.5em; font-size: 12pt; } a { font-style: normal; text-decoration: none; font-weight: normal; color: #00A8DF; }</style></head>";
		return String.format(WEBVIEW_STYLE, textColor);
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
				if (getSherlockActivity() != null) {
					getSherlockActivity()
							.setSupportProgressBarIndeterminateVisibility(false);
				}
				if (AppConfig.DEBUG)
					Log.d(TAG, "Webview loaded");
				webViewLoader = null;
			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				if (getSherlockActivity() != null) {
					getSherlockActivity()
							.setSupportProgressBarIndeterminateVisibility(true);
				}
			}

			@Override
			protected Void doInBackground(Void... params) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Loading Webview");
				data = "";
				if (contentEncodedRef == null && descriptionRef != null) {
					data = descriptionRef;
				} else {
					data = StringEscapeUtils.unescapeHtml4(contentEncodedRef);
				}
				Activity activity = getActivity();
				if (activity != null) {
					TypedArray res = getActivity()
							.getTheme()
							.obtainStyledAttributes(
									new int[] { android.R.attr.textColorPrimary });
					int colorResource = res.getColor(0, 0);
					String colorString = String.format("#%06X",
							0xFFFFFF & colorResource);
					Log.i(TAG, "text color: " + colorString);
					res.recycle();
					data = getWebViewStyle(colorString) + data;
				} else {
					cancel(true);
				}
				return null;
			}

		};
	}
}
