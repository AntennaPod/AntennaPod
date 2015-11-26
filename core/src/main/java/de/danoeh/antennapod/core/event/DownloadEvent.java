package de.danoeh.antennapod.core.event;

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

    @Override
    public String toString() {
        return "DownloadEvent{" +
                "update=" + update +
                '}';
    }
}
