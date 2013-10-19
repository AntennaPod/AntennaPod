package de.danoeh.antennapod.activity;

import java.util.Date;

import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import de.danoeh.antennapod.activity.gpoddernet.GpodnetMainActivity;
import org.apache.commons.lang3.StringUtils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.ConnectionTester;
import de.danoeh.antennapod.util.DownloadError;
import de.danoeh.antennapod.util.StorageUtils;
import de.danoeh.antennapod.util.URLChecker;

/** Activity for adding a Feed */
public class AddFeedActivity extends ActionBarActivity {
	private static final String TAG = "AddFeedActivity";

	private DownloadRequester requester;

	private EditText etxtFeedurl;
	private Button butBrowseMiroGuide;
    private Button butBrowserGpoddernet;
	private Button butOpmlImport;
	private Button butConfirm;
	private Button butCancel;

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

		requester = DownloadRequester.getInstance();
		progDialog = new ProgressDialog(this);

		etxtFeedurl = (EditText) findViewById(R.id.etxtFeedurl);
		if (StringUtils.equals(getIntent().getAction(), Intent.ACTION_VIEW)) {
			etxtFeedurl.setText(getIntent().getDataString());
		}

		butBrowseMiroGuide = (Button) findViewById(R.id.butBrowseMiroguide);
        butBrowserGpoddernet = (Button) findViewById(R.id.butBrowseGpoddernet);
		butOpmlImport = (Button) findViewById(R.id.butOpmlImport);
		butConfirm = (Button) findViewById(R.id.butConfirm);
		butCancel = (Button) findViewById(R.id.butCancel);

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
							try {
								requester.downloadFeed(AddFeedActivity.this,
										feed);
								if (progDialog.isShowing()) {
									progDialog.dismiss();
									finish();
								}
							} catch (DownloadRequestException e) {
								e.printStackTrace();
								onConnectionFailure(DownloadError.ERROR_REQUEST_ERROR);
							}

						}

						@Override
						public void onConnectionFailure(DownloadError reason) {
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

	private void handleDownloadError(DownloadError reason) {
		final AlertDialog errorDialog = new AlertDialog.Builder(this).create();
		errorDialog.setTitle(R.string.error_label);
		errorDialog.setMessage(getString(R.string.error_msg_prefix) + " "
				+ reason.getErrorString(this));
		errorDialog.setButton(getString(android.R.string.ok),
				new DialogInterface.OnClickListener() {
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
