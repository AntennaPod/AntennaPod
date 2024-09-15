package de.danoeh.antennapod.net.download.serviceinterface;

import android.content.Context;

public abstract class AutoDownloadManager {
    private static AutoDownloadManager instance;

    public static AutoDownloadManager getInstance() {
        return instance;
    }

    public static void setInstance(AutoDownloadManager instance) {
        AutoDownloadManager.instance = instance;
    }

    public abstract void performAutoDownload(final Context context);

    public abstract void performAutoDeletion(final Context context);
}
