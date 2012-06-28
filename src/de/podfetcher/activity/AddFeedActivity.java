package de.podfetcher.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import de.podfetcher.R;
import de.podfetcher.asynctask.DownloadObserver;
import de.podfetcher.asynctask.DownloadStatus;
import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedManager;
import de.podfetcher.storage.DownloadRequester;
import de.podfetcher.util.ConnectionTester;
import de.podfetcher.util.DownloadError;
import de.podfetcher.util.URLChecker;
import de.podfetcher.service.DownloadService;

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
	private FeedManager manager;

	private EditText etxtFeedurl;
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

				unregisterReceiver(downloadCompleted);
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

	@Override
	protected void onStop() {
		super.onStop();
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
							Intent intent = new Intent(DownloadService.ACTION_DOWNLOAD_HANDLED);
							intent.putExtra(DownloadService.EXTRA_DOWNLOAD_ID, downloadId);
							intent.putExtra(DownloadService.EXTRA_STATUS_ID, statusId);
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
