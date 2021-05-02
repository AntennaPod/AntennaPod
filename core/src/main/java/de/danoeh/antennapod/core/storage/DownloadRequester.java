package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;

import de.danoeh.antennapod.core.service.download.Downloader;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedFile;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.util.DownloadError;
import de.danoeh.antennapod.core.util.FileNameGenerator;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.URLChecker;


/**
 * Sends download requests to the DownloadService. This class should always be used for starting downloads,
 * otherwise they won't work correctly.
 */
public class DownloadRequester implements DownloadStateProvider {
    private static final String TAG = "DownloadRequester";

    private static final String FEED_DOWNLOADPATH = "cache/";
    private static final String MEDIA_DOWNLOADPATH = "media/";

    /**
     * Denotes the page of the feed that is contained in the DownloadRequest sent by the DownloadRequester.
     */
    public static final String REQUEST_ARG_PAGE_NR = "page";

    /**
     * True if all pages after the feed that is contained in this DownloadRequest should be downloaded.
     */
    public static final String REQUEST_ARG_LOAD_ALL_PAGES = "loadAllPages";

    private static DownloadRequester downloader;

    private final Map<String, DownloadRequest> downloads;

    private DownloadRequester() {
        downloads = new ConcurrentHashMap<>();
    }

    public static synchronized DownloadRequester getInstance() {
        if (downloader == null) {
            downloader = new DownloadRequester();
        }
        return downloader;
    }

    /**
     * Starts a new download with the given a list of DownloadRequest. This method should only
     * be used from outside classes if the DownloadRequest was created by the DownloadService to
     * ensure that the data is valid. Use downloadFeed(), downloadImage() or downloadMedia() instead.
     *
     * @param context Context object for starting the DownloadService
     * @param requests The list of DownloadRequest objects. If another DownloadRequest
     *                 with the same source URL is already stored, this one will be skipped.
     * @return True if any of the download request was accepted, false otherwise.
     */
    public synchronized boolean download(@NonNull Context context, DownloadRequest... requests) {
        return download(context, false, requests);
    }

    private boolean download(@NonNull Context context, boolean cleanupMedia, DownloadRequest... requests) {
        if (requests.length <= 0) {
            return false;
        }
        boolean result = false;

        ArrayList<DownloadRequest> requestsToSend = new ArrayList<>(requests.length);
        for (DownloadRequest request : requests) {
            if (downloads.containsKey(request.getSource())) {
                if (BuildConfig.DEBUG) Log.i(TAG, "DownloadRequest is already stored.");
                continue;
            }
            downloads.put(request.getSource(), request);

            requestsToSend.add(request);
            result = true;
        }
        Intent launchIntent = new Intent(context, DownloadService.class);
        launchIntent.putParcelableArrayListExtra(DownloadService.EXTRA_REQUESTS, requestsToSend);
        if (cleanupMedia) {
            launchIntent.putExtra(DownloadService.EXTRA_CLEANUP_MEDIA, cleanupMedia);
        }
        ContextCompat.startForegroundService(context, launchIntent);

        return result;
    }

    @Nullable
    private DownloadRequest createRequest(FeedFile item, FeedFile container, File dest, boolean overwriteIfExists,
                                          String username, String password, String lastModified,
                                          boolean deleteOnFailure, Bundle arguments, boolean initiatedByUser) {
        final boolean partiallyDownloadedFileExists = item.getFile_url() != null && new File(item.getFile_url()).exists();

        Log.d(TAG, "partiallyDownloadedFileExists: " + partiallyDownloadedFileExists);
        if (isDownloadingFile(item)) {
            Log.e(TAG, "URL " + item.getDownload_url()
                    + " is already being downloaded");
            return null;
        }
        if (!isFilenameAvailable(dest.toString()) || (!partiallyDownloadedFileExists && dest.exists())) {
            Log.d(TAG, "Filename already used.");
            if (isFilenameAvailable(dest.toString()) && overwriteIfExists) {
                boolean result = dest.delete();
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
                    Log.d(TAG, "Testing filename " + newName);
                    newDest = new File(dest.getParent(), newName);
                    if (!newDest.exists()
                            && isFilenameAvailable(newDest.toString())) {
                        Log.d(TAG, "File doesn't exist yet. Using " + newName);
                        break;
                    }
                }
                if (newDest != null) {
                    dest = newDest;
                }
            }
        }
        Log.d(TAG, "Requesting download of url " + item.getDownload_url());
        String baseUrl = (container != null) ? container.getDownload_url() : null;
        item.setDownload_url(URLChecker.prepareURL(item.getDownload_url(), baseUrl));

        DownloadRequest.Builder builder = new DownloadRequest.Builder(dest.toString(), item, initiatedByUser)
                .withAuthentication(username, password)
                .lastModified(lastModified)
                .deleteOnFailure(deleteOnFailure)
                .withArguments(arguments);
        return builder.build();
    }

    /**
     * Returns true if a filename is available and false if it has already been
     * taken by another requested download.
     */
    private boolean isFilenameAvailable(String path) {
        for (String key : downloads.keySet()) {
            DownloadRequest r = downloads.get(key);
            if (TextUtils.equals(r.getDestination(), path)) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, path
                            + " is already used by another requested download");
                return false;
            }
        }
        if (BuildConfig.DEBUG)
            Log.d(TAG, path + " is available as a download destination");
        return true;
    }

    /**
     * Downloads a feed.
     *
     * @param context The application's environment.
     * @param feed Feeds to download
     * @param loadAllPages Set to true to download all pages
     */
    public synchronized void downloadFeed(Context context, Feed feed, boolean loadAllPages,
                                           boolean force, boolean initiatedByUser) throws DownloadRequestException {
        downloadFeeds(context, Collections.singletonList(feed), loadAllPages, force, initiatedByUser);
    }

    /**
     * Downloads a list of feeds.
     *
     * @param context The application's environment.
     * @param feeds Feeds to download
     * @param loadAllPages Set to true to download all pages
     */
    public synchronized void downloadFeeds(Context context, List<Feed> feeds, boolean loadAllPages,
                                          boolean force, boolean initiatedByUser) throws DownloadRequestException {
        List<DownloadRequest> requests = new ArrayList<>();
        for (Feed feed : feeds) {
            if (!feedFileValid(feed)) {
                continue;
            }
            String username = (feed.getPreferences() != null) ? feed.getPreferences().getUsername() : null;
            String password = (feed.getPreferences() != null) ? feed.getPreferences().getPassword() : null;
            String lastModified = feed.isPaged() || force ? null : feed.getLastUpdate();

            Bundle args = new Bundle();
            args.putInt(REQUEST_ARG_PAGE_NR, feed.getPageNr());
            args.putBoolean(REQUEST_ARG_LOAD_ALL_PAGES, loadAllPages);

            DownloadRequest request = createRequest(feed, null, new File(getFeedfilePath(), getFeedfileName(feed)),
                    true, username, password, lastModified, true, args, initiatedByUser
            );
            if (request != null) {
                requests.add(request);
            }
        }
        if (!requests.isEmpty()) {
            download(context, requests.toArray(new DownloadRequest[0]));
        }
    }

    public synchronized void downloadFeed(Context context, Feed feed) throws DownloadRequestException {
        downloadFeed(context, feed, false, false, true);
    }

    public synchronized void downloadMedia(@NonNull Context context, boolean initiatedByUser, FeedItem... feedItems)
            throws DownloadRequestException {
        downloadMedia(true, context, initiatedByUser, feedItems);

    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public synchronized void downloadMedia(boolean performAutoCleanup, @NonNull Context context,
                                           boolean initiatedByUser, FeedItem... items)
            throws DownloadRequestException {
        Log.d(TAG, "downloadMedia() called with: performAutoCleanup = [" + performAutoCleanup
                + "], #items = [" + items.length + "]");

        List<DownloadRequest> requests = new ArrayList<>(items.length);
        for (FeedItem item : items) {
            try {
                DownloadRequest request = createRequest(item.getMedia(), initiatedByUser);
                if (request != null) {
                    requests.add(request);
                }
            } catch (DownloadRequestException e) {
                if (items.length < 2) {
                    // single download, typically initiated from users
                    throw e;
                } else {
                    // batch download, typically initiated by auto-download in the background
                    e.printStackTrace();
                    DBWriter.addDownloadStatus(
                            new DownloadStatus(item.getMedia(), item
                                    .getMedia()
                                    .getHumanReadableIdentifier(),
                                    DownloadError.ERROR_REQUEST_ERROR,
                                    false, e.getMessage(), initiatedByUser
                            )
                    );
                }
            }
        }
        download(context, performAutoCleanup, requests.toArray(new DownloadRequest[0]));
    }

    @Nullable
    private DownloadRequest createRequest(@Nullable FeedMedia feedmedia, boolean initiatedByUser)
            throws DownloadRequestException {
        if (!feedFileValid(feedmedia)) {
            return null;
        }
        Feed feed = feedmedia.getItem().getFeed();
        String username;
        String password;
        if (feed != null && feed.getPreferences() != null) {
            username = feed.getPreferences().getUsername();
            password = feed.getPreferences().getPassword();
        } else {
            username = null;
            password = null;
        }

        File dest;
        if (feedmedia.getFile_url() != null && new File(feedmedia.getFile_url()).exists()) {
            dest = new File(feedmedia.getFile_url());
        } else {
            dest = new File(getMediafilePath(feedmedia), getMediafilename(feedmedia));
        }
        return createRequest(feedmedia, feed, dest, false, username, password, null, false, null, initiatedByUser);
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
    public synchronized void cancelDownload(final Context context, final FeedFile f) {
        cancelDownload(context, f.getDownload_url());
    }

    /**
     * Cancels a running download.
     */
    public synchronized void cancelDownload(final Context context, final String downloadUrl) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Cancelling download with url " + downloadUrl);
        Intent cancelIntent = new Intent(DownloadService.ACTION_CANCEL_DOWNLOAD);
        cancelIntent.putExtra(DownloadService.EXTRA_DOWNLOAD_URL, downloadUrl);
        cancelIntent.setPackage(context.getPackageName());
        context.sendBroadcast(cancelIntent);
    }

    /**
     * Cancels all running downloads
     */
    public synchronized void cancelAllDownloads(Context context) {
        Log.d(TAG, "Cancelling all running downloads");
        IntentUtils.sendLocalBroadcast(context, DownloadService.ACTION_CANCEL_ALL_DOWNLOADS);
    }

    /**
     * Returns true if there is at least one Feed in the downloads queue.
     */
    public synchronized boolean isDownloadingFeeds() {
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
    public synchronized boolean isDownloadingFile(@NonNull FeedFile item) {
        return item.getDownload_url() != null && downloads.containsKey(item.getDownload_url());
    }

    /**
     * Get the downloader for this item.
     */
    public synchronized DownloadRequest getRequestFor(FeedFile item) {
        if (isDownloadingFile(item)) {
            return downloads.get(item.getDownload_url());
        }
        return null;
    }

    /**
     * Checks if feedfile with the given download url is in the downloads list
     */
    public synchronized boolean isDownloadingFile(String downloadUrl) {
        return downloads.get(downloadUrl) != null;
    }

    public synchronized boolean hasNoDownloads() {
        return downloads.isEmpty();
    }

    /**
     * Remove an object from the downloads-list of the requester.
     */
    public synchronized void removeDownload(DownloadRequest r) {
        if (downloads.remove(r.getSource()) == null) {
            Log.e(TAG,
                    "Could not remove object with url " + r.getSource());
        }
    }

    /**
     * Get the number of uncompleted Downloads
     */
    public synchronized int getNumberOfDownloads() {
        return downloads.size();
    }

    private synchronized String getFeedfilePath() throws DownloadRequestException {
        return getExternalFilesDirOrThrowException(FEED_DOWNLOADPATH).toString() + "/";
    }

    private synchronized String getFeedfileName(Feed feed) {
        String filename = feed.getDownload_url();
        if (feed.getTitle() != null && !feed.getTitle().isEmpty()) {
            filename = feed.getTitle();
        }
        return "feed-" + FileNameGenerator.generateFileName(filename);
    }

    private synchronized String getMediafilePath(FeedMedia media) throws DownloadRequestException {
        File externalStorage = getExternalFilesDirOrThrowException(
                MEDIA_DOWNLOADPATH
                        + FileNameGenerator.generateFileName(media.getItem()
                        .getFeed().getTitle()) + "/"
        );
        return externalStorage.toString();
    }

    private File getExternalFilesDirOrThrowException(String type) throws DownloadRequestException {
        File result = UserPreferences.getDataFolder(type);
        if (result == null) {
            throw new DownloadRequestException(
                    "Failed to access external storage");
        }
        return result;
    }

    private String getMediafilename(FeedMedia media) {
        String filename;
        String titleBaseFilename = "";

        // Try to generate the filename by the item title
        if (media.getItem() != null && media.getItem().getTitle() != null) {
            String title = media.getItem().getTitle();
            titleBaseFilename = FileNameGenerator.generateFileName(title);
        }

        String URLBaseFilename = URLUtil.guessFileName(media.getDownload_url(),
                null, media.getMime_type());

        if (!titleBaseFilename.equals("")) {
            // Append extension
            final int FILENAME_MAX_LENGTH = 220;
            if (titleBaseFilename.length() > FILENAME_MAX_LENGTH) {
                titleBaseFilename = titleBaseFilename.substring(0, FILENAME_MAX_LENGTH);
            }
            filename = titleBaseFilename + FilenameUtils.EXTENSION_SEPARATOR +
                    FilenameUtils.getExtension(URLBaseFilename);
        } else {
            // Fall back on URL file name
            filename = URLBaseFilename;
        }
        return filename;
    }

    public void updateProgress(List<Downloader> newDownloads) {
        for (Downloader downloader : newDownloads) {
            DownloadRequest request = downloader.getDownloadRequest();
            if (downloads.containsKey(request.getSource())) {
                downloads.put(request.getSource(), request);
            }
        }
    }
}
