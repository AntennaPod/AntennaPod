package de.danoeh.antennapod;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.os.StrictMode;

import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;
import com.joanzapata.iconify.fonts.MaterialModule;

import de.danoeh.antennapod.activity.SplashActivity;
import de.danoeh.antennapod.core.ApCoreEventBusIndex;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.error.CrashReportWriter;
import de.danoeh.antennapod.error.RxJavaErrorHandlerSetup;
import de.danoeh.antennapod.spa.SPAUtil;
import org.greenrobot.eventbus.EventBus;

/** Main application class. */
public class PodcastApp extends Application {

    // make sure that ClientConfigurator executes its static code
    static {
        try {
            Class.forName("de.danoeh.antennapod.config.ClientConfigurator");
        } catch (Exception e) {
            throw new RuntimeException("ClientConfigurator not found", e);
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

        ClientConfig.initialize(this);

        Iconify.with(new FontAwesomeModule());
        Iconify.with(new MaterialModule());

        SPAUtil.sendSPAppsQueryFeedsIntent(this);
        EventBus.builder()
                .addIndex(new ApEventBusIndex())
                .addIndex(new ApCoreEventBusIndex())
                .logNoSubscriberMessages(false)
                .sendNoSubscriberEvent(false)
                .installDefaultEventBus();
    }

    public static void forceRestart() {
        Intent intent = new Intent(getInstance(), SplashActivity.class);
        ComponentName cn = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(cn);
        getInstance().startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

}
