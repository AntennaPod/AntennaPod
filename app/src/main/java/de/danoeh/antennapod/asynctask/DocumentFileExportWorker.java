package de.danoeh.antennapod.asynctask;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import de.danoeh.antennapod.core.export.ExportWriter;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.LangUtils;
import io.reactivex.Observable;

/**
 * Writes an OPML file into the export directory in the background.
 */
public class ExportWorker {

    private static final String EXPORT_DIR = "export/";
    private static final String TAG = "ExportWorker";
    private static final String DEFAULT_OUTPUT_NAME = "antennapod-feeds";
    private static final String CONTENT_TYPE = "text/x-opml";

    private final @NonNull ExportWriter exportWriter;
    private final @NonNull File output;
    private DocumentFile alternateOutput;
    private Context context;

    public ExportWorker(@NonNull ExportWriter exportWriter) {
        this(exportWriter, new File(UserPreferences.getDataFolder(EXPORT_DIR),
                DEFAULT_OUTPUT_NAME + "." + exportWriter.fileExtension()));
    }

    public ExportWorker(@NonNull ExportWriter exportWriter, @NonNull Context context, @NonNull Uri uri) {
        this(exportWriter, new File(UserPreferences.getDataFolder(EXPORT_DIR),
                DEFAULT_OUTPUT_NAME + "." + exportWriter.fileExtension()));

        DocumentFile directory = DocumentFile.fromTreeUri(context, uri);
        context.grantUriPermission(context.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        directory.findFile(DEFAULT_OUTPUT_NAME + "." + exportWriter.fileExtension())
        alternateOutput = directory.createFile(CONTENT_TYPE, DEFAULT_OUTPUT_NAME + "." + exportWriter.fileExtension());
        this.context = context;
    }

    private ExportWorker(@NonNull ExportWriter exportWriter, @NonNull File output) {
        this.exportWriter = exportWriter;
        this.output = output;
    }

    public Observable<File> exportObservable() {
        if (alternateOutput != null) {
            return exportDocumentFileObservable();
        } else {
            return exportFileObservable();
        }
    }

    private Observable<File> exportDocumentFileObservable() {
        //if (alternateOutput.exists()) {
        //    Log.w(TAG, "Overwriting previously exported file.");
        //    alternateOutput.delete();
        //}
        return Observable.create(subscriber -> {
            OutputStreamWriter writer = null;
            try {
                writer = new OutputStreamWriter(context.getContentResolver().openOutputStream(alternateOutput.getUri()), LangUtils.UTF_8);
                exportWriter.writeDocument(DBReader.getFeedList(), writer);
                subscriber.onNext(new File(alternateOutput.getUri().toString()));
            } catch (IOException e) {
                subscriber.onError(e);
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        subscriber.onError(e);
                    }
                }
                subscriber.onComplete();
            }
        });
    }

    private Observable<File> exportFileObservable() {
        if (output.exists()) {
            Log.w(TAG, "Overwriting previously exported file.");
            output.delete();
        }
        return Observable.create(subscriber -> {
            OutputStreamWriter writer = null;
            try {
                writer = new OutputStreamWriter(new FileOutputStream(output), LangUtils.UTF_8);
                exportWriter.writeDocument(DBReader.getFeedList(), writer);
                subscriber.onNext(output);
            } catch (IOException e) {
                subscriber.onError(e);
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        subscriber.onError(e);
                    }
                }
                subscriber.onComplete();
            }
        });
    }

}
