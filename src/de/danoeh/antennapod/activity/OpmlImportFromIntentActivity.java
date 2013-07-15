package de.danoeh.antennapod.activity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import android.app.AlertDialog;
import android.os.Bundle;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.util.LangUtils;

/** Lets the user start the OPML-import process. */
public class OpmlImportFromIntentActivity extends OpmlImportBaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(UserPreferences.getTheme());
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        try {
            URL mOpmlURL = new URL(getIntent().getData().toString());
            BufferedReader in = new BufferedReader(new InputStreamReader(mOpmlURL.openStream(),
                LangUtils.UTF_8));
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
