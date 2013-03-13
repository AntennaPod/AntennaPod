package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.util.Log;

import com.actionbarsherlock.app.SherlockActivity;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.asynctask.OpmlFeedQueuer;

public abstract class AbstractImportActivity extends SherlockActivity {

    private static final String TAG = "AbstractImportActivity";

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
                        Intent intent = new Intent(AbstractImportActivity.this, MainActivity.class);
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
