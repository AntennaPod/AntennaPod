package de.danoeh.antennapod.core.util.flattr;

import android.util.Log;

import org.shredzone.flattr4j.FlattrFactory;
import org.shredzone.flattr4j.FlattrService;
import org.shredzone.flattr4j.oauth.AccessToken;

import de.danoeh.antennapod.core.BuildConfig;

/** Ensures that only one instance of the FlattrService class exists at a time */

class FlattrServiceCreator {
	private static final String TAG = "FlattrServiceCreator";
	
	private static volatile FlattrService flattrService;
	
	public static synchronized FlattrService getService(AccessToken token) {
		if (flattrService == null) {
			flattrService = FlattrFactory.getInstance().createFlattrService(token);
		}
		return flattrService;
	}
	
	public static synchronized void deleteFlattrService() {
		if (BuildConfig.DEBUG) Log.d(TAG, "Deleting service instance");
		flattrService = null;
	}
}

