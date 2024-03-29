package de.danoeh.antennapod;

import android.app.Application;
import android.os.StrictMode;

import com.google.android.material.color.DynamicColors;

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

        EventBus.builder()
                .addIndex(new ApEventBusIndex())
                .logNoSubscriberMessages(false)
                .sendNoSubscriberEvent(false)
                .installDefaultEventBus();
        DynamicColors.applyToActivitiesIfAvailable(this);

        ClientConfigurator.initialize(this);
        PreferenceUpgrader.checkUpgrades(this);

        SPAUtil.sendSPAppsQueryFeedsIntent(this);
    }
}
