package de.danoeh.antennapod.asynctask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.shredzone.flattr4j.exception.FlattrException;
import org.shredzone.flattr4j.model.Flattr;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.FeedItemlistActivity;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.util.flattr.FlattrThing;
import de.danoeh.antennapod.util.flattr.FlattrUtils;
import de.danoeh.antennapod.util.flattr.FlattrStatus;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.DBWriter;

/** Performs a click action in a background thread. */

public class FlattrClickWorker extends AsyncTask<Void, String, Void> {
	protected static final String TAG = "FlattrClickWorker";
	protected Context context;
	
	private final int NOTIFICATION_ID = 4;
	
	protected String errorMsg;
	protected int exitCode;
	protected ArrayList<String> flattrd;
	protected ArrayList<String> flattr_failed;
	
	protected NotificationCompat.Builder notificationBuilder;
	protected NotificationManager notificationManager;
	
	protected ProgressDialog progDialog;

	protected final static int EXIT_DEFAULT = 0;
	protected final static int NO_TOKEN = 1;
	protected final static int ENQUEUED = 2;
	protected final static int NO_THINGS = 3;
	
	private boolean enqueue_only = false;

	public FlattrClickWorker(Context context, boolean enqueue_only) {
		this(context);
		this.enqueue_only = enqueue_only;
	}
	
	public FlattrClickWorker(Context context) {
		super();
		this.context = context;
		exitCode = EXIT_DEFAULT;

		flattrd = new ArrayList<String>();
		flattr_failed = new ArrayList<String>();
		
		errorMsg = "";
	}

    /* only used in PreferencesActivity for flattring antennapod itself,
    * can't really enqueue this thing
    */
    public FlattrClickWorker(Context context, String url) {
        Log.e(TAG, "Not implemented yet");
    }

	protected void onNoAccessToken() {
		Log.w(TAG, "No access token was available");
		FlattrUtils.showNoTokenDialog(context, "");
	}

	protected void onFlattrError() {
		FlattrUtils.showErrorDialog(context, errorMsg);
	}
	
	protected void onFlattred() {
		String notificationTitle = context.getString(R.string.flattrd_label);
		String notificationText = "", notificationSubText = "";
		
		// text for successfully flattred items
		if (flattrd.size() == 1)
			notificationText = String.format(context.getString(R.string.flattr_click_success));
		else if (flattrd.size() > 1) // flattred pending items from queue
			notificationText = String.format(context.getString(R.string.flattr_click_success_count, flattrd.size()));
			
		if (flattrd.size() > 0) {
			String acc = "";
			for (String s: flattrd)
				acc += s + ", ";
			acc = acc.substring(0, acc.length()-2);
			
			notificationSubText = String.format(context.getString(R.string.flattr_click_success_queue), acc);
		}
		
		// add text for failures
		if (flattr_failed.size() > 0) {
			notificationTitle = context.getString(R.string.flattrd_failed_label);
			notificationText = String.format(context.getString(R.string.flattr_click_failure_count), flattr_failed.size()) 
					+ " " + notificationText;
			
			String acc = "";
			for (String s: flattr_failed)
				acc += s + ", ";
			acc = acc.substring(0, acc.length()-2);
			
			notificationSubText = String.format(context.getString(R.string.flattr_click_failure), acc)
					+ " " + notificationSubText;
		}
		
		notificationBuilder = new NotificationCompat.Builder(context) // need new notificationBuilder and cancel/renotify to get rid of progress bar
							.setContentTitle(notificationTitle)
							.setContentText(notificationText)
							.setSubText(notificationSubText)
							.setTicker(notificationTitle)
							.setSmallIcon(R.drawable.stat_notify_sync)
							.setOngoing(false);
		notificationManager.cancel(NOTIFICATION_ID);
		notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
	}
	
	protected void onEnqueue() {
		Toast.makeText(context.getApplicationContext(), 
									 R.string.flattr_click_enqueued, 
									 Toast.LENGTH_LONG)
							.show();
	}

	protected void onSetupNotification() {
		notificationBuilder = new NotificationCompat.Builder(context)
							.setContentTitle(context.getString(R.string.flattring_label))
                            .setAutoCancel(true)
							.setSmallIcon(R.drawable.stat_notify_sync)
							.setProgress(0,  0, true)
                            .setOngoing(true);
		
		notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	protected void onPostExecute(Void result) {
		if (AppConfig.DEBUG) Log.d(TAG, "Exit code was " + exitCode);

		switch (exitCode) {
		case NO_TOKEN:
			notificationManager.cancel(NOTIFICATION_ID);
			onNoAccessToken();
			break;
		case ENQUEUED:
			onEnqueue();
			break;
		case EXIT_DEFAULT:
			onFlattred();
			break;
		case NO_THINGS: // FlattrClickWorker called automatically somewhere to empty flattr queue
			notificationManager.cancel(NOTIFICATION_ID);
			break;
		}
	}

	@Override
	protected void onPreExecute() {
		onSetupNotification();
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

		exitCode = EXIT_DEFAULT;

		if (!FlattrUtils.hasToken()) {
			exitCode = NO_TOKEN;
		}
		else if (DBReader.getFlattrQueueEmpty(context)) {
			exitCode = NO_THINGS;
		}
		else if (!haveInternetAccess(context) || enqueue_only) {
			exitCode = ENQUEUED;
		}
		else {
			List<FlattrThing> flattrList = DBReader.getFlattrQueue(context);
			Log.d(TAG, "flattrQueue processing list with " + flattrList.size() + " items.");

			flattrd.ensureCapacity(flattrList.size());
			
			for (FlattrThing thing: flattrList) {
				try {
					Log.d(TAG, "flattrQueue processing " + thing.getTitle() + " " + thing.getPaymentLink());
					publishProgress(String.format(context.getString(R.string.flattring_thing), thing.getTitle()));

					thing.getFlattrStatus().setUnflattred();  // pop from queue to prevent unflattrable things from getting stuck in flattr queue infinitely

					FlattrUtils.clickUrl(context, thing.getPaymentLink());
					flattrd.add(thing.getTitle());
					
					thing.getFlattrStatus().setFlattred();
				}
				catch (FlattrException e) {
					Log.d(TAG, "flattrQueue processing exception at item " + thing.getTitle() + " "  + e.getMessage());
					flattr_failed.ensureCapacity(flattrList.size());
					flattr_failed.add(thing.getTitle());
				}
                Log.d(TAG, "flattrQueue processing - going to write thing back to db with flattr_status " + Long.toString(thing.getFlattrStatus().toLong()));
                DBWriter.setFlattredStatus(context, thing);
			}
			
		}
		
		return null;
	}
	
	@Override
	protected void onProgressUpdate(String... names) {
		notificationBuilder.setContentText(names[0]);
		notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
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
