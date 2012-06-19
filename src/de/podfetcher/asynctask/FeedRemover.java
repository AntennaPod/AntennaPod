package de.podfetcher.asynctask;

import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;

/** Removes a feed in the background. */
public class FeedRemover extends AsyncTask<Feed, Void, Void> {
	Context context;
	ProgressDialog dialog;
	
	public FeedRemover(Context context) {
		super();
		this.context = context;
	}

	@Override
	protected Void doInBackground(Feed... params) {
		FeedManager manager = FeedManager.getInstance();
		for (Feed feed : params) {
			manager.deleteFeed(context, feed);
			if (isCancelled()) {
				break;
			}
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
		dialog.setMessage("Removing Feed");
		dialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				cancel(true);
				
			}
			
		});
		dialog.show();
	}
	
	

}
