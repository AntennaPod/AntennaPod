package de.danoeh.antennapod.asynctask;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import de.danoeh.antennapod.core.export.ExportWriter;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.LangUtils;
import io.reactivex.Observable;

/**
 * Writes an OPML file into the user selected export directory in the background.
 */
public class DocumentFileExportWorker {

    private static final String TAG = "ExportWorker";
    private static final String DEFAULT_OUTPUT_NAME = "antennapod-feeds";
    private static final String CONTENT_TYPE = "text/x-opml";

    private final @NonNull ExportWriter exportWriter;
    private @NonNull Context context;
    private @NonNull Uri outputDirectoryUri;

    public DocumentFileExportWorker(@NonNull ExportWriter exportWriter, @NonNull Context context, @NonNull Uri outputDirectoryUri) {
        this.exportWriter = exportWriter;
        this.context = context;
        this.outputDirectoryUri = outputDirectoryUri;
    }

    public Observable<DocumentFile> exportObservable() {
        DocumentFile output = createExportFile();
        return Observable.create(subscriber -> {
            OutputStream outputStream = null;
            OutputStreamWriter writer = null;
            try {
                Uri uri = output.getUri();
                if (uri == null) {
                    throw new FileNotFoundException("Export file not found.");
                }
                outputStream = context.getContentResolver().openOutputStream(uri);
                if (outputStream == null) {
                    throw new IOException();
                }
                writer = new OutputStreamWriter(outputStream, LangUtils.UTF_8);
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
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        subscriber.onError(e);
                    }
                }
                subscriber.onComplete();
            }
        });
    }

    private DocumentFile createExportFile() {
        return DocumentFile.fromSingleUri(context, outputDirectoryUri);
    }

}
