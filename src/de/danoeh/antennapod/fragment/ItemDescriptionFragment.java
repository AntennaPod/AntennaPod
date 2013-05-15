package de.danoeh.antennapod.fragment;

import org.apache.commons.lang3.StringEscapeUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Picture;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebView.PictureListener;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.util.ShareUtils;
import de.danoeh.antennapod.util.playback.Playable;

/** Displays the description of a Playable object in a Webview. */
public class ItemDescriptionFragment extends SherlockFragment {

	private static final String TAG = "ItemDescriptionFragment";

	private static final String PREF = "ItemDescriptionFragmentPrefs";
	private static final String PREF_SCROLL_Y = "prefScrollY";
	private static final String PREF_PLAYABLE_ID = "prefPlayableId";

	private static final String ARG_PLAYABLE = "arg.playable";

	private static final String ARG_FEED_ID = "arg.feedId";
	private static final String ARG_FEED_ITEM_ID = "arg.feeditemId";
	private static final String ARG_SAVE_STATE = "arg.saveState";

	private WebView webvDescription;
	private Playable media;

	private FeedItem item;

	private AsyncTask<Void, Void, Void> webViewLoader;

	private String shownotes;

	/** URL that was selected via long-press. */
	private String selectedURL;

	/**
	 * True if Fragment should save its state (e.g. scrolling position) in a
	 * shared preference.
	 */
	private boolean saveState;

	public static ItemDescriptionFragment newInstance(Playable media,
			boolean saveState) {
		ItemDescriptionFragment f = new ItemDescriptionFragment();
		Bundle args = new Bundle();
		args.putParcelable(ARG_PLAYABLE, media);
		args.putBoolean(ARG_SAVE_STATE, saveState);
		f.setArguments(args);
		return f;
	}

	public static ItemDescriptionFragment newInstance(FeedItem item,
			boolean saveState) {
		ItemDescriptionFragment f = new ItemDescriptionFragment();
		Bundle args = new Bundle();
		args.putLong(ARG_FEED_ID, item.getFeed().getId());
		args.putLong(ARG_FEED_ITEM_ID, item.getId());
		args.putBoolean(ARG_SAVE_STATE, saveState);
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

		if (UserPreferences.getTheme() == R.style.Theme_AntennaPod_Dark) {
			if (Build.VERSION.SDK_INT >= 11
					&& Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
				webvDescription.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
			}
			webvDescription.setBackgroundColor(getResources().getColor(
					R.color.black));
		}
		webvDescription.getSettings().setUseWideViewPort(false);
		webvDescription.getSettings().setLayoutAlgorithm(
				LayoutAlgorithm.NARROW_COLUMNS);
		webvDescription.getSettings().setLoadWithOverviewMode(true);
		webvDescription.setOnLongClickListener(webViewLongClickListener);
		registerForContextMenu(webvDescription);
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
		Bundle args = getArguments();
		saveState = args.getBoolean(ARG_SAVE_STATE, false);
		if (args.containsKey(ARG_PLAYABLE)) {
			media = args.getParcelable(ARG_PLAYABLE);
		} else if (args.containsKey(ARG_FEED_ID)
				&& args.containsKey(ARG_FEED_ITEM_ID)) {
			long feedId = args.getLong(ARG_FEED_ID);
			long itemId = args.getLong(ARG_FEED_ITEM_ID);
			FeedItem f = FeedManager.getInstance().getFeedItem(itemId, feedId);
			if (f != null) {
				item = f;
			}
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (media != null) {
			media.loadShownotes(new Playable.ShownoteLoaderCallback() {

				@Override
				public void onShownotesLoaded(String shownotes) {
					ItemDescriptionFragment.this.shownotes = shownotes;
					if (ItemDescriptionFragment.this.shownotes != null) {
						startLoader();
					}
				}
			});
		} else if (item != null) {
			if (item.getDescription() == null
					|| item.getContentEncoded() == null) {
				FeedManager.getInstance().loadExtraInformationOfItem(
						PodcastApp.getInstance(), item,
						new FeedManager.TaskCallback<String[]>() {
							@Override
							public void onCompletion(String[] result) {
								if (result[1] != null) {
									shownotes = result[1];
								} else {
									shownotes = result[0];
								}
								if (shownotes != null) {
									startLoader();
								}

							}
						});
			} else {
				shownotes = item.getContentEncoded();
				startLoader();
			}
		} else {
			Log.e(TAG, "Error in onViewCreated: Item and media were null");
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
	private String applyWebviewStyle(String textColor, String data) {
		final String WEBVIEW_STYLE = "<html><head><style type=\"text/css\"> * { color: %s; font-family: Helvetica; line-height: 1.5em; font-size: 11pt; } a { font-style: normal; text-decoration: none; font-weight: normal; color: #00A8DF; } img { display: block; margin: 10 auto; max-width: %s; height: auto; } body { margin: %dpx %dpx %dpx %dpx; }</style></head><body>%s</body></html>";
		final int pageMargin = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 8, getResources()
						.getDisplayMetrics());
		return String.format(WEBVIEW_STYLE, textColor, "100%", pageMargin,
				pageMargin, pageMargin, pageMargin, data);
	}

	private View.OnLongClickListener webViewLongClickListener = new View.OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			WebView.HitTestResult r = webvDescription.getHitTestResult();
			if (r != null
					&& r.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
				if (AppConfig.DEBUG)
					Log.d(TAG,
							"Link of webview was long-pressed. Extra: "
									+ r.getExtra());
				selectedURL = r.getExtra();
				webvDescription.showContextMenu();
				return true;
			}
			selectedURL = null;
			return false;
		}
	};

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		boolean handled = selectedURL != null;
		if (selectedURL != null) {
			switch (item.getItemId()) {
			case R.id.open_in_browser_item:
				Uri uri = Uri.parse(selectedURL);
				getActivity()
						.startActivity(new Intent(Intent.ACTION_VIEW, uri));
				break;
			case R.id.share_url_item:
				ShareUtils.shareLink(getActivity(), selectedURL);
				break;
			case R.id.copy_url_item:
				if (android.os.Build.VERSION.SDK_INT >= 11) {
					ClipData clipData = ClipData.newPlainText(selectedURL,
							selectedURL);
					android.content.ClipboardManager cm = (android.content.ClipboardManager) getActivity()
							.getSystemService(Context.CLIPBOARD_SERVICE);
					cm.setPrimaryClip(clipData);
				} else {
					android.text.ClipboardManager cm = (android.text.ClipboardManager) getActivity()
							.getSystemService(Context.CLIPBOARD_SERVICE);
					cm.setText(selectedURL);
				}
				Toast t = Toast.makeText(getActivity(),
						R.string.copied_url_msg, Toast.LENGTH_SHORT);
				t.show();
				break;
			default:
				handled = false;
				break;

			}
			selectedURL = null;
		}
		return handled;

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if (selectedURL != null) {
			super.onCreateContextMenu(menu, v, menuInfo);
			menu.add(Menu.NONE, R.id.open_in_browser_item, Menu.NONE,
					R.string.open_in_browser_label);
			menu.add(Menu.NONE, R.id.copy_url_item, Menu.NONE,
					R.string.copy_url_label);
			menu.add(Menu.NONE, R.id.share_url_item, Menu.NONE,
					R.string.share_url_label);
			menu.setHeaderTitle(selectedURL);
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

			@SuppressWarnings("deprecation")
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
				webvDescription.setPictureListener(new PictureListener() {

					@Override
					@Deprecated
					public void onNewPicture(WebView view, Picture picture) {
						restoreFromPreference();

					}
				});

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
				data = StringEscapeUtils.unescapeHtml4(shownotes);
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
					data = applyWebviewStyle(colorString, data);
				} else {
					cancel(true);
				}
				return null;
			}

		};
	}

	@Override
	public void onPause() {
		super.onPause();
		savePreference();
	}

	private void savePreference() {
		if (saveState) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Saving preferences");
			SharedPreferences prefs = getActivity().getSharedPreferences(PREF,
					Activity.MODE_PRIVATE);
			SharedPreferences.Editor editor = prefs.edit();
			if (media != null && webvDescription != null) {
				if (AppConfig.DEBUG)
					Log.d(TAG,
							"Saving scroll position: "
									+ webvDescription.getScrollY());
				editor.putInt(PREF_SCROLL_Y, webvDescription.getScrollY());
				editor.putString(PREF_PLAYABLE_ID, media.getIdentifier()
						.toString());
			} else {
				if (AppConfig.DEBUG)
					Log.d(TAG,
							"savePreferences was called while media or webview was null");
				editor.putInt(PREF_SCROLL_Y, -1);
				editor.putString(PREF_PLAYABLE_ID, "");
			}
			editor.commit();
		}
	}

	private boolean restoreFromPreference() {
		if (saveState) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Restoring from preferences");
			SharedPreferences prefs = getActivity().getSharedPreferences(PREF,
					Activity.MODE_PRIVATE);
			String id = prefs.getString(PREF_PLAYABLE_ID, "");
			int scrollY = prefs.getInt(PREF_SCROLL_Y, -1);
			if (scrollY != -1 && media != null
					&& id.equals(media.getIdentifier().toString())
					&& webvDescription != null) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Restored scroll Position: " + scrollY);
				webvDescription.scrollTo(webvDescription.getScrollX(), scrollY);
				return true;
			}
		}
		return false;
	}
}
