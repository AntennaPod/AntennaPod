package de.danoeh.antennapod.asynctask;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.opml.OpmlElement;

public abstract class AbstractImportWorker extends
        AsyncTask<Void, Void, ArrayList<OpmlElement>> {

    protected final String TAG = "BaseImportWorker";
    protected Context context;
    
    public AbstractImportWorker(Context context) {
        super();
        this.context = context;
    }

    protected Exception exception;
    private ProgressDialog progDialog;

    @Override
    protected ArrayList<OpmlElement> doInBackground(Void... params) {
    	if (AppConfig.DEBUG)
    		Log.d(TAG, "Starting background work"); 
        try {
            ArrayList<OpmlElement> feeds = work();
            return feeds;
        } catch (Exception e) {
            this.exception = e;
            return null;
        }
    }

    abstract protected ArrayList<OpmlElement> work() throws Exception;

    @Override
    protected void onPostExecute(ArrayList<OpmlElement> result) {
    	progDialog.dismiss();
    	if (exception != null) {
    		if (AppConfig.DEBUG)
    			Log.d(TAG,
    					"An error occured while trying to parse the opml document");
    		AlertDialog.Builder alert = new AlertDialog.Builder(context);
    		alert.setTitle(R.string.error_label);
    		alert.setMessage(getAlertMessage(exception));
    		alert.setNeutralButton(android.R.string.ok, new OnClickListener() {
    
    			@Override
    			public void onClick(DialogInterface dialog, int which) {
    				dialog.dismiss();
    			}
    
    		});
    		alert.create().show();
    	}
    	handleResult(result);
    }

    protected abstract void handleResult(ArrayList<OpmlElement> result);

    protected abstract String getAlertMessage(Exception e);
    
    protected abstract String getInProgressMessage();

    @Override
    protected void onPreExecute() {
    	progDialog = new ProgressDialog(context);
    	progDialog.setMessage(getInProgressMessage());
    	progDialog.setIndeterminate(true);
    	progDialog.setCancelable(false);
    	progDialog.show();
    }

    public boolean wasSuccessful() {
    	return exception != null;
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