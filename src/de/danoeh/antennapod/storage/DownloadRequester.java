package de.danoeh.antennapod.storage;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.webkit.URLUtil;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.feed.*;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.service.download.DownloadRequest;
import de.danoeh.antennapod.service.download.DownloadService;
import de.danoeh.antennapod.util.FileNameGenerator;
import de.danoeh.antennapod.util.URLChecker;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Sends download requests to the DownloadService. This class should always be used for starting downloads,
 * otherwise they won't work correctly.
 */
public class DownloadRequester {
    private static final String TAG = "DownloadRequester";

    public static final String IMAGE_DOWNLOADPATH = "images/";
    public static final String FEED_DOWNLOADPATH = "cache/";
    public static final String MEDIA_DOWNLOADPATH = "media/";

    private static DownloadRequester downloader;

    Map<String, DownloadRequest> downloads;

    private DownloadRequester() {
        downloads = new ConcurrentHashMap<String, DownloadRequest>();
    }

    public static synchronized DownloadRequester getInstance() {
        if (downloader == null) {
            downloader = new DownloadRequester();
        }
        return downloader;
    }

    /**
     * Starts a new download with the given DownloadRequest. This method should only
     * be used from outside classes if the DownloadRequest was created by the DownloadService to
     * ensure that the data is valid. Use downloadFeed(), downloadImage() or downloadMedia() instead.
     *
     * @param context Context object for starting the DownloadService
     * @param request The DownloadRequest. If another DownloadRequest with the same source URL is already stored, this method
     *                call will return false.
     * @return True if the download request was accepted, false otherwise.
     */
    public boolean download(Context context, DownloadRequest request) {
        if (context == null) throw new IllegalArgumentException("context = null");
        if (request == null) throw new IllegalArgumentException("request = null");
        if (downloads.containsKey(request.getSource())) {
            if (AppConfig.DEBUG) Log.i(TAG, "DownloadRequest is already stored.");
            return false;
        }
        downloads.put(request.getSource(), request);

        Intent launchIntent = new Intent(context, DownloadService.class);
        launchIntent.putExtra(DownloadService.EXTRA_REQUEST, request);
        context.startService(launchIntent);
        EventDistributor.getInstance().sendDownloadQueuedBroadcast();
        return true;
    }

    private void download(Context context, FeedFile item, File dest,
                          boolean overwriteIfExists, String username, String password) {
        if (!isDownloadingFile(item)) {
            if (!isFilenameAvailable(dest.toString()) || dest.exists()) {
                if (AppConfig.DEBUG)
                    Log.d(TAG, "Filename already used.");
                if (isFilenameAvailable(dest.toString()) && overwriteIfExists) {
                    boolean result = dest.delete();
                    if (AppConfig.DEBUG)
                        Log.d(TAG, "Deleting file. Result: " + result);
                } else {
                    // find different name
                    File newDest = null;
                    for (int i = 1; i < Integer.MAX_VALUE; i++) {
                        String newName = FilenameUtils.getBaseName(dest
                                .getName())
                                + "-"
                                + i
                                + FilenameUtils.EXTENSION_SEPARATOR
                                + FilenameUtils.getExtension(dest.getName());
                        if (AppConfig.DEBUG)
                            Log.d(TAG, "Testing filename " + newName);
                        newDest = new File(dest.getParent(), newName);
                        if (!newDest.exists()
                                && isFilenameAvailable(newDest.toString())) {
                            if (AppConfig.DEBUG)
                                Log.d(TAG, "File doesn't exist yet. Using "
                                        + newName);
                            break;
                        }
                    }
                    if (newDest != null) {
                        dest = newDest;
                    }
                }
            }
            if (AppConfig.DEBUG)
                Log.d(TAG,
                        "Requesting download of url " + item.getDownload_url());
            item.setDownload_url(URLChecker.prepareURL(item.getDownload_url()));

            DownloadRequest request = new DownloadRequest(dest.toString(),
                    item.getDownload_url(), item.getHumanReadableIdentifier(),
                    item.getId(), item.getTypeAsInt(), username, password);

            download(context, request);
        } else {
            Log.e(TAG, "URL " + item.getDownload_url()
                    + " is already being downloaded");
        }
    }

    /**
     * Returns true if a filename is available and false if it has already been
     * taken by another requested download.
     */
    private boolean isFilenameAvailable(String path) {
        for (String key : downloads.keySet()) {
            DownloadRequest r = downloads.get(key);
            if (StringUtils.equals(r.getDestination(), path)) {
                if (AppConfig.DEBUG)
                    Log.d(TAG, path
                            + " is already used by another requested download");
                return false;
            }
        }
        if (AppConfig.DEBUG)
            Log.d(TAG, path + " is available as a download destination");
        return true;
    }

    public void downloadFeed(Context context, Feed feed)
            throws DownloadRequestException {
        if (feedFileValid(feed)) {
            String username = (feed.getPreferences() != null) ? feed.getPreferences().getUsername() : null;
            String password = (feed.getPreferences() != null) ? feed.getPreferences().getPassword() : null;

            download(context, feed, new File(getFeedfilePath(context),
                    getFeedfileName(feed)), true, username, password);
        }
    }

    public void downloadImage(Context context, FeedImage image)
            throws DownloadRequestException {
        if (feedFileValid(image)) {
            download(context, image, new File(getImagefilePath(context),
                    getImagefileName(image)), false, null, null);
        }
    }

    public void downloadMedia(Context context, FeedMedia feedmedia)
            throws DownloadRequestException {
        if (feedFileValid(feedmedia)) {
            download(context, feedmedia,
                    new File(getMediafilePath(context, feedmedia),
                            getMediafilename(feedmedia)), false, null, null
            );
        }
    }

    /**
     * Throws a DownloadRequestException if the feedfile or the download url of
     * the feedfile is null.
     *
     * @throws DownloadRequestException
     */
    private boolean feedFileValid(FeedFile f) throws DownloadRequestException {
        if (f == null) {
            throw new DownloadRequestException("Feedfile was null");
        } else if (f.getDownload_url() == null) {
            throw new DownloadRequestException("File has no download URL");
        } else {
            return true;
        }
    }

    /**
     * Cancels a running download.
     */
    public void cancelDownload(final Context context, final FeedFile f) {
        cancelDownload(context, f.getDownload_url());
    }

    /**
     * Cancels a running download.
     */
    public void cancelDownload(final Context context, final String downloadUrl) {
        if (AppConfig.DEBUG)
            Log.d(TAG, "Cancelling download with url " + downloadUrl);
        Intent cancelIntent = new Intent(DownloadService.ACTION_CANCEL_DOWNLOAD);
        cancelIntent.putExtra(DownloadService.EXTRA_DOWNLOAD_URL, downloadUrl);
        context.sendBroadcast(cancelIntent);
    }

    /**
     * Cancels all running downloads
     */
    public void cancelAllDownloads(Context context) {
        if (AppConfig.DEBUG)
            Log.d(TAG, "Cancelling all running downloads");
        context.sendBroadcast(new Intent(
                DownloadService.ACTION_CANCEL_ALL_DOWNLOADS));
    }

    /**
     * Returns true if there is at least one Feed in the downloads queue.
     */
    public boolean isDownloadingFeeds() {
        for (DownloadRequest r : downloads.values()) {
            if (r.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if feedfile is in the downloads list
     */
    public boolean isDownloadingFile(FeedFile item) {
        if (item.getDownload_url() != null) {
            return downloads.containsKey(item.getDownload_url());
        }
        return false;
    }

    public DownloadRequest getDownload(String downloadUrl) {
        return downloads.get(downloadUrl);
    }

    /**
     * Checks if feedfile with the given download url is in the downloads list
     */
    public boolean isDownloadingFile(String downloadUrl) {
        return downloads.get(downloadUrl) != null;
    }

    public boolean hasNoDownloads() {
        return downloads.isEmpty();
    }

    /**
     * Remove an object from the downloads-list of the requester.
     */
    public void removeDownload(DownloadRequest r) {
        if (downloads.remove(r.getSource()) == null) {
            Log.e(TAG,
                    "Could not remove object with url " + r.getSource());
        }
    }

    /**
     * Get the number of uncompleted Downloads
     */
    public int getNumberOfDownloads() {
        return downloads.size();
    }

    public String getFeedfilePath(Context context)
            throws DownloadRequestException {
        return getExternalFilesDirOrThrowException(context, FEED_DOWNLOADPATH)
                .toString() + "/";
    }

    public String getFeedfileName(Feed feed) {
        String filename = feed.getDownload_url();
        if (feed.getTitle() != null && !feed.getTitle().isEmpty()) {
            filename = feed.getTitle();
        }
        return "feed-" + FileNameGenerator.generateFileName(filename);
    }

    public String getImagefilePath(Context context)
            throws DownloadRequestException {
        return getExternalFilesDirOrThrowException(context, IMAGE_DOWNLOADPATH)
                .toString() + "/";
    }

    public String getImagefileName(FeedImage image) {
        String filename = image.getDownload_url();
        if (image.getOwner() != null && image.getOwner().getHumanReadableIdentifier() != null) {
            filename = image.getOwner().getHumanReadableIdentifier();
        }
        return "image-" + FileNameGenerator.generateFileName(filename);
    }

    public String getMediafilePath(Context context, FeedMedia media)
            throws DownloadRequestException {
        File externalStorage = getExternalFilesDirOrThrowException(
                context,
                MEDIA_DOWNLOADPATH
                        + FileNameGenerator.generateFileName(media.getItem()
                        .getFeed().getTitle()) + "/"
        );
        return externalStorage.toString();
    }

    private File getExternalFilesDirOrThrowException(Context context,
                                                     String type) throws DownloadRequestException {
        File result = UserPreferences.getDataFolder(context, type);
        if (result == null) {
            throw new DownloadRequestException(
                    "Failed to access external storage");
        }
        return result;
    }

    public String getMediafilename(FeedMedia media) {
        String filename;
        String titleBaseFilename = "";

        // Try to generate the filename by the item title
        if (media.getItem() != null && media.getItem().getTitle() != null) {
            String title = media.getItem().getTitle();
            // Delete reserved characters
            titleBaseFilename = title.replaceAll("[\\\\/%\\?\\*:|<>\"\\p{Cntrl}]", "");
            titleBaseFilename = titleBaseFilename.trim();
        }

        String URLBaseFilename = URLUtil.guessFileName(media.getDownload_url(),
                null, media.getMime_type());
        ;

        if (titleBaseFilename != "") {
            // Append extension
            filename = titleBaseFilename + FilenameUtils.EXTENSION_SEPARATOR +
                    FilenameUtils.getExtension(URLBaseFilename);
        } else {
            // Fall back on URL file name
            filename = URLBaseFilename;
        }
        return filename;
    }
}
