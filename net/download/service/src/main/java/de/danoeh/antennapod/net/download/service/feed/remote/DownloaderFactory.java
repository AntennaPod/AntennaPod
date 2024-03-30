package de.danoeh.antennapod.net.download.service.feed.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.danoeh.antennapod.model.download.DownloadRequest;

public interface DownloaderFactory {
    @Nullable
    Downloader create(@NonNull DownloadRequest request);
}