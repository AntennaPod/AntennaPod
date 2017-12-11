package de.danoeh.antennapod.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.StorageUtils;

/**
 * Lets the user start the OPML-import process from a path
 */
public class OpmlImportFromPathActivity extends OpmlImportBaseActivity {

    private static final String TAG = "OpmlImportFromPathAct";

    private static final int CHOOSE_OPML_FILE = 1;

    private Intent intentPickAction;
    private Intent intentGetContentAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.opml_import);

        final TextView txtvHeaderExplanation1 = (TextView) findViewById(R.id.txtvHeadingExplanation1);
        final TextView txtvExplanation1 = (TextView) findViewById(R.id.txtvExplanation1);
        final TextView txtvHeaderExplanation2 = (TextView) findViewById(R.id.txtvHeadingExplanation2);
        final TextView txtvExplanation2 = (TextView) findViewById(R.id.txtvExplanation2);
        final TextView txtvHeaderExplanation3 = (TextView) findViewById(R.id.txtvHeadingExplanation3);

        Button butChooseFilesystem = (Button) findViewById(R.id.butChooseFileFromFilesystem);
        butChooseFilesystem.setOnClickListener(v -> chooseFileFromFilesystem());

        Button butChooseExternal = (Button) findViewById(R.id.butChooseFileFromExternal);
        butChooseExternal.setOnClickListener(v -> chooseFileFromExternal());

        int nextOption = 1;
        String optionLabel = getString(R.string.opml_import_option);
        intentPickAction = new Intent(Intent.ACTION_PICK);

        if(!IntentUtils.isCallable(getApplicationContext(), intentPickAction)) {
            intentPickAction.setData(null);
            if(!IntentUtils.isCallable(getApplicationContext(), intentPickAction)) {
                txtvHeaderExplanation1.setVisibility(View.GONE);
                txtvExplanation1.setVisibility(View.GONE);
                findViewById(R.id.divider1).setVisibility(View.GONE);
                butChooseFilesystem.setVisibility(View.GONE);
            }
        }
        if(txtvExplanation1.getVisibility() == View.VISIBLE) {
            txtvHeaderExplanation1.setText(String.format(optionLabel, nextOption));
            nextOption++;
        }

        intentGetContentAction = new Intent(Intent.ACTION_GET_CONTENT);
        intentGetContentAction.addCategory(Intent.CATEGORY_OPENABLE);
        intentGetContentAction.setType("*/*");
        if(!IntentUtils.isCallable(getApplicationContext(), intentGetContentAction)) {
            txtvHeaderExplanation2.setVisibility(View.GONE);
            txtvExplanation2.setVisibility(View.GONE);
            findViewById(R.id.divider2).setVisibility(View.GONE);
            butChooseExternal.setVisibility(View.GONE);
        } else {
            txtvHeaderExplanation2.setText(String.format(optionLabel, nextOption));
            nextOption++;
        }

        txtvHeaderExplanation3.setText(String.format(optionLabel, nextOption));
    }

    @Override
    protected void onResume() {
        super.onResume();
        StorageUtils.checkStorageAvailability(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return false;
        }
    }

    /*
     * Creates an implicit intent to launch a file manager which lets
     * the user choose a specific OPML-file to import from.
     */
    private void chooseFileFromFilesystem() {
        try {
            startActivityForResult(intentPickAction, CHOOSE_OPML_FILE);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No activity found. Should never happen...");
        }
    }

    private void chooseFileFromExternal() {
        try {
            startActivityForResult(intentGetContentAction, CHOOSE_OPML_FILE);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No activity found. Should never happen...");
        }
    }

    /**
      * Gets the path of the file chosen with chooseFileToImport()
      */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == CHOOSE_OPML_FILE) {
            Uri uri = data.getData();
            if(uri != null && uri.toString().startsWith("/")) {
                uri = Uri.parse("file://" + uri.toString());
            }
            importUri(uri);
        }
    }

}
