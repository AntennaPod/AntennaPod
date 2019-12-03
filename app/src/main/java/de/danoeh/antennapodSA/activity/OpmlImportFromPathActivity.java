package de.danoeh.antennapodSA.activity;

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

import de.danoeh.antennapodSA.R;
import de.danoeh.antennapodSA.core.preferences.UserPreferences;
import de.danoeh.antennapodSA.core.util.IntentUtils;
import de.danoeh.antennapodSA.core.util.StorageUtils;

/**
 * Lets the user start the OPML-import process from a path
 */
public class OpmlImportFromPathActivity extends OpmlImportBaseActivity {

    private static final String TAG = "OpmlImportFromPathAct";

    private static final int CHOOSE_OPML_FILE = 1;

    private Intent intentGetContentAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.opml_import);

        final TextView txtvHeaderExplanation = findViewById(R.id.txtvHeadingExplanation);
        final TextView txtvExplanation = findViewById(R.id.txtvExplanation);
        final TextView txtvHeaderExplanationOpenWith = findViewById(R.id.txtvHeadingExplanationOpenWith);

        Button butChooseFilesystem = findViewById(R.id.butChooseFileFromFilesystem);
        butChooseFilesystem.setOnClickListener(v -> chooseFileFromExternal());

        int nextOption = 1;
        String optionLabel = getString(R.string.opml_import_option);
        intentGetContentAction = new Intent(Intent.ACTION_GET_CONTENT);
        intentGetContentAction.addCategory(Intent.CATEGORY_OPENABLE);
        intentGetContentAction.setType("*/*");

        if (IntentUtils.isCallable(getApplicationContext(), intentGetContentAction)) {
            txtvHeaderExplanation.setText(String.format(optionLabel, nextOption));
            nextOption++;
        } else {
            txtvHeaderExplanation.setVisibility(View.GONE);
            txtvExplanation.setVisibility(View.GONE);
            findViewById(R.id.divider).setVisibility(View.GONE);
            butChooseFilesystem.setVisibility(View.GONE);
        }

        txtvHeaderExplanationOpenWith.setText(String.format(optionLabel, nextOption));
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
