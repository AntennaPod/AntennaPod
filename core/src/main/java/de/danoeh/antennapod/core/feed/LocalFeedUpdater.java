package de.danoeh.antennapod.core.feed;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.model.download.DownloadStatus;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.parser.feed.util.DateUtils;
import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.parser.feed.util.MimeTypeUtils;
import de.danoeh.antennapod.parser.media.id3.ID3ReaderException;
import de.danoeh.antennapod.parser.media.id3.Id3MetadataReader;
import de.danoeh.antennapod.parser.media.vorbis.VorbisCommentMetadataReader;
import de.danoeh.antennapod.parser.media.vorbis.VorbisCommentReaderException;
import org.apache.commons.io.input.CountingInputStream;

public class LocalFeedUpdater {
    private static final String TAG = "LocalFeedUpdater";

    static final String[] PREFERRED_FEED_IMAGE_FILENAMES = { "folder.jpg", "Folder.jpg", "folder.png", "Folder.png" };

    public static void updateFeed(Feed feed, Context context) {
        try {
            tryUpdateFeed(feed, context);

            if (mustReportDownloadSuccessful(feed)) {
                reportSuccess(feed);
            }
        } catch (Exception e) {
            e.printStackTrace();
            reportError(feed, e.getMessage());
        }
    }

    private static void tryUpdateFeed(Feed feed, Context context) throws IOException {
        String uriString = feed.getDownload_url().replace(Feed.PREFIX_LOCAL_FOLDER, "");
        DocumentFile documentFolder = DocumentFile.fromTreeUri(context, Uri.parse(uriString));
        if (documentFolder == null) {
            throw new IOException("Unable to retrieve document tree. "
                    + "Try re-connecting the folder on the podcast info page.");
        }
        if (!documentFolder.exists() || !documentFolder.canRead()) {
            throw new IOException("Cannot read local directory. "
                    + "Try re-connecting the folder on the podcast info page.");
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
            String mimeType = MimeTypeUtils.getMimeType(file.getType(), file.getUri().toString());
            MediaType mediaType = MediaType.fromMimeType(mimeType);
            if (mediaType == MediaType.AUDIO || mediaType == MediaType.VIDEO) {
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

        feed.setImageUrl(getImageUrl(documentFolder));

        feed.getPreferences().setAutoDownload(false);
        feed.getPreferences().setAutoDeleteAction(FeedPreferences.AutoDeleteAction.NO);
        feed.setDescription(context.getString(R.string.local_feed_description));
        feed.setAuthor(context.getString(R.string.local_folder));

        // update items, delete items without existing file;
        // only delete items if the folder contains at least one element to avoid accidentally
        // deleting played state or position in case the folder is temporarily unavailable.
        boolean removeUnlistedItems = (newItems.size() >= 1);
        DBTasks.updateFeed(context, feed, removeUnlistedItems);
    }

    /**
     * Returns the image URL for the local feed.
     */
    @NonNull
    static String getImageUrl(@NonNull DocumentFile documentFolder) {
        // look for special file names
        for (String iconLocation : PREFERRED_FEED_IMAGE_FILENAMES) {
            DocumentFile image = documentFolder.findFile(iconLocation);
            if (image != null) {
                return image.getUri().toString();
            }
        }

        // use the first image in the folder if existing
        for (DocumentFile file : documentFolder.listFiles()) {
            String mime = file.getType();
            if (mime != null && (mime.startsWith("image/jpeg") || mime.startsWith("image/png"))) {
                return file.getUri().toString();
            }
        }

        // use default icon as fallback
        return Feed.PREFIX_GENERATIVE_COVER + documentFolder.getUri();
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
        FeedItem item = new FeedItem(0, file.getName(), UUID.randomUUID().toString(),
                file.getName(), new Date(file.lastModified()), FeedItem.UNPLAYED, feed);
        item.disableAutoDownload();

        long size = file.length();
        FeedMedia media = new FeedMedia(0, item, 0, 0, size, file.getType(),
                file.getUri().toString(), file.getUri().toString(), false, null, 0, 0);
        item.setMedia(media);

        try {
            loadMetadata(item, file, context);
        } catch (Exception e) {
            item.setDescriptionIfLonger(e.getMessage());
        }

        return item;
    }

    private static void loadMetadata(FeedItem item, DocumentFile file, Context context) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(context, file.getUri());

        String dateStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
        if (!TextUtils.isEmpty(dateStr)) {
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault());
                item.setPubDate(simpleDateFormat.parse(dateStr));
            } catch (ParseException parseException) {
                Date date = DateUtils.parse(dateStr);
                if (date != null) {
                    item.setPubDate(date);
                }
            }
        }

        String title = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (!TextUtils.isEmpty(title)) {
            item.setTitle(title);
        }

        String durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        item.getMedia().setDuration((int) Long.parseLong(durationStr));

        item.getMedia().setHasEmbeddedPicture(mediaMetadataRetriever.getEmbeddedPicture() != null);

        try (InputStream inputStream = context.getContentResolver().openInputStream(file.getUri())) {
            Id3MetadataReader reader = new Id3MetadataReader(new CountingInputStream(inputStream));
            reader.readInputStream();
            item.setDescriptionIfLonger(reader.getComment());
        } catch (IOException | ID3ReaderException e) {
            Log.d(TAG, "Unable to parse ID3 of " + file.getUri() + ": " + e.getMessage());

            try (InputStream inputStream = context.getContentResolver().openInputStream(file.getUri())) {
                VorbisCommentMetadataReader reader = new VorbisCommentMetadataReader(inputStream);
                reader.readInputStream();
                item.setDescriptionIfLonger(reader.getDescription());
            } catch (IOException | VorbisCommentReaderException e2) {
                Log.d(TAG, "Unable to parse vorbis comments of " + file.getUri() + ": " + e2.getMessage());
            }
        }
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
