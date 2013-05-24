package de.danoeh.antennapod.asynctask;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.shredzone.flattr4j.exception.FlattrException;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.util.flattr.FlattrThing;
import de.danoeh.antennapod.util.flattr.FlattrUtils;
import de.danoeh.antennapod.util.flattr.FlattrStatus;

/** Performs a click action in a background thread. */

public class FlattrClickWorker extends AsyncTask<Void, String, Void> {
	protected static final String TAG = "FlattrClickWorker";
	protected Context context;
	
	protected String errorMsg;
	protected int exitCode;
	protected int flattred;
	protected ProgressDialog progDialog;

	protected final static int SUCCESS = 0;
	protected final static int NO_TOKEN = 1;
	protected final static int FLATTR_ERROR = 2;
	protected final static int ENQUEUED = 3;

	public FlattrClickWorker(Context context) {
		super();
		this.context = context;
		exitCode = SUCCESS;
		flattred = 0;
		errorMsg = "";
	}

	protected void onNoAccessToken() {
		Log.w(TAG, "No access token was available");
		FlattrUtils.showNoTokenDialog(context, "");
	}

	protected void onFlattrError() {
		FlattrUtils.showErrorDialog(context, errorMsg);
	}
	
	protected void onSuccess() {
		Toast toast;
		
		if (flattred == 0)
			toast = Toast.makeText(context.getApplicationContext(),
					R.string.flattr_click_enqueued, Toast.LENGTH_LONG);
		else if (flattred == 1)
			toast = Toast.makeText(context.getApplicationContext(),
					R.string.flattr_click_success, Toast.LENGTH_LONG);
		else // flattred pending items from queue
			toast = Toast.makeText(context.getApplicationContext(),
				String.format(context.getString(R.string.flattr_click_success_queue), flattred), Toast.LENGTH_LONG);

		toast.show();
	}

	protected void onSetupProgDialog() {
		progDialog = new ProgressDialog(context);
		progDialog.setMessage(context.getString(R.string.flattring_label));
		progDialog.setIndeterminate(true);
		progDialog.setCancelable(false);
		progDialog.show();
	}

	@Override
	protected void onPostExecute(Void result) {
		if (AppConfig.DEBUG) Log.d(TAG, "Exit code was " + exitCode);
		if (progDialog != null) {
			progDialog.dismiss();
		}
		switch (exitCode) {
		case NO_TOKEN:
			onNoAccessToken();
			break;
		case FLATTR_ERROR:
			onFlattrError();
			break;
		case SUCCESS:
			onSuccess();
			break;
		}
	}

	@Override
	protected void onPreExecute() {
		onSetupProgDialog();
	}

	private static boolean haveInternetAccess(Context context) {
		ConnectivityManager cm =
				(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnectedOrConnecting());
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		if (AppConfig.DEBUG) Log.d(TAG, "Starting background work");

		if (haveInternetAccess(context)) {
			if (FlattrUtils.hasToken()) {
				List<FlattrThing> flattrList = FeedManager.getInstance().getFlattrQueue();
				Log.d(TAG, "flattrQueue processing list with " + flattrList.size() + " items.");

				for (FlattrThing thing: flattrList) {
					try {
						Log.d(TAG, "flattrQueue processing " + thing.getTitle() + " " + thing.getPaymentLink());
						publishProgress(thing.getTitle());
						
						thing.getFlattrStatus().setFlattred(); 

						// must propagate this to back db
						if (thing instanceof FeedItem)
							FeedManager.getInstance().setFeedItem(context, (FeedItem) thing);
						else if (thing instanceof Feed)
							FeedManager.getInstance().setFeed(context, (Feed) thing);
						else
							Log.e(TAG, "flattrQueue processing - thing is neither FeedItem nor Feed");
						
						FlattrUtils.clickUrl(context, thing.getPaymentLink());
						flattred++;
					} 
					catch (FlattrException e) {
						Log.d(TAG, "flattrQueue processing exception at item " + thing.getTitle() + " "  + e.getMessage());
						//e.printStackTrace();
						exitCode = FLATTR_ERROR;
						errorMsg = errorMsg + thing.getTitle() + ": " + e.getMessage() + "\n";
					} 
				}
			} else {
				exitCode = NO_TOKEN;
			}
		}

		return null;
	}
	
	@Override
	protected void onProgressUpdate(String... names) {
		progDialog.setMessage(context.getString(R.string.flattring_label) + " " + names[0]);
	}
	
	@SuppressLint("NewApi")
	public void executeAsync() {
		FlattrUtils.hasToken();
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			executeOnExecutor(THREAD_POOL_EXECUTOR);
		} else {
			execute();
		}
	}

}
