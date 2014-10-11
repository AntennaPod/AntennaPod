package de.danoeh.antennapod.core.asynctask;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.storage.DBWriter;

import java.util.concurrent.ExecutionException;

/** Removes a feed in the background. */
public class FeedRemover extends AsyncTask<Void, Void, Void> {
	Context context;
	ProgressDialog dialog;
	Feed feed;

	public FeedRemover(Context context, Feed feed) {
		super();
		this.context = context;
		this.feed = feed;
	}

	@Override
	protected Void doInBackground(Void... params) {
        try {
            DBWriter.deleteFeed(context, feed.getId()).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
	}

	@Override
	protected void onCancelled() {
		dialog.dismiss();
	}

	@Override
	protected void onPostExecute(Void result) {
		dialog.dismiss();
	}

	@Override
	protected void onPreExecute() {
		dialog = new ProgressDialog(context);
		dialog.setMessage(context.getString(R.string.feed_remover_msg));
		dialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				cancel(true);

			}

		});
		dialog.show();
	}

	@SuppressLint("NewApi")
	public void executeAsync() {
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			execute();
		}
	}

}
