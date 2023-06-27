package de.danoeh.antennapod.core;

import android.content.Context;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.core.preferences.UsageStatistics;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.core.service.download.DownloadServiceInterfaceImpl;
import de.danoeh.antennapod.core.sync.SyncService;
import de.danoeh.antennapod.core.sync.queue.SynchronizationQueueSink;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.core.util.download.NetworkConnectionChangeHandler;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;
import de.danoeh.antennapod.net.ssl.SslProviderInstaller;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

import java.io.File;

public class ClientConfigurator {
    private static boolean initialized = false;

    public static synchronized void initialize(Context context) {
        if (initialized) {
            return;
        }
        PodDBAdapter.init(context);
        UserPreferences.init(context);
        UsageStatistics.init(context);
        PlaybackPreferences.init(context);
        SslProviderInstaller.install(context);
        NetworkUtils.init(context);
        NetworkConnectionChangeHandler.init(context);
        DownloadServiceInterface.setImpl(new DownloadServiceInterfaceImpl());
        SynchronizationQueueSink.setServiceStarterImpl(() -> SyncService.sync(context));
        AntennapodHttpClient.setCacheDirectory(new File(context.getCacheDir(), "okhttp"));
        AntennapodHttpClient.setProxyConfig(UserPreferences.getProxyConfig());
        SleepTimerPreferences.init(context);
        NotificationUtils.createChannels(context);
        initialized = true;
    }
}
