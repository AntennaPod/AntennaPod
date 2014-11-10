package de.danoeh.antennapod;

import android.app.Application;
import android.content.res.Configuration;

import de.danoeh.antennapod.core.asynctask.PicassoProvider;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.spa.SPAUtil;

/** Main application class. */
public class PodcastApp extends Application {

    // make sure that ClientConfigurator executes its static code
    static {
        try {
            Class.forName("de.danoeh.antennapod.config.ClientConfigurator");
        } catch (Exception e) {
            throw new RuntimeException("ClientConfigurator not found");
        }
    }

	private static final String TAG = "PodcastApp";

	private static float LOGICAL_DENSITY;

	private static PodcastApp singleton;

	public static PodcastApp getInstance() {
		return singleton;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
		LOGICAL_DENSITY = getResources().getDisplayMetrics().density;

        PicassoProvider.setupPicassoInstance(this);
		UserPreferences.createInstance(this);
		PlaybackPreferences.createInstance(this);
		EventDistributor.getInstance();

        SPAUtil.sendSPAppsQueryFeedsIntent(this);
	}

	public static float getLogicalDensity() {
		return LOGICAL_DENSITY;
	}

	public boolean isLargeScreen() {
		return (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE
				|| (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;

	}
}
