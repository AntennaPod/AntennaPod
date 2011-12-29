package de.podfetcher.activity;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import de.podfetcher.R;
import de.podfetcher.feed.FeedManager;

/** Activity for adding/editing a Feed */
public class AddFeedActivity extends Activity {

	private EditText etxtFeedurl;
	private Button butConfirm;
	private FeedManager manager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addfeed);

		manager = FeedManager.getInstance();
		
		etxtFeedurl = (EditText) findViewById(R.id.etxtFeedurl);
		butConfirm = (Button) findViewById(R.id.butConfirm);

		butConfirm.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addNewFeed();		
				finish();
			}
		});

		
	}

	private void addNewFeed() {
		String url = etxtFeedurl.getText().toString();	
		manager.addFeed(this, url);


	}
	
	
}
