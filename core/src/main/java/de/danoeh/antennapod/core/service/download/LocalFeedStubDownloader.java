package de.danoeh.antennapod.core.service.download;

import androidx.annotation.NonNull;

/**
 * This does not actually download, but it keeps track of a local feed's refresh state.
 */
public class LocalFeedStubDownloader extends Downloader {

    public LocalFeedStubDownloader(@NonNull DownloadRequest request) {
        super(request);
    }

    @Override
    protected void download() {
    }
}