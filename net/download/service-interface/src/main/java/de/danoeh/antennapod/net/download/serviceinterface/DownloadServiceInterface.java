package de.danoeh.antennapod.net.download.serviceinterface;

import android.content.Context;

public abstract class DownloadServiceInterface {
    private static DownloadServiceInterface impl;

    public static DownloadServiceInterface get() {
        return impl;
    }

    public static void setImpl(DownloadServiceInterface impl) {
        DownloadServiceInterface.impl = impl;
    }

    public abstract void download(Context context, boolean cleanupMedia, DownloadRequest... requests);

    public abstract void refreshAllFeeds(Context context, boolean initiatedByUser);

    public abstract void cancel(Context context, String url);

    public abstract void cancelAll(Context context);
}
