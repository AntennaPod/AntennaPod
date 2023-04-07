package de.danoeh.antennapod.net.download.serviceinterface;

import android.content.Context;
import android.content.Intent;
import de.danoeh.antennapod.model.feed.FeedItem;

public class DownloadServiceInterfaceStub extends DownloadServiceInterface {

    @Override
    public void download(Context context, FeedItem item) {
    }

    public void download(Context context, boolean cleanupMedia, DownloadRequest... requests) {
    }

    public Intent makeDownloadIntent(Context context, boolean cleanupMedia, DownloadRequest... requests) {
        return null;
    }

    public void cancel(Context context, String url) {
    }

    public void cancelAll(Context context) {
    }
}
