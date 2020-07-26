package de.danoeh.antennapod.core.event;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.core.service.download.Downloader;

public class DownloadEvent {

    public final DownloaderUpdate update;

    private DownloadEvent(DownloaderUpdate downloader) {
        this.update = downloader;
    }

    public static DownloadEvent refresh(List<Downloader> list) {
        list = new ArrayList<>(list);
        DownloaderUpdate update = new DownloaderUpdate(list);
        return new DownloadEvent(update);
    }

    @NonNull
    @Override
    public String toString() {
        return "DownloadEvent{" +
                "update=" + update +
                '}';
    }

    public boolean hasChangedFeedUpdateStatus(boolean oldStatus) {
        return oldStatus != update.feedIds.length > 0;
    }
}
