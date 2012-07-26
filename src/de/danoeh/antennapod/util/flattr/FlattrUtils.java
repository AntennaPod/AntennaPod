package de.danoeh.antennapod.util.flattr;

import java.util.EnumSet;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;

import de.danoeh.antennapod.R;

/** Utility methods for doing something with flattr. */

public class FlattrUtils {
	private static final String TAG = "FlattrUtils";

	private static final String HOST_NAME = "de.danoeh.antennapod";
	private static final String APP_KEY = "oJ5B1Z90x0gpHbu84F81fWgZWjRpKj4Y";
	private static final String APP_SECRET = "S0qKPpAcfGrNHMLCiKtnwAciJUyj3t8MjomZEN2KkjnSWZk4zEiW4k4D5sNZMLBV";

	private static final String PREF_ACCESS_TOKEN = "de.danoeh.antennapod.preference.flattrAccessToken";

	// Flattr URL for this app.
	public static final String APP_URL = "http://antennapod.com";
	// Human-readable flattr-page.
	public static final String APP_LINK = "https://flattr.com/thing/745609/";
	public static final String APP_THING_ID = "745609";
/*
	private static volatile AccessToken cachedToken;

	private static AndroidAuthenticator createAuthenticator() {
		return new AndroidAuthenticator(HOST_NAME, APP_KEY, APP_SECRET);
	}

	public static void startAuthProcess(Context context) throws FlattrException {
		AndroidAuthenticator auth = createAuthenticator();
		auth.setScope(EnumSet.of(Scope.FLATTR));
		Intent intent = auth.createAuthenticateIntent();
		context.startActivity(intent);
	}

	private static AccessToken retrieveToken() {
		if (cachedToken == null) {
			if (AppConfig.DEBUG) Log.d(TAG, "Retrieving access token");
			String token = PreferenceManager.getDefaultSharedPreferences(
					PodcastApp.getInstance())
					.getString(PREF_ACCESS_TOKEN, null);
			if (token != null) {
				if (AppConfig.DEBUG) Log.d(TAG, "Found access token. Caching.");
				cachedToken = new AccessToken(token);
			} else {
				if (AppConfig.DEBUG) Log.d(TAG, "No access token found");
				return null;
			}
		}
		return cachedToken;

	}

	public static boolean hasToken() {
		return retrieveToken() != null;
	}

	public static void storeToken(AccessToken token) {
		if (AppConfig.DEBUG) Log.d(TAG, "Storing token");
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(PodcastApp.getInstance()).edit();
		if (token != null) {
			editor.putString(PREF_ACCESS_TOKEN, token.getToken());
		} else {
			editor.putString(PREF_ACCESS_TOKEN, null);
		}
		editor.commit();
		cachedToken = token;
	}

	public static void deleteToken() {
		if (AppConfig.DEBUG) Log.d(TAG, "Deleting flattr token");
		storeToken(null);
	}

	public static Thing getAppThing(Context context) {
		FlattrService fs = FlattrServiceCreator.getService(retrieveToken());
		try {
			Thing thing = fs.getThing(Thing.withId(APP_THING_ID));
			return thing;
		} catch (FlattrException e) {
			e.printStackTrace();
			showErrorDialog(context, e.getMessage());
			return null;
		}
	}

	public static void clickUrl(Context context, String url)
			throws FlattrException {
		if (hasToken()) {
			FlattrService fs = FlattrServiceCreator.getService(retrieveToken());
			fs.click(url);
		} else {
			Log.e(TAG, "clickUrl was called with null access token");
		}
	}

	public static void handleCallback(Context context, Uri uri) {
		AndroidAuthenticator auth = createAuthenticator();
		new FlattrTokenFetcher(context, auth, uri).executeAsync();
	}

	public static void revokeAccessToken(Context context) {
		if (AppConfig.DEBUG) Log.d(TAG, "Revoking access token");
		deleteToken();
		FlattrServiceCreator.deleteFlattrService();
		showRevokeDialog(context);
	}

	// ------------------------------------------------ DIALOGS

	public static void showRevokeDialog(final Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.access_revoked_title);
		builder.setMessage(R.string.access_revoked_info);
		builder.setNeutralButton(android.R.string.ok, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.create().show();
	}

	public static void showNoTokenDialog(final Context context, final String url) {
		if (AppConfig.DEBUG) Log.d(TAG, "Creating showNoTokenDialog");
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.no_flattr_token_title);
		builder.setMessage(R.string.no_flattr_token_msg);
		builder.setPositiveButton(R.string.authenticate_now_label,
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						context.startActivity(new Intent(context,
								FlattrAuthActivity.class));
					}

				});
		builder.setNegativeButton(R.string.visit_website_label,
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Uri uri = Uri.parse(url);
						context.startActivity(new Intent(Intent.ACTION_VIEW,
								uri));
					}

				});
		builder.create().show();
	}

	public static void showForbiddenDialog(final Context context,
			final String url) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.action_forbidden_title);
		builder.setMessage(R.string.action_forbidden_msg);
		builder.setPositiveButton(R.string.authenticate_now_label,
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						context.startActivity(new Intent(context,
								FlattrAuthActivity.class));
					}

				});
		builder.setNegativeButton(R.string.visit_website_label,
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Uri uri = Uri.parse(url);
						context.startActivity(new Intent(Intent.ACTION_VIEW,
								uri));
					}

				});
		builder.create().show();
	}

	public static void showErrorDialog(final Context context, final String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.error_label);
		builder.setMessage(msg);
		builder.setNeutralButton(android.R.string.ok, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.create().show();
	}
	*/


}