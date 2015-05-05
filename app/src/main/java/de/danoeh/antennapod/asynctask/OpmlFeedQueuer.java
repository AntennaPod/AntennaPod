package de.danoeh.antennapod.asynctask;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

import java.util.Arrays;
import java.util.Date;

import de.danoeh.antennapod.activity.OpmlImportHolder;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.opml.OpmlElement;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;

/** Queues items for download in the background. */
public class OpmlFeedQueuer extends IntentService {
	int[] selection;

	public OpmlFeedQueuer() {
		super("OpmlFeedQueuer");
	}

	public void onHandleIntent(Intent intent) {
		this.selection = (int[]) intent.getSerializableExtra("selection");
		DownloadRequester requester = DownloadRequester.getInstance();
		for (int idx = 0; idx < selection.length; idx++) {
			OpmlElement element = OpmlImportHolder.getReadElements().get(
					selection[idx]);
			Feed feed = new Feed(element.getXmlUrl(), new Date(0),
					element.getText());
			try {
				requester.downloadFeed(getApplicationContext(), feed);
			} catch (DownloadRequestException e) {
				e.printStackTrace();
			}
		}
		Intent resultIntent = new Intent(intent.getStringExtra("INTENT_FILTER"));
		LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);
	}

	public static class OpmlFeedQueuerReceiver extends BroadcastReceiver {
		private Context context;
		private ProgressDialog progDialog;
		public OpmlFeedQueuerReceiver(Context context) {
			super();
			this.context = context;
		}

		@Override
		public void onReceive(Context receiverContext, Intent receiverIntent) {
			if (progDialog != null) {
				progDialog.dismiss();
			}
		}

		public void showProgDialog() {
			progDialog = new ProgressDialog(context);
			progDialog.setMessage(context.getString(R.string.processing_label));
			progDialog.setCancelable(false);
			progDialog.setIndeterminate(true);
			progDialog.show();
		}
	}
}
