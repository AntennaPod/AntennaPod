package de.danoeh.antennapod.activity;

import java.util.ArrayList;

import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.GoogleReaderImportWorker;
import de.danoeh.antennapod.opml.OpmlElement;
import de.danoeh.antennapod.util.googlereader.GoogleReader;

public class GoogleReaderImportActivity extends AbstractImportActivity {
    private static final String TAG = "GoogleReaderImportActivity";
    private ListView lstvGoogleAccount;
    private TextView txtvNoGoogleAccount;
    private Button butStart;
    private GoogleReader greader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(PodcastApp.getThemeResourceId());
        super.onCreate(savedInstanceState);
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.greader_import);
        
        lstvGoogleAccount = (ListView) findViewById(R.id.lstvGoogleAccount);
        butStart = (Button) findViewById(R.id.butStartImport);
        txtvNoGoogleAccount = (TextView) findViewById(R.id.txtvNoGoogleAccount);
        
        greader = new GoogleReader(this);
        
        if(greader.getGoogleAccountNames().length==0) {
            txtvNoGoogleAccount.setVisibility(View.VISIBLE);
            butStart.setEnabled(false);
            return;
        }else {        
            lstvGoogleAccount.setAdapter(new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_single_choice,
                    greader.getGoogleAccountNames()));
            lstvGoogleAccount.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    
            butStart.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int checkPosition = lstvGoogleAccount.getCheckedItemPosition();
                    if(checkPosition == -1) {
                        return;
                    }
                    Log.d(TAG, "getSelectedItemPosition:"+lstvGoogleAccount.getCheckedItemPosition());
                    greader.setSelectedAccount(checkPosition);
                    GoogleReaderImportWorker importWorker = new GoogleReaderImportWorker(GoogleReaderImportActivity.this, greader) {
                        @Override
                        protected void handleResult(ArrayList<OpmlElement> result) {
                            if (result != null) {
                                if (AppConfig.DEBUG)
                                    Log.d(TAG, "Parsing was successful");
                                OpmlImportHolder.setReadElements(result);
                                startActivityForResult(new Intent(
                                        GoogleReaderImportActivity.this,
                                        OpmlFeedChooserActivity.class), 0);
                            } else {
                                if (AppConfig.DEBUG)
                                    Log.d(TAG, "Parser error occurred");
                            }
                        }
                    };
                    importWorker.executeAsync();                
                }
            });
        }
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
