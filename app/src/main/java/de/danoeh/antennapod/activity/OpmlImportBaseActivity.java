package de.danoeh.antennapod.activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.asynctask.OpmlFeedQueuer;
import de.danoeh.antennapod.asynctask.OpmlFeedQueuer.OpmlFeedQueuerReceiver;
import de.danoeh.antennapod.asynctask.OpmlImportWorker;
import de.danoeh.antennapod.core.opml.OpmlElement;

import java.io.Reader;
import java.util.ArrayList;

/**
 * Base activity for Opml Import - e.g. with code what to do afterwards
 * */
public class OpmlImportBaseActivity extends ActionBarActivity {

    private static final String OPML_FEED_RECEVIER = "OpmlImportBaseActivity_opmlFeedQueuerReceiver";
    private static final String TAG = "OpmlImportBaseActivity";
    private OpmlImportWorker importWorker;
    private OpmlFeedQueuerReceiver opmlFeedQueuerReceiver;

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		opmlFeedQueuerReceiver = new OpmlFeedQueuerReceiver(this) {
			@Override
			public void onReceive(Context receiverContext, Intent receiverIntent) {
				super.onReceive(receiverContext, receiverIntent);
				Intent intent = new Intent(OpmlImportBaseActivity.this, MainActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
						| Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		};
		LocalBroadcastManager.getInstance(this)
			.registerReceiver(opmlFeedQueuerReceiver, new IntentFilter(OPML_FEED_RECEVIER));
	}

    @Override
    protected void onDestroy() {
		super.onDestroy();
		if (opmlFeedQueuerReceiver != null) {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(opmlFeedQueuerReceiver);
		}
	}

    /**
	 * Handles the choices made by the user in the OpmlFeedChooserActivity and
	 * starts the OpmlFeedQueuer if necessary.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Received result");
		if (resultCode == RESULT_CANCELED) {
			if (BuildConfig.DEBUG)
				Log.d(TAG, "Activity was cancelled");
            if (finishWhenCanceled())
                finish();
		} else {
			int[] selected = data
					.getIntArrayExtra(OpmlFeedChooserActivity.EXTRA_SELECTED_ITEMS);
			if (selected != null && selected.length > 0) {
				Intent opmlFeedQueuer = new Intent(this, OpmlFeedQueuer.class);
				opmlFeedQueuerReceiver.showProgDialog();
				opmlFeedQueuer.putExtra("selection", selected);
				opmlFeedQueuer.putExtra("INTENT_FILTER", OPML_FEED_RECEVIER);
				this.startService(opmlFeedQueuer);
			} else {
				if (BuildConfig.DEBUG)
					Log.d(TAG, "No items were selected");
			}
		}
	}

    /** Starts the import process. */
    protected void startImport(Reader reader) {

        if (reader != null) {
            importWorker = new OpmlImportWorker(this, reader) {

                @Override
                protected void onPostExecute(ArrayList<OpmlElement> result) {
                    super.onPostExecute(result);
                    if (result != null) {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Parsing was successful");
                        OpmlImportHolder.setReadElements(result);
                        startActivityForResult(new Intent(
                                OpmlImportBaseActivity.this,
                                OpmlFeedChooserActivity.class), 0);
                    } else {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Parser error occurred");
                    }
                }
            };
            importWorker.executeAsync();
        }
    }

    protected boolean finishWhenCanceled() {
        return false;
    }


}
