package de.danoeh.antennapod;

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;

import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;
import com.joanzapata.iconify.fonts.MaterialModule;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.feed.EventDistributor;
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

	private static PodcastApp singleton;

	public static PodcastApp getInstance() {
		return singleton;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Thread.setDefaultUncaughtExceptionHandler(new CrashReportWriter());

		if(BuildConfig.DEBUG) {
			StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder()
				.detectLeakedSqlLiteObjects()
				.penaltyLog()
				.penaltyDropBox();
			builder.detectActivityLeaks();
			builder.detectLeakedClosableObjects();
			if(Build.VERSION.SDK_INT >= 16) {
				builder.detectLeakedRegistrationObjects();
			}
			StrictMode.setVmPolicy(builder.build());
		}

		singleton = this;

		ClientConfig.initialize(this);

		EventDistributor.getInstance();
		Iconify.with(new FontAwesomeModule());
		Iconify.with(new MaterialModule());

        SPAUtil.sendSPAppsQueryFeedsIntent(this);
    }

}
