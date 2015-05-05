package de.danoeh.antennapod.asynctask;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.IntentService;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
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
public class OpmlExportWorker extends IntentService {
    private static final String TAG = "OpmlExportWorker";
    private static final String DEFAULT_OUTPUT_NAME = "antennapod-feeds.opml";
    public static final String EXPORT_DIR = "export/";
    private File output;
    private IOException exception;

    public OpmlExportWorker() {
        super("OpmlExportWorker");
    }

    public void onHandleIntent(Intent intent) {
        this.output = (File) intent.getSerializableExtra("output");
        OpmlWriter opmlWriter = new OpmlWriter();
        if (output == null) {
            output = new File(
                    UserPreferences.getDataFolder(this, EXPORT_DIR),
                    DEFAULT_OUTPUT_NAME);
            if (output.exists()) {
                Log.w(TAG, "Overwriting previously exported file.");
                output.delete();
            }
        }
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(output), LangUtils.UTF_8);
            opmlWriter.writeDocument(DBReader.getFeedList(this), writer);
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
        Intent resultIntent = new Intent(intent.getStringExtra("INTENT_FILTER"));
        resultIntent.putExtra("output", output);
        resultIntent.putExtra("exception", exception);
        LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);
    }

    public static class OpmlExportWorkerReceiver extends BroadcastReceiver {
        private Context context;
        private File output;

        private ProgressDialog progDialog;
        private Exception exception;

        public OpmlExportWorkerReceiver(Context context, File output) {
            this.context = context;
            this.output = output;
        }

        public OpmlExportWorkerReceiver(Context context) {
            this.context = context;
        }

        @Override
        public void onReceive(Context receiverContext, Intent receiverIntent) {
            output = (File) receiverIntent.getSerializableExtra("output");
            exception = (Exception) receiverIntent.getSerializableExtra("exception");
            if (progDialog != null) {
                progDialog.dismiss();
            }
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

        public void showProgDialog() {
            progDialog = new ProgressDialog(context);
            progDialog.setMessage(context.getString(R.string.exporting_label));
            progDialog.setIndeterminate(true);
            progDialog.show();
        }
    }
}
