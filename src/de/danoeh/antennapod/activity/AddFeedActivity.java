package de.danoeh.antennapod.activity;

import java.util.Date;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.DownloadStatus;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.service.download.DownloadService;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.ConnectionTester;
import de.danoeh.antennapod.util.DownloadError;
import de.danoeh.antennapod.util.StorageUtils;
import de.danoeh.antennapod.util.URLChecker;

/** Activity for adding a Feed */
public class AddFeedActivity extends SherlockActivity {
	private static final String TAG = "AddFeedActivity";

	private DownloadRequester requester;

	private EditText etxtFeedurl;
	private Button butBrowseMiroGuide;
	private Button butConfirm;
	private Button butCancel;

	private ProgressDialog progDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		StorageUtils.checkStorageAvailability(this);
		setContentView(R.layout.addfeed);

		requester = DownloadRequester.getInstance();
		progDialog = new ProgressDialog(this);

		etxtFeedurl = (EditText) findViewById(R.id.etxtFeedurl);
		butBrowseMiroGuide = (Button) findViewById(R.id.butBrowseMiroguide);
		butConfirm = (Button) findViewById(R.id.butConfirm);
		butCancel = (Button) findViewById(R.id.butCancel);

		butBrowseMiroGuide.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(AddFeedActivity.this,
						MiroGuideMainActivity.class));
			}
		});

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

	@Override
	protected void onResume() {
		super.onResume();
		StorageUtils.checkStorageAvailability(this);
		Intent intent = getIntent();
		if (intent.getAction() != null
				&& intent.getAction().equals(Intent.ACTION_SEND)) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Was started with ACTION_SEND intent");
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

	/** Read the url text field and start downloading a new feed. */
	private void addNewFeed() {
		String url = etxtFeedurl.getText().toString();
		url = URLChecker.prepareURL(url);

		if (url != null) {
			final Feed feed = new Feed(url, new Date());
			final ConnectionTester conTester = new ConnectionTester(url,
					new ConnectionTester.Callback() {

						@Override
						public void onConnectionSuccessful() {
							requester.downloadFeed(AddFeedActivity.this, feed);
							if (progDialog.isShowing()) {
								progDialog.dismiss();
								finish();
							}
						}

						@Override
						public void onConnectionFailure(int reason) {
							handleDownloadError(reason);
						}
					});
			observeDownload(feed);
			new Thread(conTester).start();

		}
	}

	/** Start listening for any intents send by the DownloadService. */
	private void observeDownload(Feed feed) {
		progDialog.show();
		progDialog.setMessage(getString(R.string.loading_label));
	}

	private void handleDownloadError(int reason) {
		final AlertDialog errorDialog = new AlertDialog.Builder(this).create();
		errorDialog.setTitle(R.string.error_label);
		errorDialog.setMessage(getString(R.string.error_msg_prefix) + " "
				+ DownloadError.getErrorString(this, reason));
		errorDialog.setButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				errorDialog.dismiss();
			}
		});
		if (progDialog.isShowing()) {
			progDialog.dismiss();
		}
		errorDialog.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			return true;
		default:
			return false;
		}
	}

}
