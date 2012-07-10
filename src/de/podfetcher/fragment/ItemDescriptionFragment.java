package de.podfetcher.fragment;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang3.StringEscapeUtils;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockFragment;

import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedItem;
import de.podfetcher.feed.FeedManager;

/** Displays the description of a FeedItem in a Webview. */
public class ItemDescriptionFragment extends SherlockFragment {

	private static final String TAG = "ItemDescriptionFragment";
	private static final String ARG_FEED_ID = "arg.feedId";
	private static final String ARG_FEEDITEM_ID = "arg.feedItemId";

	private WebView webvDescription;
	private FeedItem item;

	private AsyncTask<Void, Void, Void> webViewLoader;

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
		webvDescription = new WebView(getActivity());
		return webvDescription;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (webViewLoader == null && item != null) {
			webViewLoader = createLoader();
			webViewLoader.execute();
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
		if (feedId != -1 && itemId != -1) {
			Feed feed = manager.getFeed(feedId);
			item = manager.getFeedItem(itemId, feed);
			webViewLoader = createLoader();
			webViewLoader.execute();
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

			String url;

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				webvDescription.loadData(url, "text/html", "utf-8");
				getSherlockActivity()
						.setSupportProgressBarIndeterminateVisibility(false);
				Log.d(TAG, "Webview loaded");
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
				Log.d(TAG, "Loading Webview");
				url = "";
				try {
					if (item.getContentEncoded() == null) {
						url = URLEncoder.encode(item.getDescription(), "utf-8")
								.replaceAll("\\+", " ");
					} else {
						url = URLEncoder.encode(
								StringEscapeUtils.unescapeHtml4(item
										.getContentEncoded()), "utf-8")
								.replaceAll("\\+", " ");
					}

				} catch (UnsupportedEncodingException e) {
					url = "Page could not be loaded";
					e.printStackTrace();
				}

				return null;
			}

		};
	}
}
