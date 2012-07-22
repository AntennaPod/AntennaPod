package de.danoeh.antennapod.asynctask;

import org.shredzone.flattr4j.exception.FlattrException;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.util.FlattrUtils;

/** Performs a click action in a background thread. */
public class FlattrClickWorker extends AsyncTask<Void, Void, Void> {
	protected static final String TAG = "FlattrClickWorker";
	protected Context context;
	protected String url;
	protected String errorMsg;
	protected int exitCode;
	protected ProgressDialog progDialog;

	protected final static int SUCCESS = 0;
	protected final static int NO_TOKEN = 1;
	protected final static int FLATTR_ERROR = 2;

	public FlattrClickWorker(Context context, String url) {
		super();
		this.context = context;
		this.url = url;
		exitCode = SUCCESS;
		errorMsg = "";
	}

	protected void onNoAccessToken() {
		Log.w(TAG, "No access token was available");
		if (url.equals(FlattrUtils.APP_URL)) {
			FlattrUtils.showNoTokenDialog(context, FlattrUtils.APP_LINK);
		} else {
			FlattrUtils.showNoTokenDialog(context, url);
		}
	}

	protected void onFlattrError() {
		FlattrUtils.showErrorDialog(context, errorMsg);
	}

	protected void onSuccess() {
		Toast toast = Toast.makeText(context.getApplicationContext(),
				R.string.flattr_click_success, Toast.LENGTH_LONG);
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
		if (BuildConfig.DEBUG) Log.d(TAG, "Exit code was " + exitCode);
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

	@Override
	protected Void doInBackground(Void... params) {
		if (BuildConfig.DEBUG) Log.d(TAG, "Starting background work");
		if (FlattrUtils.hasToken()) {
			try {
				FlattrUtils.clickUrl(context, url);
			} catch (FlattrException e) {
				e.printStackTrace();
				exitCode = FLATTR_ERROR;
				errorMsg = e.getMessage();
			}
		} else {
			exitCode = NO_TOKEN;
		}
		return null;
	}

	@SuppressLint("NewApi")
	public void executeAsync() {
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			executeOnExecutor(THREAD_POOL_EXECUTOR);
		} else {
			execute();
		}
	}

}
