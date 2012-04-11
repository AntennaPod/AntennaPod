package de.podfetcher.activity;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.podfetcher.R;
import de.podfetcher.feed.*;
import de.podfetcher.storage.DownloadRequester;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.os.Bundle;
import android.content.Intent;
import com.actionbarsherlock.app.SherlockListActivity;

    

public class PodfetcherActivity extends SherlockListActivity {
    private static final String TAG = "PodfetcherActivity";
    
    private final String[] ITEMS = {"Feeds", "Settings"};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
		// Add navigation menu
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, ITEMS)); 
    }

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final String selection = (String) l.getAdapter().getItem(position);
		
		if(selection.equals(ITEMS[0])) {
		    Intent intent = new Intent(PodfetcherActivity.this, FeedlistActivity.class);
		    startActivity(intent);
		} else if(selection.equals(ITEMS[1])){
		    
		}
	}
}
