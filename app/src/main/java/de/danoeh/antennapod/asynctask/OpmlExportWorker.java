package de.danoeh.antennapod.asynctask;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import de.danoeh.antennapod.core.opml.OpmlWriter;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.LangUtils;
import rx.Observable;

/**
 * Writes an OPML file into the export directory in the background.
 */
public class OpmlExportWorker {

    public static final String EXPORT_DIR = "export/";
    private static final String TAG = "OpmlExportWorker";
    private static final String DEFAULT_OUTPUT_NAME = "antennapod-feeds.opml";
    private File output;

    public OpmlExportWorker() {
        this(new File(UserPreferences.getDataFolder(EXPORT_DIR), DEFAULT_OUTPUT_NAME));
    }

    public OpmlExportWorker(File output) {
        this.output = output;
    }

    public Observable<File> exportObservable() {
        if (output.exists()) {
            Log.w(TAG, "Overwriting previously exported file.");
            output.delete();
        }
        OpmlWriter opmlWriter = new OpmlWriter();
        return Observable.create(subscriber -> {
            OutputStreamWriter writer = null;
            try {
                writer = new OutputStreamWriter(new FileOutputStream(output), LangUtils.UTF_8);
                opmlWriter.writeDocument(DBReader.getFeedList(), writer);
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
