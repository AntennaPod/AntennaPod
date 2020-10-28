package de.danoeh.antennapod.core.feed;

import android.content.ContentResolver;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.text.TextUtils;

import androidx.documentfile.provider.DocumentFile;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.DownloadError;

public class LocalFeedUpdater {

    public static void updateFeed(Feed feed, Context context) {
        String uriString = feed.getDownload_url().replace(Feed.PREFIX_LOCAL_FOLDER, "");
        DocumentFile documentFolder = DocumentFile.fromTreeUri(context, Uri.parse(uriString));
        if (documentFolder == null) {
            reportError(feed, "Unable to retrieve document tree."
                    + "Try re-connecting the folder on the podcast info page.");
            return;
        }
        if (!documentFolder.exists() || !documentFolder.canRead()) {
            reportError(feed, "Cannot read local directory. Try re-connecting the folder on the podcast info page.");
            return;
        }

        if (feed.getItems() == null) {
            feed.setItems(new ArrayList<>());
        }
        //make sure it is the latest 'version' of this feed from the db (all items etc)
        feed = DBTasks.updateFeed(context, feed, false);

        // list files in feed folder
        List<DocumentFile> mediaFiles = new ArrayList<>();
        Set<String> mediaFileNames = new HashSet<>();
        for (DocumentFile file : documentFolder.listFiles()) {
            String mime = file.getType();
            if (mime != null && (mime.startsWith("audio/") || mime.startsWith("video/"))) {
                mediaFiles.add(file);
                mediaFileNames.add(file.getName());
            }
        }

        // add new files to feed and update item data
        List<FeedItem> newItems = feed.getItems();
        for (DocumentFile f : mediaFiles) {
            FeedItem oldItem = feedContainsFile(feed, f.getName());
            FeedItem newItem = createFeedItem(feed, f, context);
            if (oldItem == null) {
                newItems.add(newItem);
            } else {
                oldItem.updateFromOther(newItem);
            }
        }

        // remove feed items without corresponding file
        Iterator<FeedItem> it = newItems.iterator();
        while (it.hasNext()) {
            FeedItem feedItem = it.next();
            if (!mediaFileNames.contains(feedItem.getLink())) {
                it.remove();
            }
        }

        List<String> iconLocations = Arrays.asList("folder.jpg", "Folder.jpg", "folder.png", "Folder.png");
        for (String iconLocation : iconLocations) {
            DocumentFile image = documentFolder.findFile(iconLocation);
            if (image != null) {
                feed.setImageUrl(image.getUri().toString());
                break;
            }
        }
        if (StringUtils.isBlank(feed.getImageUrl())) {
            // set default feed image
            feed.setImageUrl(getDefaultIconUrl(context));
        }
        if (feed.getPreferences().getAutoDownload()) {
            feed.getPreferences().setAutoDownload(false);
            feed.getPreferences().setAutoDeleteAction(FeedPreferences.AutoDeleteAction.NO);
            try {
                DBWriter.setFeedPreferences(feed.getPreferences()).get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        // update items, delete items without existing file;
        // only delete items if the folder contains at least one element to avoid accidentally
        // deleting played state or position in case the folder is temporarily unavailable.
        boolean removeUnlistedItems = (newItems.size() >= 1);
        DBTasks.updateFeed(context, feed, removeUnlistedItems);

        if (mustReportDownloadSuccessful(feed)) {
            reportSuccess(feed);
        }
    }

    /**
     * Returns the URL of the default icon for a local feed. The URL refers to an app resource file.
     */
    public static String getDefaultIconUrl(Context context) {
        String resourceEntryName = context.getResources().getResourceEntryName(R.raw.local_feed_default_icon);
        return ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + context.getPackageName() + "/raw/"
                + resourceEntryName;
    }

    private static FeedItem feedContainsFile(Feed feed, String filename) {
        List<FeedItem> items = feed.getItems();
        for (FeedItem i : items) {
            if (i.getMedia() != null && i.getLink().equals(filename)) {
                return i;
            }
        }
        return null;
    }

    private static FeedItem createFeedItem(Feed feed, DocumentFile file, Context context) {
        String uuid = UUID.randomUUID().toString();
        FeedItem item = new FeedItem(0, file.getName(), uuid, file.getName(), new Date(),
                FeedItem.UNPLAYED, feed);
        item.setAutoDownload(false);

        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(context, file.getUri());
        String durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        String title = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (!TextUtils.isEmpty(title)) {
            item.setTitle(title);
        }

        //add the media to the item
        long duration = Long.parseLong(durationStr);
        long size = file.length();
        FeedMedia media = new FeedMedia(0, item, (int) duration, 0, size, file.getType(),
                file.getUri().toString(), file.getUri().toString(), false, null, 0, 0);
        media.setHasEmbeddedPicture(mediaMetadataRetriever.getEmbeddedPicture() != null);
        item.setMedia(media);

        return item;
    }

    private static void reportError(Feed feed, String reasonDetailed) {
        DownloadStatus status = new DownloadStatus(feed, feed.getTitle(),
                DownloadError.ERROR_IO_ERROR, false, reasonDetailed, true);
        DBWriter.addDownloadStatus(status);
        DBWriter.setFeedLastUpdateFailed(feed.getId(), true);
    }

    /**
     * Reports a successful download status.
     */
    private static void reportSuccess(Feed feed) {
        DownloadStatus status = new DownloadStatus(feed, feed.getTitle(),
                DownloadError.SUCCESS, true, null, true);
        DBWriter.addDownloadStatus(status);
        DBWriter.setFeedLastUpdateFailed(feed.getId(), false);
    }

    /**
     * Answers if reporting success is needed for the given feed.
     */
    private static boolean mustReportDownloadSuccessful(Feed feed) {
        List<DownloadStatus> downloadStatuses = DBReader.getFeedDownloadLog(feed.getId());

        if (downloadStatuses.isEmpty()) {
            // report success if never reported before
            return true;
        }

        Collections.sort(downloadStatuses, (downloadStatus1, downloadStatus2) ->
                downloadStatus1.getCompletionDate().compareTo(downloadStatus2.getCompletionDate()));

        DownloadStatus lastDownloadStatus = downloadStatuses.get(downloadStatuses.size() - 1);

        // report success if the last update was not successful
        // (avoid logging success again if the last update was ok)
        return !lastDownloadStatus.isSuccessful();
    }
}
