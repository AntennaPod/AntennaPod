package de.danoeh.antennapod.asynctask;

import java.util.List;

import org.shredzone.flattr4j.exception.FlattrException;
import org.shredzone.flattr4j.model.Flattr;

import android.util.Log;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.util.flattr.FlattrUtils;
import de.danoeh.antennapod.storage.DBWriter;

/** Fetch list of flattred things and flattr status in database in a background thread. */

public class FlattrStatusFetcher extends AsyncTask<Void, Void, Void> {
	protected static final String TAG = "FlattrStatusFetcher";
	protected Context context;

	public FlattrStatusFetcher(Context context) {
		super();
		this.context = context;
	}
		
	@Override
	protected Void doInBackground(Void... params) {
		if (AppConfig.DEBUG) Log.d(TAG, "Starting background work: Retrieving Flattr status");
		
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);		
		
		try {			
			List<Flattr> flattredThings = FlattrUtils.retrieveFlattredThings();
			DBWriter.setFlattredStatus(context, flattredThings);
		}
		catch (FlattrException e) {
			Log.d(TAG, "flattrQueue exception retrieving list with flattred items " + e.getMessage());
		} 
		
		if (AppConfig.DEBUG) Log.d(TAG, "Finished background work: Retrieved Flattr status");

		return null;
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
