package de.danoeh.antennapod.util;

import org.shredzone.flattr4j.FlattrFactory;
import org.shredzone.flattr4j.FlattrService;
import org.shredzone.flattr4j.oauth.AccessToken;

import android.util.Log;

/** Ensures that only one instance of the FlattrService class exists at a time */
public class FlattrServiceCreator {
	public static final String TAG = "FlattrServiceCreator";
	
	private static FlattrService flattrService;
	
	public static FlattrService getService(AccessToken token) {
		if (flattrService == null) {
			Log.d(TAG, "Creating new instance of Flattr Service");
			FlattrFactory factory = FlattrFactory.getInstance();
			flattrService = factory.createFlattrService(token);
		}
		return flattrService;
	}
}
