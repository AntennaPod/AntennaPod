package de.danoeh.antennapod.net.download.service.feed.remote;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.concurrent.Callable;

import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.model.download.DownloadRequest;
import de.danoeh.antennapod.net.download.service.R;

/**
 * Downloads files
 */
public abstract class Downloader implements Callable<Downloader> {
    private static final String TAG = "Downloader";

    private volatile boolean finished;
    public volatile boolean cancelled;
    public String permanentRedirectUrl = null;

    @NonNull
    final DownloadRequest request;
    @NonNull
    final DownloadResult result;

    Downloader(@NonNull DownloadRequest request) {
        super();
        this.request = request;
        this.request.setStatusMsg(R.string.download_pending);
        this.cancelled = false;
        this.result = new DownloadResult(0, request.getTitle(), request.getFeedfileId(), request.getFeedfileType(),
                false, null, new Date(), null);
    }

    protected abstract void download();

    public final Downloader call() {
        download();
        finished = true;
        return this;
    }

    @NonNull
    public DownloadRequest getDownloadRequest() {
        return request;
    }

    @NonNull
    public DownloadResult getResult() {
        return result;
    }

    public boolean isFinished() {
        return finished;
    }

    public void cancel() {
        cancelled = true;
    }

}