package de.danoeh.antennapodSA.core.service.download.handler;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapodSA.core.event.DownloadEvent;
import de.danoeh.antennapodSA.core.service.download.Downloader;

public class PostDownloaderTask implements Runnable {
    private List<Downloader> downloads;

    public PostDownloaderTask(List<Downloader> downloads) {
        this.downloads = downloads;
    }

    @Override
    public void run() {
        List<Downloader> runningDownloads = new ArrayList<>();
        for (Downloader downloader : downloads) {
            if (!downloader.cancelled) {
                runningDownloads.add(downloader);
            }
        }
        List<Downloader> list = Collections.unmodifiableList(runningDownloads);
        EventBus.getDefault().postSticky(DownloadEvent.refresh(list));
    }
}
