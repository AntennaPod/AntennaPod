package de.danoeh.antennapod.core.asynctask;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import java.util.concurrent.ExecutionException;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.storage.DBWriter;

/** Removes a feed in the background. */
public class FeedRemover extends AsyncTask<Void, Void, Void> {
	Context context;
	ProgressDialog dialog;
	Feed feed;
	public boolean skipOnCompletion = false;

	public FeedRemover(Context context, Feed feed) {
		super();
		this.context = context;
		this.feed = feed;
	}

	@Override
	protected Void doInBackground(Void... params) {
        try {
            DBWriter.deleteFeed(context, feed.getId()).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		dialog.dismiss();
		if(skipOnCompletion) {
			context.sendBroadcast(new Intent(
					PlaybackService.ACTION_SKIP_CURRENT_EPISODE));
		}
	}

	@Override
	protected void onPreExecute() {
		dialog = new ProgressDialog(context);
		dialog.setMessage(context.getString(R.string.feed_remover_msg));
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);
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
