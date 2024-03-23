package de.danoeh.antennapod;

import android.app.Application;
import android.os.StrictMode;

import com.google.android.material.color.DynamicColors;

import de.danoeh.antennapod.config.ApplicationCallbacksImpl;
import de.danoeh.antennapod.core.ApCoreEventBusIndex;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.ClientConfigurator;
import de.danoeh.antennapod.error.CrashReportWriter;
import de.danoeh.antennapod.error.RxJavaErrorHandlerSetup;
import de.danoeh.antennapod.preferences.PreferenceUpgrader;
import de.danoeh.antennapod.spa.SPAUtil;
import org.greenrobot.eventbus.EventBus;

/** Main application class. */
public class PodcastApp extends Application {

    private static PodcastApp singleton;

    public static PodcastApp getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ClientConfig.applicationCallbacks = new ApplicationCallbacksImpl();

        Thread.setDefaultUncaughtExceptionHandler(new CrashReportWriter());
        RxJavaErrorHandlerSetup.setupRxJavaErrorHandler();

        if (BuildConfig.DEBUG) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .penaltyDropBox()
                    .detectActivityLeaks()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects();
            StrictMode.setVmPolicy(builder.build());
        }

        singleton = this;

        ClientConfigurator.initialize(this);
        PreferenceUpgrader.checkUpgrades(this);

        SPAUtil.sendSPAppsQueryFeedsIntent(this);
        EventBus.builder()
                .addIndex(new ApEventBusIndex())
                .addIndex(new ApCoreEventBusIndex())
                .logNoSubscriberMessages(false)
                .sendNoSubscriberEvent(false)
                .installDefaultEventBus();

        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
