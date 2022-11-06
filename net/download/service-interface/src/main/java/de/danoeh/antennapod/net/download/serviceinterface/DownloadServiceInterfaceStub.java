package de.danoeh.antennapod.net.download.serviceinterface;

import android.content.Context;

public class DownloadServiceInterfaceStub extends DownloadServiceInterface {

    public void download(Context context, boolean cleanupMedia, DownloadRequest... requests) {
    }

    public void refreshAllFeeds(Context context, boolean initiatedByUser) {
    }

    public void cancel(Context context, String url) {
    }

    public void cancelAll(Context context) {
    }
}
