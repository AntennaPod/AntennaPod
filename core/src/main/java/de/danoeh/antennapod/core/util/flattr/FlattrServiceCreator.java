package de.danoeh.antennapod.core.util.flattr;

import android.util.Log;
import de.danoeh.antennapod.core.BuildConfig;
import org.shredzone.flattr4j.FlattrFactory;
import org.shredzone.flattr4j.FlattrService;
import org.shredzone.flattr4j.oauth.AccessToken;

/** Ensures that only one instance of the FlattrService class exists at a time */

public class FlattrServiceCreator {
	public static final String TAG = "FlattrServiceCreator";
	
	private static volatile FlattrService flattrService;
	
	public static FlattrService getService(AccessToken token) {
		return FlattrFactory.getInstance().createFlattrService(token);
	}
	
	public static void deleteFlattrService() {
		if (BuildConfig.DEBUG) Log.d(TAG, "Deleting service instance");
		flattrService = null;
	}
}

