package de.danoeh.antennapod.core.service.download;

import android.util.Log;
import android.webkit.URLUtil;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.FileNameGenerator;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadRequest;
import org.apache.commons.io.FilenameUtils;

import java.io.File;

/**
 * Creates download requests that can be sent to the DownloadService.
 */
public class DownloadRequestCreator {
    private static final String TAG = "DownloadRequestCreat";
    private static final String FEED_DOWNLOADPATH = "cache/";
    private static final String MEDIA_DOWNLOADPATH = "media/";

    public static DownloadRequest.Builder create(Feed feed) {
        File dest = new File(getFeedfilePath(), getFeedfileName(feed));
        if (dest.exists()) {
            dest.delete();
        }
        Log.d(TAG, "Requesting download of url " + feed.getDownload_url());

        String username = (feed.getPreferences() != null) ? feed.getPreferences().getUsername() : null;
        String password = (feed.getPreferences() != null) ? feed.getPreferences().getPassword() : null;

        return new DownloadRequest.Builder(dest.toString(), feed)
                .withAuthentication(username, password)
                .lastModified(feed.getLastUpdate());
    }

    public static DownloadRequest.Builder create(FeedMedia media) {
        final boolean partiallyDownloadedFileExists =
                media.getFile_url() != null && new File(media.getFile_url()).exists();
        File dest;
        if (partiallyDownloadedFileExists) {
            dest = new File(media.getFile_url());
        } else {
            dest = new File(getMediafilePath(media), getMediafilename(media));
        }

        if (dest.exists() && !partiallyDownloadedFileExists) {
            dest = findUnusedFile(dest);
        }
        Log.d(TAG, "Requesting download of url " + media.getDownload_url());

        String username = (media.getItem().getFeed().getPreferences() != null)
                ? media.getItem().getFeed().getPreferences().getUsername() : null;
        String password = (media.getItem().getFeed().getPreferences() != null)
                ? media.getItem().getFeed().getPreferences().getPassword() : null;

        return new DownloadRequest.Builder(dest.toString(), media)
                .withAuthentication(username, password);
    }

    private static File findUnusedFile(File dest) {
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
            if (!newDest.exists()) {
                Log.d(TAG, "File doesn't exist yet. Using " + newName);
                break;
            }
        }
        return newDest;
    }

    private static String getFeedfilePath() {
        return UserPreferences.getDataFolder(FEED_DOWNLOADPATH).toString() + "/";
    }

    private static String getFeedfileName(Feed feed) {
        String filename = feed.getDownload_url();
        if (feed.getTitle() != null && !feed.getTitle().isEmpty()) {
            filename = feed.getTitle();
        }
        return "feed-" + FileNameGenerator.generateFileName(filename) + feed.getId();
    }

    private static String getMediafilePath(FeedMedia media) {
        String mediaPath = MEDIA_DOWNLOADPATH
                + FileNameGenerator.generateFileName(media.getItem().getFeed().getTitle());
        return UserPreferences.getDataFolder(mediaPath).toString() + "/";
    }

    private static String getMediafilename(FeedMedia media) {
        String titleBaseFilename = "";

        // Try to generate the filename by the item title
        if (media.getItem() != null && media.getItem().getTitle() != null) {
            String title = media.getItem().getTitle();
            titleBaseFilename = FileNameGenerator.generateFileName(title);
        }

        String urlBaseFilename = URLUtil.guessFileName(media.getDownload_url(), null, media.getMime_type());

        String baseFilename;
        if (!titleBaseFilename.equals("")) {
            baseFilename = titleBaseFilename;
        } else {
            baseFilename = urlBaseFilename;
        }
        final int filenameMaxLength = 220;
        if (baseFilename.length() > filenameMaxLength) {
            baseFilename = baseFilename.substring(0, filenameMaxLength);
        }
        return baseFilename + FilenameUtils.EXTENSION_SEPARATOR + media.getId()
                + FilenameUtils.EXTENSION_SEPARATOR + FilenameUtils.getExtension(urlBaseFilename);
    }
}
