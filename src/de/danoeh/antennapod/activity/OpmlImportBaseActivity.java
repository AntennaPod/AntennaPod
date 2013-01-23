package de.danoeh.antennapod.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.OpmlFeedQueuer;
import de.danoeh.antennapod.asynctask.OpmlImportWorker;
import de.danoeh.antennapod.opml.OpmlElement;
import de.danoeh.antennapod.util.StorageUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * Base activity for Opml Import - e.g. with code what to do afterwards
 * */
public class OpmlImportBaseActivity extends SherlockActivity {

    private static final String TAG = "OpmlImportBaseActivity";

	/**
	 * Handles the choices made by the user in the OpmlFeedChooserActivity and
	 * starts the OpmlFeedQueuer if necessary.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Received result");
		if (resultCode == RESULT_CANCELED) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Activity was cancelled");
            if (finishWhenCanceled())
                finish();
		} else {
			int[] selected = data
					.getIntArrayExtra(OpmlFeedChooserActivity.EXTRA_SELECTED_ITEMS);
			if (selected != null && selected.length > 0) {
				OpmlFeedQueuer queuer = new OpmlFeedQueuer(this, selected) {

					@Override
					protected void onPostExecute(Void result) {
						super.onPostExecute(result);
						Intent intent = new Intent(OpmlImportBaseActivity.this, MainActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
								| Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
					}

				};
				queuer.executeAsync();
			} else {
				if (AppConfig.DEBUG)
					Log.d(TAG, "No items were selected");
			}
		}
	}

    protected boolean finishWhenCanceled() {
        return false;
    }


}
