package de.danoeh.antennapod.model.feed;

import android.text.TextUtils;

import java.io.File;

/**
 * Represents a component of a Feed that has to be downloaded
 */
public abstract class FeedFile extends FeedComponent {

    String file_url;
    protected String download_url;
    boolean downloaded;

    /**
     * Creates a new FeedFile object.
     *
     * @param file_url     The location of the FeedFile. If this is null, the downloaded-attribute
     *                     will automatically be set to false.
     * @param download_url The location where the FeedFile can be downloaded.
     * @param downloaded   true if the FeedFile has been downloaded, false otherwise. This parameter
     *                     will automatically be interpreted as false if the file_url is null.
     */
    public FeedFile(String file_url, String download_url, boolean downloaded) {
        super();
        this.file_url = file_url;
        this.download_url = download_url;
        this.downloaded = (file_url != null) && downloaded;
    }

    public FeedFile() {
        this(null, null, false);
    }

    public abstract int getTypeAsInt();

    /**
     * Update this FeedFile's attributes with the attributes from another
     * FeedFile. This method should only update attributes which where read from
     * the feed.
     */
    void updateFromOther(FeedFile other) {
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
    boolean compareWithOther(FeedFile other) {
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
            downloaded = false;
        }
    }

    public String getDownload_url() {
        return download_url;
    }

    public void setDownload_url(String download_url) {
        this.download_url = download_url;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }
}
