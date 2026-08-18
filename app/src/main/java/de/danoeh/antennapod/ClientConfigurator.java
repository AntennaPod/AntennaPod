package de.danoeh.antennapod;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.net.common.BasicAuthorizationInterceptor;
import de.danoeh.antennapod.net.download.service.episode.autodownload.AutoDownloadManagerImpl;
import de.danoeh.antennapod.net.download.service.feed.FeedUpdateManagerImpl;
import de.danoeh.antennapod.net.download.serviceinterface.AutoDownloadManager;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.net.sync.service.SynchronizationQueueImpl;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings;
import de.danoeh.antennapod.storage.preferences.SynchronizationCredentials;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.storage.preferences.UsageStatistics;
import de.danoeh.antennapod.net.common.UserAgentInterceptor;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.net.download.service.feed.DownloadServiceInterfaceImpl;
import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.net.ssl.SslProviderInstaller;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

import de.danoeh.antennapod.ui.notifications.NotificationUtils;
import java.io.File;
import java.util.List;

public class ClientConfigurator {
    private static boolean initialized = false;

    public static synchronized void initialize(Context context) {
        if (initialized) {
            return;
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            UserAgentInterceptor.USER_AGENT = "AntennaPod/" + packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        PodDBAdapter.init(context);
        UserPreferences.init(context);
        SynchronizationCredentials.init(context);
        SynchronizationSettings.init(context);
        UsageStatistics.init(context);
        PlaybackPreferences.init(context);
        SslProviderInstaller.install(context);
        NetworkUtils.init(context);
        DownloadServiceInterface.setImpl(new DownloadServiceInterfaceImpl());
        FeedUpdateManager.setInstance(new FeedUpdateManagerImpl());
        AutoDownloadManager.setInstance(new AutoDownloadManagerImpl());
        SynchronizationQueue.setInstance(new SynchronizationQueueImpl(context));
        AntennapodHttpClient.setCacheDirectory(new File(context.getCacheDir(), "okhttp"));
        AntennapodHttpClient.setProxyConfig(UserPreferences.getProxyConfig());
        BasicAuthorizationInterceptor.setCredentialProvider(
                ClientConfigurator::getCredentialsForUrl);
        SleepTimerPreferences.init(context);
        NotificationUtils.createChannels(context);
        initialized = true;
    }

    private static String getCredentialsForUrl(String url) {
        Uri requestUri = Uri.parse(url);
        String requestHost = requestUri.getHost();
        if (TextUtils.isEmpty(requestHost)) {
            return null;
        }
        List<Feed> feeds = DBReader.getFeedList();
        for (Feed feed : feeds) {
            FeedPreferences prefs = feed.getPreferences();
            if (prefs == null || TextUtils.isEmpty(prefs.getUsername())) {
                continue;
            }
            Uri feedUri = Uri.parse(feed.getDownloadUrl());
            if (requestHost.equals(feedUri.getHost())
                    && requestUri.getPort() == feedUri.getPort()) {
                return prefs.getUsername() + ":" + prefs.getPassword();
            }
        }
        return null;
    }
}
