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

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.DownloadStatus;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.service.DownloadService;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.ConnectionTester;
import de.danoeh.antennapod.util.DownloadError;
import de.danoeh.antennapod.util.StorageUtils;
import de.danoeh.antennapod.util.URLChecker;

/** Activity for adding/editing a Feed */
public class AddFeedActivity extends SherlockActivity {
	private static final String TAG = "AddFeedActivity";

	private DownloadRequester requester;
	private FeedManager manager;

	private EditText etxtFeedurl;
	private Button butBrowseMiroGuide;
	private Button butConfirm;
	private Button butCancel;
	private long downloadId;

	private boolean hasImage;
	private boolean isWaitingForImage = false;
	private long imageDownloadId;

	private ProgressDialog progDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		StorageUtils.checkStorageAvailability(this);
		setContentView(R.layout.addfeed);

		requester = DownloadRequester.getInstance();
		manager = FeedManager.getInstance();

		progDialog = new ProgressDialog(this) {
			@Override
			public void onBackPressed() {
				if (isWaitingForImage) {
					requester.cancelDownload(getContext(), imageDownloadId);
				} else {
					requester.cancelDownload(getContext(), downloadId);
				}

				try {
					unregisterReceiver(downloadCompleted);
				} catch (IllegalArgumentException e) {
					// ignore
				}
				dismiss();
			}

		};

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
		try {
			unregisterReceiver(downloadCompleted);
		} catch (IllegalArgumentException e) {
			// ignore
		}

	}

	private void addNewFeed() {
		String url = etxtFeedurl.getText().toString();
		url = URLChecker.prepareURL(url);

		if (url != null) {
			final Feed feed = new Feed(url, new Date());
			final ConnectionTester conTester = new ConnectionTester(url, this,
					new ConnectionTester.Callback() {

						@Override
						public void onConnectionSuccessful() {
							downloadId = requester.downloadFeed(
									AddFeedActivity.this, feed);

						}

						@Override
						public void onConnectionFailure() {
							int reason = DownloadError.ERROR_CONNECTION_ERROR;
							long statusId = manager.addDownloadStatus(
									AddFeedActivity.this, new DownloadStatus(
											feed, reason, false));
							Intent intent = new Intent(
									DownloadService.ACTION_DOWNLOAD_HANDLED);
							intent.putExtra(DownloadService.EXTRA_DOWNLOAD_ID,
									downloadId);
							intent.putExtra(DownloadService.EXTRA_STATUS_ID,
									statusId);
							AddFeedActivity.this.sendBroadcast(intent);
						}
					});
			observeDownload(feed);
			new Thread(conTester).start();

		}
	}

	private void observeDownload(Feed feed) {
		progDialog.show();
		progDialog.setMessage("Downloading Feed");
		registerReceiver(downloadCompleted, new IntentFilter(
				DownloadService.ACTION_DOWNLOAD_HANDLED));
	}

	private void updateProgDialog(final String msg) {
		if (progDialog.isShowing()) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					progDialog.setMessage(msg);

				}

			});
		}
	}

	private void handleDownloadError(DownloadStatus status) {
		final AlertDialog errorDialog = new AlertDialog.Builder(this).create();
		errorDialog.setTitle(R.string.error_label);
		errorDialog.setMessage(getString(R.string.error_msg_prefix) + " "
				+ DownloadError.getErrorString(this, status.getReason()));
		errorDialog.setButton("OK", new DialogInterface.OnClickListener() {
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

	private BroadcastReceiver downloadCompleted = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			long receivedDownloadId = intent.getLongExtra(
					DownloadService.EXTRA_DOWNLOAD_ID, -1);
			if (receivedDownloadId == downloadId
					|| (isWaitingForImage && receivedDownloadId == imageDownloadId)) {
				long statusId = intent.getLongExtra(
						DownloadService.EXTRA_STATUS_ID, 0);
				DownloadStatus status = manager.getDownloadStatus(statusId);
				if (status.isSuccessful()) {
					if (!isWaitingForImage) {
						hasImage = intent.getBooleanExtra(
								DownloadService.EXTRA_FEED_HAS_IMAGE, false);
						if (!hasImage) {
							progDialog.dismiss();
							finish();
						} else {
							imageDownloadId = intent
									.getLongExtra(
											DownloadService.EXTRA_IMAGE_DOWNLOAD_ID,
											-1);
							isWaitingForImage = true;
							updateProgDialog("Downloading Image");
						}
					} else {
						progDialog.dismiss();
						finish();
					}
				} else {
					handleDownloadError(status);
				}
			}

		}

	};

}
