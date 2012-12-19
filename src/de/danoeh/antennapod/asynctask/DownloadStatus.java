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

	// ----------------------------------- ATTRIBUTES STORED IN DB
	/** Unique id for storing the object in database. */
	protected long id;
	/**
	 * A human-readable string which is shown to the user so that he can
	 * identify the download. Should be the title of the item/feed/media or the
	 * URL if the download has no other title.
	 */
	protected String title;
	protected int reason;
	/**
	 * A message which can be presented to the user to give more information.
	 * Should be null if Download was successful.
	 */
	protected String reasonDetailed;
	protected boolean successful;
	protected Date completionDate;
	protected FeedFile feedfile;
	/**
	 * Is used to determine the type of the feedfile even if the feedfile does
	 * not exist anymore. The value should be FEEDFILETYPE_FEED,
	 * FEEDFILETYPE_FEEDIMAGE or FEEDFILETYPE_FEEDMEDIA
	 */
	protected int feedfileType;

	// ------------------------------------ NOT STORED IN DB
	protected int progressPercent;
	protected long soFar;
	protected long size;
	protected int statusMsg;
	protected boolean done;
	protected boolean cancelled;

	public DownloadStatus(FeedFile feedfile, String title) {
		this.feedfile = feedfile;
		if (feedfile != null) {
			feedfileType = feedfile.getTypeAsInt();
		}
		this.title = title;
	}

	/** Constructor for restoring Download status entries from DB. */
	public DownloadStatus(long id, String title, FeedFile feedfile,
			int feedfileType, boolean successful, int reason,
			Date completionDate, String reasonDetailed) {
		progressPercent = 100;
		soFar = 0;
		size = 0;

		this.id = id;
		this.title = title;
		this.done = true;
		this.feedfile = feedfile;
		this.reason = reason;
		this.successful = successful;
		this.completionDate = completionDate;
		this.reasonDetailed = reasonDetailed;
		this.feedfileType = feedfileType;
	}

	/** Constructor for creating new completed downloads. */
	public DownloadStatus(FeedFile feedfile, String title, int reason,
			boolean successful, String reasonDetailed) {
		this(0, title, feedfile, feedfile.getTypeAsInt(), successful, reason,
				new Date(), reasonDetailed);
	}

	@Override
	public String toString() {
		return "DownloadStatus [id=" + id + ", title=" + title + ", reason="
				+ reason + ", reasonDetailed=" + reasonDetailed
				+ ", successful=" + successful + ", completionDate="
				+ completionDate + ", feedfile=" + feedfile + ", feedfileType="
				+ feedfileType + ", progressPercent=" + progressPercent
				+ ", soFar=" + soFar + ", size=" + size + ", statusMsg="
				+ statusMsg + ", done=" + done + ", cancelled=" + cancelled
				+ "]";
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

	public String getReasonDetailed() {
		return reasonDetailed;
	}

	public void setReasonDetailed(String reasonDetailed) {
		this.reasonDetailed = reasonDetailed;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getFeedfileType() {
		return feedfileType;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

}