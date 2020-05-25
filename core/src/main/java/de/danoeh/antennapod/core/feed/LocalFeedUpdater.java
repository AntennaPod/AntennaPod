package de.danoeh.antennapod.core.feed;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import androidx.documentfile.provider.DocumentFile;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.DateUtils;
import de.danoeh.antennapod.core.util.DownloadError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class LocalFeedUpdater {

    public static void updateFeed(Feed feed, Context context) {
        String uriString = feed.getDownload_url().replace(Feed.PREFIX_LOCAL_FOLDER, "");
        DocumentFile documentFolder = DocumentFile.fromTreeUri(context, Uri.parse(uriString));
        if (documentFolder == null) {
            reportError(feed, "Unable to retrieve document tree");
            return;
        }
        if (!documentFolder.exists() || !documentFolder.canRead()) {
            reportError(feed, "Cannot read local directory");
            return;
        }

        if (feed.getItems() == null) {
            feed.setItems(new ArrayList<>());
        }
        //make sure it is the latest 'version' of this feed from the db (all items etc)
        feed = DBTasks.updateFeed(context, feed)[0];

        List<DocumentFile> mediaFiles = new ArrayList<>();
        for (DocumentFile file : documentFolder.listFiles()) {
            String mime = file.getType();
            if (mime != null && (mime.startsWith("audio/") || mime.startsWith("video/"))) {
                mediaFiles.add(file);
            }
        }

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

        List<String> iconLocations = Arrays.asList("folder.jpg", "Folder.jpg", "folder.png", "Folder.png");
        for (String iconLocation : iconLocations) {
            DocumentFile image = documentFolder.findFile(iconLocation);
            if (image != null) {
                feed.setImageUrl(image.getUri().toString());
                break;
            }
        }

        DBTasks.updateFeed(context, feed);
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
        item.setMedia(media);

        return item;
    }

    private static void reportError(Feed feed, String reasonDetailed) {
        DownloadStatus status = new DownloadStatus(feed, feed.getTitle(),
                DownloadError.ERROR_IO_ERROR, false, reasonDetailed, true);
        DBWriter.addDownloadStatus(status);
        DBWriter.setFeedLastUpdateFailed(feed.getId(), true);
    }
}
