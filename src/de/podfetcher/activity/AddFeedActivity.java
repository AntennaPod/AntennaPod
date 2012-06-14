package de.podfetcher.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.app.ProgressDialog;
import android.util.Log;
import de.podfetcher.R;
import de.podfetcher.feed.Feed;
import de.podfetcher.storage.DownloadRequester;
import de.podfetcher.util.URLChecker;
import de.podfetcher.service.DownloadObserver;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import java.util.Date;
import java.util.concurrent.Callable;

/** Activity for adding/editing a Feed */
public class AddFeedActivity extends SherlockActivity {
	private static final String TAG = "AddFeedActivity";

	private DownloadRequester requester;
	
	private EditText etxtFeedurl;
	private Button butConfirm;
	private Button butCancel;
	private long downloadId;

	
	private DownloadObserver observer;
	
	private  ProgressDialog progDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addfeed);

		requester = DownloadRequester.getInstance();
		
		createObserver();
		progDialog = new ProgressDialog(this) {
			@Override
			public void onBackPressed() {
				requester.cancelDownload(getContext(), downloadId);
				observer.cancel(true);
				createObserver();
				dismiss();
			}
			
		};
		
		etxtFeedurl = (EditText) findViewById(R.id.etxtFeedurl);
		butConfirm = (Button) findViewById(R.id.butConfirm);
		butCancel = (Button) findViewById(R.id.butCancel);

		butConfirm.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addNewFeed();		
			}
		});

		butCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
	}
	
	private void createObserver() {
		observer = new DownloadObserver(this) {
			@Override
			protected void onPostExecute(Boolean result) {
				progDialog.dismiss();
				finish();
			}

			@Override
			protected void onProgressUpdate(DownloadObserver.DownloadStatus... values) {
				DownloadObserver.DownloadStatus progr = values[0];
				progDialog.setMessage(getContext().getString(progr.getStatusMsg())
						+ " (" + progr.getProgressPercent() + "%)");
			}
		};
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "Stopping Activity");
		observer.cancel(true);
	}

	private void addNewFeed() {
		String url = etxtFeedurl.getText().toString();	
		url = URLChecker.prepareURL(url);

		if(url != null) {
			Feed feed = new Feed(url, new Date());
			downloadId = requester.downloadFeed(this, feed);
			observeDownload(feed);
		}
	}

	private void observeDownload(Feed feed) {
		progDialog.show();
		observer.execute(feed);
	}

}
