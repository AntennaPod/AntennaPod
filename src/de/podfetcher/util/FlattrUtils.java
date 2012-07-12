package de.podfetcher.util;

import java.util.EnumSet;

import org.shredzone.flattr4j.FlattrFactory;
import org.shredzone.flattr4j.FlattrService;
import org.shredzone.flattr4j.exception.FlattrException;
import org.shredzone.flattr4j.exception.ForbiddenException;
import org.shredzone.flattr4j.oauth.AccessToken;
import org.shredzone.flattr4j.oauth.AndroidAuthenticator;
import org.shredzone.flattr4j.oauth.Scope;

import de.podfetcher.PodcastApp;
import de.podfetcher.R;
import de.podfetcher.activity.FlattrAuthActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

/** Utility methods for doing something with flattr. */
public class FlattrUtils {
	private static final String TAG = "FlattrUtils";

	private static final String HOST_NAME = "";
	private static final String APP_KEY = "";
	private static final String APP_SECRET = "";

	private static final String PREF_ACCESS_TOKEN = "de.danoeh.antennapod.preference.flattrAccessToken";

	private static AndroidAuthenticator createAuthenticator() {
		return new AndroidAuthenticator(HOST_NAME, APP_KEY, APP_SECRET);
	}

	public static void startAuthProcess(Context context) throws FlattrException {
		AndroidAuthenticator auth = createAuthenticator();
		auth.setScope(EnumSet.of(Scope.FLATTR));
		Intent intent = auth.createAuthenticateIntent();
		context.startActivity(intent);
	}

	/**
	 * Returns the access token from the preferences or null if no access token
	 * was saved before.
	 */
	public static AccessToken retrieveToken() {
		Log.d(TAG, "Retrieving access token");
		String token = PreferenceManager.getDefaultSharedPreferences(
				PodcastApp.getInstance()).getString(PREF_ACCESS_TOKEN, null);
		if (token != null) {
			Log.d(TAG, "Found access token");
			return new AccessToken(token);
		} else {
			Log.d(TAG, "No access token found");
			return null;
		}
	}

	/** Returns true if the application has saved an access token */
	public static boolean hasToken() {
		return retrieveToken() != null;
	}

	/** Stores the token as a preference */
	private static void storeToken(AccessToken token) {
		Log.d(TAG, "Storing token");
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(PodcastApp.getInstance()).edit();
		if (token != null) {
			editor.putString(PREF_ACCESS_TOKEN, token.getToken());
		} else {
			editor.putString(PREF_ACCESS_TOKEN, null);
		}
		editor.commit();
	}

	public static void deleteToken() {
		Log.d(TAG, "Deleting flattr token");
		storeToken(null);
	}

	public static void clickUrl(Context context, String url) {
		FlattrFactory factory = FlattrFactory.getInstance();
		AccessToken token = retrieveToken();
		if (token != null) {
			FlattrService fs = factory.createFlattrService(retrieveToken());
			try {
				fs.click(url);
			} catch (ForbiddenException fe) {
				deleteToken();
				showForbiddenDialog(context, url);
			} catch (FlattrException e) {
				e.printStackTrace();
			}
		} else {
			showNoTokenDialog(context, url);
		}
	}

	public static AccessToken handleCallback(Uri uri) throws FlattrException {
		AndroidAuthenticator auth = createAuthenticator();
		AccessToken token = auth.fetchAccessToken(uri);
		if (token != null) {
			Log.d(TAG, "Successfully got token");
			storeToken(token);
			return token;
		} else {
			Log.w(TAG, "Flattr token was null");
			return null;
		}
	}

	private static void showNoTokenDialog(final Context context,
			final String url) {
		Log.d(TAG, "Creating showNoTokenDialog");
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

	private static void showForbiddenDialog(final Context context,
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
}
