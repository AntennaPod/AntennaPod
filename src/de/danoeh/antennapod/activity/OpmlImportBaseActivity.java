package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.util.Log;
import com.actionbarsherlock.app.SherlockActivity;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.asynctask.OpmlFeedQueuer;
import de.danoeh.antennapod.asynctask.OpmlImportWorker;
import de.danoeh.antennapod.opml.OpmlElement;
import java.io.Reader;
import java.util.ArrayList;

/**
 * Base activity for Opml Import - e.g. with code what to do afterwards
 * */
public class OpmlImportBaseActivity extends AbstractImportActivity {

    private static final String TAG = "OpmlImportBaseActivity";
    private OpmlImportWorker importWorker;


    /** Starts the import process. */
    protected void startImport(Reader reader) {

        if (reader != null) {
            importWorker = new OpmlImportWorker(this, reader) {
                @Override
                protected void handleResult(ArrayList<OpmlElement> result) {
                    if (result != null) {
                        if (AppConfig.DEBUG)
                            Log.d(TAG, "Parsing was successful");
                        OpmlImportHolder.setReadElements(result);
                        startActivityForResult(new Intent(
                                OpmlImportBaseActivity.this,
                                OpmlFeedChooserActivity.class), 0);
                    } else {
                        if (AppConfig.DEBUG)
                            Log.d(TAG, "Parser error occurred");
                    }
                }
            };
            importWorker.executeAsync();
        }
    }
}
