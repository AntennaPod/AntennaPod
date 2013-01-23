package de.danoeh.antennapod.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.OpmlImportWorker;
import de.danoeh.antennapod.opml.OpmlElement;
import de.danoeh.antennapod.util.StorageUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/** Lets the user start the OPML-import process. */
public class OpmlImportFromIntentActivity extends OpmlImportBaseActivity {
	private static final String TAG = "OpmlImportFromPathActivity";

	public static final String IMPORT_DIR = "import/";

	private OpmlImportWorker importWorker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(PodcastApp.getThemeResourceId());
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        try {
            URL mOpmlURL = new URL(getIntent().getData().toString());
            BufferedReader in = new BufferedReader(new InputStreamReader(mOpmlURL.openStream()));
            startImport(in);
        } catch (Exception e) {
            new AlertDialog.Builder(this).setMessage("Cannot open XML - Reason: " + e.getMessage()).show();
        }

    }


	/** Starts the import process. */
	private void startImport(Reader reader) {

		if (reader != null) {
			importWorker = new OpmlImportWorker(this, reader) {

				@Override
				protected void onPostExecute(ArrayList<OpmlElement> result) {
					super.onPostExecute(result);
					if (result != null) {
						if (AppConfig.DEBUG)
							Log.d(TAG, "Parsing was successful");
						OpmlImportHolder.setReadElements(result);
						startActivityForResult(new Intent(
								OpmlImportFromIntentActivity.this,
								OpmlFeedChooserActivity.class), 0);
					} else {
						if (AppConfig.DEBUG)
							Log.d(TAG, "Parser error occured");
					}
				}
			};
			importWorker.executeAsync();
		}
	}

    @Override
    protected boolean finishWhenCanceled() {
        return true;
    }

}
