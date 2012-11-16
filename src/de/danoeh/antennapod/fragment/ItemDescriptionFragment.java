package de.danoeh.antennapod.fragment;

import org.apache.commons.lang3.StringEscapeUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockFragment;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;

/** Displays the description of a FeedItem in a Webview. */
public class ItemDescriptionFragment extends SherlockFragment {

	private static final String TAG = "ItemDescriptionFragment";
	private static final String ARG_FEED_ID = "arg.feedId";
	private static final String ARG_FEEDITEM_ID = "arg.feedItemId";

	private static final String WEBVIEW_STYLE = "<head><style type=\"text/css\"> * { font-family: Helvetica; line-height: 1.5em; font-size: 12pt; } a { font-style: normal; text-decoration: none; font-weight: normal; color: #00A8DF; }</style></head>";

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

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Creating view");
		webvDescription = new WebView(getActivity());
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
		setRetainInstance(true);
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
					&& item.getContentEncoded() == null) {
				Log.i(TAG, "Loading data");
				FeedManager.getInstance().loadExtraInformationOfItem(
						getActivity(), item, new FeedManager.TaskCallback() {
							@Override
							public void onCompletion(Cursor result) {
								if (item.getDescription() == null
										&& item.getContentEncoded() == null) {
									Log.e(TAG, "No description found");
								}
								startLoader();
							}
						});
			} else {
				Log.i(TAG, "Using cached data");
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
		contentEncodedRef = item.getContentEncoded();
		descriptionRef = item.getDescription();
		webViewLoader = createLoader();
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			webViewLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			webViewLoader.execute();
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
				if (contentEncodedRef == null
						&& descriptionRef != null) {
					data = descriptionRef;
				} else {
					data = StringEscapeUtils.unescapeHtml4(contentEncodedRef);
				}

				data = WEBVIEW_STYLE + data;
				return null;
			}

		};
	}
}
