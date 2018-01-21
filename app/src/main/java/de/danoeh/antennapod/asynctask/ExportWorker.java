package de.danoeh.antennapod.asynctask;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import de.danoeh.antennapod.core.export.ExportWriter;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.LangUtils;
import rx.Observable;

/**
 * Writes an OPML file into the export directory in the background.
 */
public class ExportWorker {

    private static final String EXPORT_DIR = "export/";
    private static final String TAG = "ExportWorker";
    private static final String DEFAULT_OUTPUT_NAME = "antennapod-feeds";

    private final ExportWriter exportWriter;
    private final File output;

    public ExportWorker(ExportWriter exportWriter) {
        this(exportWriter, new File(UserPreferences.getDataFolder(EXPORT_DIR),
                DEFAULT_OUTPUT_NAME + "." + exportWriter.fileExtension()));
    }

    private ExportWorker(ExportWriter exportWriter, @NonNull File output) {
        this.exportWriter = exportWriter;
        this.output = output;
    }

    public Observable<File> exportObservable() {
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
                subscriber.onCompleted();
            }
        });
    }

}
