package de.danoeh.antennapod.asynctask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.opml.OpmlWriter;
import de.danoeh.antennapod.preferences.UserPreferences;

/** Writes an OPML file into the export directory in the background. */
public class OpmlExportWorker extends AsyncTask<Void, Void, Void> {
	private static final String TAG = "OpmlExportWorker";
	private static final String DEFAULT_OUTPUT_NAME = "antennapod-feeds.opml";
	private Context context;
	private File output;

	private ProgressDialog progDialog;
	private Exception exception;

	public OpmlExportWorker(Context context, File output) {
		this.context = context;
		this.output = output;
	}

	public OpmlExportWorker(Context context) {
		this.context = context;
	}

	@Override
	protected Void doInBackground(Void... params) {
		OpmlWriter opmlWriter = new OpmlWriter();
		if (output == null) {
			output = new File(
					UserPreferences.getDataFolder(context, PodcastApp.EXPORT_DIR),
					DEFAULT_OUTPUT_NAME);
			if (output.exists()) {
				Log.w(TAG, "Overwriting previously exported file.");
				output.delete();
			}
		}
		try {
			FileWriter writer = new FileWriter(output);
			opmlWriter.writeDocument(Arrays.asList(FeedManager.getInstance().getFeedsArray()),
					writer);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			exception = e;
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		progDialog.dismiss();
		AlertDialog.Builder alert = new AlertDialog.Builder(context)
				.setNeutralButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
		if (exception != null) {
			alert.setTitle(R.string.export_error_label);
			alert.setMessage(exception.getMessage());
		} else {
			alert.setTitle(R.string.opml_export_success_title);
			alert.setMessage(context
					.getString(R.string.opml_export_success_sum)
					+ output.toString());
		}
		alert.create().show();
	}

	@Override
	protected void onPreExecute() {
		progDialog = new ProgressDialog(context);
		progDialog.setMessage(context.getString(R.string.exporting_label));
		progDialog.setIndeterminate(true);
		progDialog.show();
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
