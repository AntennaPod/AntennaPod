package de.danoeh.antennapod.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import de.danoeh.antennapod.PodcastApp;
import java.io.*;
import java.net.URL;

/** Lets the user start the OPML-import process. */
public class OpmlImportFromIntentActivity extends OpmlImportBaseActivity {

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

    @Override
    protected boolean finishWhenCanceled() {
        return true;
    }

}
