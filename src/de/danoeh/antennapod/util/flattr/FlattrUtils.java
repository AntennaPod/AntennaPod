package de.danoeh.antennapod.util.flattr;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;
import java.util.TimeZone;

import org.shredzone.flattr4j.FlattrService;
import org.shredzone.flattr4j.exception.FlattrException;
import org.shredzone.flattr4j.model.Flattr;
import org.shredzone.flattr4j.model.Thing;
import org.shredzone.flattr4j.oauth.AccessToken;
import org.shredzone.flattr4j.oauth.AndroidAuthenticator;
import org.shredzone.flattr4j.oauth.Scope;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.FlattrAuthActivity;
import de.danoeh.antennapod.asynctask.FlattrTokenFetcher;
import de.danoeh.antennapod.storage.DBWriter;

/** Utility methods for doing something with flattr. */

public class FlattrUtils {
	private static final String TAG = "FlattrUtils";

	private static final String HOST_NAME = "de.danoeh.antennapod";

	private static final String PREF_ACCESS_TOKEN = "de.danoeh.antennapod.preference.flattrAccessToken";

	// Flattr URL for this app.
	public static final String APP_URL = "http://antennapod.com";
	// Human-readable flattr-page.
	public static final String APP_LINK = "https://flattr.com/thing/745609/";
	public static final String APP_THING_ID = "745609";

	private static volatile AccessToken cachedToken;

	private static AndroidAuthenticator createAuthenticator() {
		return new AndroidAuthenticator(HOST_NAME, FlattrConfig.APP_KEY,
				FlattrConfig.APP_SECRET);
	}

	public static void startAuthProcess(Context context) throws FlattrException {
		AndroidAuthenticator auth = createAuthenticator();
		auth.setScope(EnumSet.of(Scope.FLATTR));
		Intent intent = auth.createAuthenticateIntent();
		context.startActivity(intent);
	}

	private static AccessToken retrieveToken() {
		if (cachedToken == null) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Retrieving access token");
			String token = PreferenceManager.getDefaultSharedPreferences(
					PodcastApp.getInstance())
					.getString(PREF_ACCESS_TOKEN, null);
			if (token != null) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Found access token. Caching.");
				cachedToken = new AccessToken(token);
			} else {
				if (AppConfig.DEBUG)
					Log.d(TAG, "No access token found");
				return null;
			}
		}
		return cachedToken;

	}

	public static boolean hasToken() {
		return retrieveToken() != null;
	}

	public static void storeToken(AccessToken token) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Storing token");
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
		if (AppConfig.DEBUG)
			Log.d(TAG, "Deleting flattr token");
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
	
	public static List<Flattr> retrieveFlattredThings() 
		throws FlattrException {
		ArrayList<Flattr> myFlattrs = new ArrayList<Flattr>();

		if (hasToken()) {
			FlattrService fs = FlattrServiceCreator.getService(retrieveToken());

			Calendar firstOfMonth = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			firstOfMonth.set(Calendar.MILLISECOND, 0);
			firstOfMonth.set(Calendar.SECOND, 0);
			firstOfMonth.set(Calendar.MINUTE, 0);
			firstOfMonth.set(Calendar.HOUR_OF_DAY, 0);
			firstOfMonth.set(Calendar.DAY_OF_MONTH, Calendar.getInstance().getActualMinimum(Calendar.DAY_OF_MONTH));
			
			Date firstOfMonthDate = firstOfMonth.getTime();

			// subscriptions some times get flattrd slightly before midnight - give it an hour leeway
			firstOfMonthDate = new Date(firstOfMonthDate.getTime() - 60*60*1000);
			
			final int FLATTR_COUNT = 30;
			final int FLATTR_MAXPAGE = 5;
			
			int page = 0;
			do {
				myFlattrs.ensureCapacity(FLATTR_COUNT*(page+1));
				
				for (Flattr fl: fs.getMyFlattrs(FLATTR_COUNT, page)) {
					if (fl.getCreated().after(firstOfMonthDate))
						myFlattrs.add(fl);
					else
						break;
				}
				page++;
			}
			while (myFlattrs.get(myFlattrs.size()-1).getCreated().after( firstOfMonthDate ) && page < FLATTR_MAXPAGE);
			
			if (AppConfig.DEBUG) {
				Log.d(TAG, "Got my flattrs list of length " + Integer.toString(myFlattrs.size()) + " comparison date" + firstOfMonthDate);

				for (Flattr fl: myFlattrs) {
					Thing thing = fl.getThing();
					Log.d(TAG, "Flattr thing: " + fl.getThingId() + " name: " + thing.getTitle() +  " url: " + thing.getUrl() + " on: " + fl.getCreated());
				}
			}
			
		} else {
			Log.e(TAG, "retrieveFlattrdThings was called with null access token");
		}
		
		return myFlattrs;
	}

	public static void handleCallback(Context context, Uri uri) {
		AndroidAuthenticator auth = createAuthenticator();
		new FlattrTokenFetcher(context, auth, uri).executeAsync();
	}

	public static void revokeAccessToken(Context context) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Revoking access token");
		deleteToken();
		FlattrServiceCreator.deleteFlattrService();
		showRevokeDialog(context);
        DBWriter.clearAllFlattrStatus(context);
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
		if (AppConfig.DEBUG)
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

}