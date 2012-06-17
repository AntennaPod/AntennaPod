package de.podfetcher.service;

import de.podfetcher.feed.FeedFile;

/** Contains status attributes for one download*/
public class DownloadStatus {

    protected FeedFile feedfile;
    protected int progressPercent;
    protected long soFar;
    protected long size;
    protected int statusMsg;
    protected int reason;
    protected boolean successful;
    protected boolean done;

    public DownloadStatus(FeedFile feedfile) {
        this.feedfile = feedfile;
    }

    public FeedFile getFeedFile() {
        return feedfile;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public long getSoFar() {
        return soFar;
    }

    public long getSize() {
        return size;
    }

    public int getStatusMsg() {
        return statusMsg;
    }

    public int getReason() {
        return reason;
    }

    public boolean isSuccessful() {
        return successful;
    }
}