package de.danoeh.antennapod.core.service.download;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadRequest;

public interface DownloaderFactory {
    @Nullable
    Downloader create(@NonNull DownloadRequest request);
}