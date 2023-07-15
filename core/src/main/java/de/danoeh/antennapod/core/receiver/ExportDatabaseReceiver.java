package de.danoeh.antennapod.core.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.IOException;

import de.danoeh.antennapod.core.storage.DatabaseExporter;

public class ExportDatabaseReceiver extends BroadcastReceiver {
    public static final String ACTION_EXPORT_DATABASE = "de.danoeh.antennapod.action.EXPORT_DATABASE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_EXPORT_DATABASE.equals(intent.getAction())) {
            String uriString = intent.getStringExtra(Intent.EXTRA_STREAM);
            if (uriString != null) {
                Uri uri = Uri.parse(uriString);
                try {
                    DatabaseExporter.exportToDocument(uri, context);
                } catch (IOException e) {
                    // Handle any exceptions
                }
            }
        }
    }
}
