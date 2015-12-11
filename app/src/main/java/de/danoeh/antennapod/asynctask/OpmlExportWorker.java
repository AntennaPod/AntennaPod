package de.danoeh.antennapod.asynctask;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.opml.OpmlWriter;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.LangUtils;

/**
 * Writes an OPML file into the export directory in the background.
 */
public class OpmlExportWorker extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "OpmlExportWorker";
    private static final String DEFAULT_OUTPUT_NAME = "antennapod-feeds.opml";
    public static final String EXPORT_DIR = "export/";

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
                    UserPreferences.getDataFolder(EXPORT_DIR),
                    DEFAULT_OUTPUT_NAME);
            if (output.exists()) {
                Log.w(TAG, "Overwriting previously exported file.");
                output.delete();
            }
        }
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(output), LangUtils.UTF_8);
            opmlWriter.writeDocument(DBReader.getFeedList(), writer);
        } catch (IOException e) {
            e.printStackTrace();
            exception = e;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ioe) {
                    exception = ioe;
                }
            }
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
                    + output.toString())
                    .setPositiveButton(R.string.send_label, (dialog, which) -> {
                        Uri outputUri = Uri.fromFile(output);
                        Intent sendIntent = new Intent(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_SUBJECT,
                                context.getResources().getText(R.string.opml_export_label));
                        sendIntent.putExtra(Intent.EXTRA_STREAM, outputUri);
                        sendIntent.setType("text/plain");
                        context.startActivity(Intent.createChooser(sendIntent,
                                context.getResources().getText(R.string.send_label)));
                    });
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
