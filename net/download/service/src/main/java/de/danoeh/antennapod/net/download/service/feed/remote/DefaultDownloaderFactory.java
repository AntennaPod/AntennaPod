package de.danoeh.antennapod.net.download.service.feed.remote;

import android.util.Log;
import android.webkit.URLUtil;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.danoeh.antennapod.model.download.DownloadRequest;

public class DefaultDownloaderFactory implements DownloaderFactory {
    private static final String TAG = "DefaultDwnldrFactory";

    @Nullable
    @Override
    public Downloader create(@NonNull DownloadRequest request) {
        if (!URLUtil.isHttpUrl(request.getSource()) && !URLUtil.isHttpsUrl(request.getSource())) {
            Log.e(TAG, "Could not find appropriate downloader for " + request.getSource());
            return null;
        }
        return new HttpDownloader(request);
    }
}