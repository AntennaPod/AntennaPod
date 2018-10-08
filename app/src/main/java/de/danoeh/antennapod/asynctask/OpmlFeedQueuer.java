package de.danoeh.antennapod.asynctask;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;

import de.danoeh.antennapod.activity.OpmlImportHolder;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.export.opml.OpmlElement;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;

/** Queues items for download in the background. */
public class OpmlFeedQueuer extends AsyncTask<Void, Void, Void> {
	private final Context context;
	private ProgressDialog progDialog;
	@NonNull
    private final int[] selection;

	public OpmlFeedQueuer(Context context, @NonNull int[] selection) {
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

	@Nullable
    @Override
	protected Void doInBackground(Void... params) {
		DownloadRequester requester = DownloadRequester.getInstance();
		for (int selected : selection) {
			OpmlElement element = OpmlImportHolder.getReadElements().get(selected);
			Feed feed = new Feed(element.getXmlUrl(), null,
					element.getText());
			try {
				requester.downloadFeed(context.getApplicationContext(), feed);
			} catch (DownloadRequestException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public void executeAsync() {
		executeOnExecutor(THREAD_POOL_EXECUTOR);
	}

}
