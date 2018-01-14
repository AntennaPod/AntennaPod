package de.danoeh.antennapod.core.asynctask;


import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.shredzone.flattr4j.exception.FlattrException;
import org.shredzone.flattr4j.oauth.AccessToken;
import org.shredzone.flattr4j.oauth.AndroidAuthenticator;

import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.util.flattr.FlattrUtils;

/**
 * Fetches the access token in the background in order to avoid networkOnMainThread exception.
 */

public class FlattrTokenFetcher extends AsyncTask<Void, Void, AccessToken> {
    private static final String TAG = "FlattrTokenFetcher";
    private final Context context;
    private final AndroidAuthenticator auth;
    private AccessToken token;
    private final Uri uri;
    private ProgressDialog dialog;
    private FlattrException exception;

    public FlattrTokenFetcher(Context context, AndroidAuthenticator auth, Uri uri) {
        super();
        this.context = context;
        this.auth = auth;
        this.uri = uri;
    }

    @Override
    protected void onPostExecute(AccessToken result) {
        if (result != null) {
            FlattrUtils.storeToken(result);
        }
        dialog.dismiss();
        if (exception == null) {
            ClientConfig.flattrCallbacks.handleFlattrAuthenticationSuccess(result);
        } else {
            FlattrUtils.showErrorDialog(context, exception.getMessage());
        }
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = new ProgressDialog(context);
        dialog.setMessage(context.getString(R.string.processing_label));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();
    }


    @Override
    protected AccessToken doInBackground(Void... params) {
        try {
            token = auth.fetchAccessToken(uri);
        } catch (FlattrException e) {
            e.printStackTrace();
            exception = e;
            return null;
        }
        if (token != null) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Successfully got token");
            return token;
        } else {
            Log.w(TAG, "Flattr token was null");
            return null;
        }
    }

    public void executeAsync() {
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

}
