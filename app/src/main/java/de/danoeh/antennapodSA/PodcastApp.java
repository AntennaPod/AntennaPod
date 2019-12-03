package de.danoeh.antennapodSA;

import android.app.Application;
import android.os.StrictMode;

import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;
import com.joanzapata.iconify.fonts.MaterialModule;

import org.greenrobot.eventbus.EventBus;

import de.danoeh.antennapodSA.ApCoreEventBusIndex;
import de.danoeh.antennapodSA.ApEventBusIndex;
import de.danoeh.antennapodSA.BuildConfig;
import de.danoeh.antennapodSA.core.ClientConfig;
import de.danoeh.antennapodSA.spa.SPAUtil;

/** Main application class. */
public class PodcastApp extends Application {

    // make sure that ClientConfigurator executes its static code
    static {
        try {
            Class.forName("de.danoeh.antennapodSA.config.ClientConfigurator");
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

}
