package de.danoeh.antennapod.asynctask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
	private File file; // path to opml file
	private Exception exception;

	private ProgressDialog progDialog;

	public OpmlImportWorker(Context context, File file) {
		super();
		this.context = context;
		this.file = file;
	}

	@Override
	protected ArrayList<OpmlElement> doInBackground(Void... params) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Starting background work");
		FileReader reader = null;
		// Create reader
		try {
			reader = new FileReader(file);
			if (AppConfig.DEBUG) Log.d(TAG, "Parsing " + file.toString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			exception = e;
			return null;
		}
		OpmlReader opmlReader = new OpmlReader();
		try {
			ArrayList<OpmlElement> result = opmlReader.readDocument(reader);
			reader.close();
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
