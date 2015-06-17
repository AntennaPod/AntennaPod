package de.danoeh.antennapod.activity;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.LangUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

/** Lets the user start the OPML-import process. */
public class OpmlImportFromIntentActivity extends OpmlImportBaseActivity {

    private static final String TAG = "OpmlImportFromIntentAct";


    @Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(UserPreferences.getTheme());
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        try {
            Uri uri = getIntent().getData();

            Reader mReader = new InputStreamReader(getContentResolver().openInputStream(uri), LangUtils.UTF_8);
            startImport(mReader);
        } catch (Exception e) {
            new AlertDialog.Builder(this).setMessage("Cannot open XML - Reason: " + e.getMessage()).show();
        }

    }

    @Override
    protected boolean finishWhenCanceled() {
        return true;
    }

}
