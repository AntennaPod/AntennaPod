package de.podfetcher.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import de.podfetcher.R;
import de.podfetcher.asynctask.DownloadObserver;
import de.podfetcher.asynctask.DownloadStatus;
import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedManager;
import de.podfetcher.storage.DownloadRequester;
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
			Feed feed = new Feed(url, new Date());
			downloadId = requester.downloadFeed(this, feed);
			observeDownload(feed);
		}
	}

	private void observeDownload(Feed feed) {
		progDialog.show();
		progDialog.setMessage("Downloading Feed");
		registerReceiver(downloadCompleted, new IntentFilter(DownloadService.ACTION_DOWNLOAD_HANDLED));
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
