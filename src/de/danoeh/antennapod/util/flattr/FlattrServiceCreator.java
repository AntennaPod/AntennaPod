package de.danoeh.antennapod.util.flattr;

import org.shredzone.flattr4j.FlattrFactory;
import org.shredzone.flattr4j.FlattrService;
import org.shredzone.flattr4j.oauth.AccessToken;

import android.util.Log;
import de.danoeh.antennapod.AppConfig;

/** Ensures that only one instance of the FlattrService class exists at a time */

public class FlattrServiceCreator {
	public static final String TAG = "FlattrServiceCreator";

	private static volatile FlattrService flattrService;

	public static FlattrService getService(AccessToken token) {
		return FlattrFactory.getInstance().createFlattrService(token);
	}

	public static void deleteFlattrService() {
		if (AppConfig.DEBUG) Log.d(TAG, "Deleting service instance");
		flattrService = null;
	}
}

