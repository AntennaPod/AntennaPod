package de.danoeh.antennapod.core.service.download.handler;

import de.danoeh.antennapod.core.service.download.Downloader;

import java.util.List;

public class PostDownloaderTask implements Runnable {
    private List<Downloader> downloads;

    public PostDownloaderTask(List<Downloader> downloads) {
        this.downloads = downloads;
    }

    @Override
    public void run() {

    }
}
