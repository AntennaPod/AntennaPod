package de.danoeh.antennapod.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.gpoddernet.GpodnetMainActivity;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.util.StorageUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Activity for adding a Feed
 */
public class AddFeedActivity extends ActionBarActivity {
    private static final String TAG = "AddFeedActivity";

    private EditText etxtFeedurl;
    private Button butBrowseMiroGuide;
    private Button butBrowserGpoddernet;
    private Button butOpmlImport;
    private Button butConfirm;

    private ProgressDialog progDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (AppConfig.DEBUG)
            Log.d(TAG, "Was started with Intent " + getIntent().getAction()
                    + " and Data " + getIntent().getDataString());
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        StorageUtils.checkStorageAvailability(this);
        setContentView(R.layout.addfeed);

        progDialog = new ProgressDialog(this);

        etxtFeedurl = (EditText) findViewById(R.id.etxtFeedurl);
        if (StringUtils.equals(getIntent().getAction(), Intent.ACTION_VIEW)) {
            etxtFeedurl.setText(getIntent().getDataString());
        }

        butBrowseMiroGuide = (Button) findViewById(R.id.butBrowseMiroguide);
        butBrowserGpoddernet = (Button) findViewById(R.id.butBrowseGpoddernet);
        butOpmlImport = (Button) findViewById(R.id.butOpmlImport);
        butConfirm = (Button) findViewById(R.id.butConfirm);

        butBrowseMiroGuide.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(AddFeedActivity.this,
                        MiroGuideMainActivity.class));
            }
        });
        butBrowserGpoddernet.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AddFeedActivity.this,
                        GpodnetMainActivity.class));
            }
        });

        butOpmlImport.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(AddFeedActivity.this,
                        OpmlImportFromPathActivity.class));
            }
        });

        butConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddFeedActivity.this, DefaultOnlineFeedViewActivity.class);
                intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, etxtFeedurl.getText().toString());
                intent.putExtra(OnlineFeedViewActivity.ARG_TITLE, getSupportActionBar().getTitle());
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        StorageUtils.checkStorageAvailability(this);
        Intent intent = getIntent();
        if (intent.getAction() != null
                && intent.getAction().equals(Intent.ACTION_SEND)) {
            if (AppConfig.DEBUG)
                Log.d(TAG, "Resuming with ACTION_SEND intent");
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null) {
                etxtFeedurl.setText(text);
            } else {
                if (AppConfig.DEBUG)
                    Log.d(TAG, "No text was sent");
            }
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (AppConfig.DEBUG)
            Log.d(TAG, "Stopping Activity");
    }

    @Override
    protected void onPause() {
        super.onPause();
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

}
