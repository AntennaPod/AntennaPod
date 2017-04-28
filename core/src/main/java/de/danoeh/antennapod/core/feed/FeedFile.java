package de.danoeh.antennapod.core.feed;

import android.text.TextUtils;

import java.io.File;

/**
 * Represents a component of a Feed that has to be downloaded
 */
public abstract class FeedFile extends FeedComponent {
    public static final int NOT_DOWNLOADED = 0;
    public static final int DOWNLOADED = 1;
    public static final int DOWNLOAD_STARTED = 2;

    protected String file_url;
    protected String download_url;
    protected int downloadStatus;

    /**
     * Creates a new FeedFile object.
     *
     * @param file_url     The location of the FeedFile. If this is null, the downloaded-attribute
     *                     will automatically be set to false.
     * @param download_url The location where the FeedFile can be downloaded.
     * @param downloadStatus 1 if the FeedFile has been downloaded, 2 if the download has been started,
     *                       0 otherwise.
     */
    public FeedFile(String file_url, String download_url, int downloadStatus) {
        super();
        this.file_url = file_url;
        this.download_url = download_url;
        this.downloadStatus = downloadStatus;
    }

    public FeedFile() {
        this(null, null, NOT_DOWNLOADED);
    }

    public abstract int getTypeAsInt();

    /**
     * Update this FeedFile's attributes with the attributes from another
     * FeedFile. This method should only update attributes which where read from
     * the feed.
     */
    public void updateFromOther(FeedFile other) {
        super.updateFromOther(other);
        this.download_url = other.download_url;
    }

    /**
     * Compare's this FeedFile's attribute values with another FeedFile's
     * attribute values. This method will only compare attributes which were
     * read from the feed.
     *
     * @return true if attribute values are different, false otherwise
     */
    public boolean compareWithOther(FeedFile other) {
        if (super.compareWithOther(other)) {
            return true;
        }
        if (!TextUtils.equals(download_url, other.download_url)) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the file exists at file_url.
     */
    public boolean fileExists() {
        if (file_url == null) {
            return false;
        } else {
            File f = new File(file_url);
            return f.exists();
        }
    }

    public String getFile_url() {
        return file_url;
    }

    /**
     * Changes the file_url of this FeedFile. Setting this value to
     * null will also set the downloaded-attribute to false.
     */
    public void setFile_url(String file_url) {
        this.file_url = file_url;
        if (file_url == null) {
            downloadStatus = NOT_DOWNLOADED;
        }
    }

    public String getDownload_url() {
        return download_url;
    }

    public void setDownload_url(String download_url) {
        this.download_url = download_url;
    }

    public boolean isDownloaded() {
        return downloadStatus == DOWNLOADED;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloadStatus = downloaded ? DOWNLOADED : NOT_DOWNLOADED;
    }

    public void setDownloadStatus(int status) {
        this.downloadStatus = status;
    }
}
