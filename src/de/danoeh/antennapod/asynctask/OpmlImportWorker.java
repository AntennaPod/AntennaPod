package de.danoeh.antennapod.asynctask;

import java.io.*;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParserException;

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
import de.danoeh.antennapod.opml.OpmlReader;

public class OpmlImportWorker extends
		AsyncTask<Void, Void, ArrayList<OpmlElement>> {
	private static final String TAG = "OpmlImportWorker";

	private Context context;
	private Exception exception;

	private ProgressDialog progDialog;

    private Reader mReader;

    public OpmlImportWorker(Context context, Reader reader) {
        super();
        this.context = context;
        this.mReader=reader;
    }

	@Override
	protected ArrayList<OpmlElement> doInBackground(Void... params) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Starting background work");

        if (mReader==null) {
            return null;
        }

		OpmlReader opmlReader = new OpmlReader();
		try {
            ArrayList<OpmlElement> result = opmlReader.readDocument(mReader);
			mReader.close();
			return result;
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			exception = e;
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			exception = e;
			return null;
		}

	}

	@Override
	protected void onPostExecute(ArrayList<OpmlElement> result) {
		progDialog.dismiss();
		if (exception != null) {
			if (AppConfig.DEBUG)
				Log.d(TAG,
						"An error occured while trying to parse the opml document");
			AlertDialog.Builder alert = new AlertDialog.Builder(context);
			alert.setTitle(R.string.error_label);
			alert.setMessage(context.getString(R.string.opml_reader_error)
					+ exception.getMessage());
			alert.setNeutralButton(android.R.string.ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}

			});
			alert.create().show();
		}
	}

	@Override
	protected void onPreExecute() {
		progDialog = new ProgressDialog(context);
		progDialog.setMessage(context.getString(R.string.reading_opml_label));
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
