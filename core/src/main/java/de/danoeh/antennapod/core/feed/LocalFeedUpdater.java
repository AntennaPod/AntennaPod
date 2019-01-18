package de.danoeh.antennapod.core.feed;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.core.storage.DBTasks;

public class LocalFeedUpdater {

    private static long getFileDuration(File f) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(f.getAbsolutePath());
        String durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Long.parseLong(durationStr);
    }

    /** Starts the import process. */
    public static void startImport(Uri uri, Context context) {
        String fileUrl = uri.toString();
        Feed theDummyFeed = new Feed("someDummyUrl", null, "Dummy feed (local files)");

        File f = new File(uri.getPath());

        //create item
        long globalId = 0;
        FeedItem item = new FeedItem(globalId, "Dummy Feed: Item " + f.getName(), "item" + Long.toString(new Date().getTime()),
                fileUrl, new Date(), FeedItem.UNPLAYED, theDummyFeed);
        item.setAutoDownload(false);

        //add the media to the item
        long duration = getFileDuration(f);
        long size = f.length();
        FeedMedia media = new FeedMedia(0, item, (int)duration, 0, size, "audio/mp3", f.getAbsolutePath(), f.getAbsolutePath(), true, null, 0, 0);
        item.setMedia(media);

        //add to the feed
        List<FeedItem> items = new ArrayList<>();
        items.add(item);
        theDummyFeed.setItems(items);

        //add or merge to the db
        Feed[] feeds = DBTasks.updateFeed(context, theDummyFeed);
    }

}
