package de.danoeh.antennapod.asynctask;

import java.util.Date;

import de.danoeh.antennapod.feed.FeedFile;

/** Contains status attributes for one download */
public class DownloadStatus {
	/**
	 * Downloaders should use this constant for the size attribute if necessary
	 * so that the listadapters etc. can react properly.
	 */
	public static final int SIZE_UNKNOWN = -1;

	public Date getCompletionDate() {
		return completionDate;
	}

	/** Unique id for storing the object in database. */
	protected long id;

	/** Used by DownloadService to check if the status has been updated. */
	protected volatile boolean updateAvailable;

	protected FeedFile feedfile;
	protected int progressPercent;
	protected long soFar;
	protected long size;
	protected int statusMsg;
	protected int reason;
	protected boolean successful;
	protected boolean done;
	protected Date completionDate;

	public DownloadStatus(FeedFile feedfile) {
		this.feedfile = feedfile;
	}

	/** Constructor for restoring Download status entries from DB. */
	public DownloadStatus(long id, FeedFile feedfile, boolean successful,
			int reason, Date completionDate) {
		this.id = id;
		this.feedfile = feedfile;
		progressPercent = 100;
		soFar = 0;
		size = 0;
		this.reason = reason;
		this.successful = successful;
		this.done = true;
		this.completionDate = completionDate;
	}

	/** Constructor for creating new completed downloads. */
	public DownloadStatus(FeedFile feedfile, int reason, boolean successful) {
		this(0, feedfile, successful, reason, new Date());
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

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public boolean isDone() {
		return done;
	}

	public void setFeedfile(FeedFile feedfile) {
		this.feedfile = feedfile;
	}

	public void setProgressPercent(int progressPercent) {
		this.progressPercent = progressPercent;
	}

	public void setSoFar(long soFar) {
		this.soFar = soFar;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public void setStatusMsg(int statusMsg) {
		this.statusMsg = statusMsg;
	}

	public void setReason(int reason) {
		this.reason = reason;
	}

	public void setSuccessful(boolean successful) {
		this.successful = successful;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	public void setCompletionDate(Date completionDate) {
		this.completionDate = completionDate;
	}

	public boolean isUpdateAvailable() {
		return updateAvailable;
	}

	public void setUpdateAvailable(boolean updateAvailable) {
		this.updateAvailable = updateAvailable;
	}

}