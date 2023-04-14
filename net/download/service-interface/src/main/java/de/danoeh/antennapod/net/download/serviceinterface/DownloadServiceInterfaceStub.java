package de.danoeh.antennapod.net.download.serviceinterface;

import android.content.Context;
import de.danoeh.antennapod.model.feed.FeedItem;

public class DownloadServiceInterfaceStub extends DownloadServiceInterface {

    @Override
    public void download(Context context, FeedItem item, boolean ignoreConstraints) {
    }

    @Override
    public void cancel(Context context, String url) {
    }

    @Override
    public void cancelAll(Context context) {
    }
}
