package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import de.danoeh.antennapod.core.preferences.UserPreferences;

/**
 * Lets the user start the OPML-import process.
 */
public class OpmlImportFromIntentActivity extends OpmlImportBaseActivity {

    private static final String TAG = "OpmlImportFromIntentAct";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith("/")) {
            uri = Uri.parse("file://" + uri.toString());
        } else {
            String extraText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            if(extraText != null) {
                uri = Uri.parse(extraText);
            }
        }
        importUri(uri);
    }

    @Override
    protected boolean finishWhenCanceled() {
        return true;
    }

}
