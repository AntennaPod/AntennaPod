package de.danoeh.antennapod.asynctask;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.activity.OpmlImportHolder;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.opml.OpmlElement;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;

import java.util.Arrays;
import java.util.Date;

/** Queues items for download in the background. */
public class OpmlFeedQueuer extends AsyncTask<Void, Void, Void> {
	private Context context;
	private ProgressDialog progDialog;
	private int[] selection;

	public OpmlFeedQueuer(Context context, int[] selection) {
		super();
		this.context = context;
		this.selection = Arrays.copyOf(selection, selection.length);
	}

	@Override
	protected void onPostExecute(Void result) {
		progDialog.dismiss();
	}

	@Override
	protected void onPreExecute() {
		progDialog = new ProgressDialog(context);
		progDialog.setMessage(context.getString(R.string.processing_label));
		progDialog.setCancelable(false);
		progDialog.setIndeterminate(true);
		progDialog.show();
	}

	@Override
	protected Void doInBackground(Void... params) {
		DownloadRequester requester = DownloadRequester.getInstance();
		for (int idx = 0; idx < selection.length; idx++) {
			OpmlElement element = OpmlImportHolder.getReadElements().get(
					selection[idx]);
			Feed feed = new Feed(element.getXmlUrl(), new Date(),
					element.getText());
			try {
				requester.downloadFeed(context.getApplicationContext(), feed);
			} catch (DownloadRequestException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@SuppressLint("NewApi")
	public void executeAsync() {
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			executeOnExecutor(THREAD_POOL_EXECUTOR);
		} else {
			execute();
		}
	}

}
