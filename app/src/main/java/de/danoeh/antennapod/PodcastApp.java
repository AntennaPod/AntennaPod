package de.danoeh.antennapod;

import android.app.Application;
import android.os.StrictMode;

import com.google.android.material.color.DynamicColors;

import de.danoeh.antennapod.core.ApCoreEventBusIndex;
import de.danoeh.antennapod.error.CrashReportWriter;
import de.danoeh.antennapod.error.RxJavaErrorHandlerSetup;
import de.danoeh.antennapod.preferences.PreferenceUpgrader;
import de.danoeh.antennapod.spa.SPAUtil;
import org.greenrobot.eventbus.EventBus;

/** Main application class. */
public class PodcastApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
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
