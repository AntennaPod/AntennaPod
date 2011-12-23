package de.podfetcher;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.podfetcher.feed.*;
import de.podfetcher.storage.DownloadRequester;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.os.Bundle;

public class PodfetcherActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	setContentView(R.layout.main);
	
	final DownloadRequester requester = DownloadRequester.getInstance();
	final FeedHandler handler = new FeedHandler();
	final FeedManager manager = FeedManager.getInstance();
        
	final Button button = (Button)findViewById(R.id.testbutton);
	final EditText edittext = (EditText)findViewById(R.id.textedit);
        
        
         
	button.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
			final String s = edittext.getText().toString();
			manager.addFeed(v.getContext(), s); 
			edittext.setText("Receiving...");
			
		}
	});
        
    }
}
