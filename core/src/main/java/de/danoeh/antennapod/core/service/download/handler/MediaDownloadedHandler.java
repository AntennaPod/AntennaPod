package de.danoeh.antennapod.core.service.download.handler;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutionException;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetEpisodeAction;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.ChapterUtils;
import de.danoeh.antennapod.core.util.DownloadError;

/**
 * Handles a completed media download.
 */
public class MediaDownloadedHandler implements Runnable {
    private static final String TAG = "MediaDownloadedHandler";
    private final DownloadRequest request;
    private final DownloadStatus status;
    private final Context context;
    private DownloadStatus updatedStatus;

    public MediaDownloadedHandler(@NonNull Context context, @NonNull DownloadStatus status,
                                  @NonNull DownloadRequest request) {
        this.status = status;
        this.request = request;
        this.context = context;
    }

    @Override
    public void run() {
        updatedStatus = status;
        FeedMedia media = DBReader.getFeedMedia(request.getFeedfileId());
        if (media == null) {
            Log.e(TAG, "Could not find downloaded media object in database");
            return;
        }
        media.setDownloaded(true);
        media.setFile_url(request.getDestination());
        media.checkEmbeddedPicture(); // enforce check

        // check if file has chapters
        if (media.getItem() != null && !media.getItem().hasChapters()) {
            ChapterUtils.loadChaptersFromFileUrl(media);
        }

        // Get duration
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        String durationStr = null;
        try {
            mmr.setDataSource(media.getFile_url());
            durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            media.setDuration(Integer.parseInt(durationStr));
            Log.d(TAG, "Duration of file is " + media.getDuration());
        } catch (NumberFormatException e) {
            Log.d(TAG, "Invalid file duration: " + durationStr);
        } catch (Exception e) {
            Log.e(TAG, "Get duration failed", e);
        } finally {
            mmr.release();
        }

        final FeedItem item = media.getItem();

        try {
            DBWriter.setFeedMedia(media).get();

            // we've received the media, we don't want to autodownload it again
            if (item != null) {
                item.setAutoDownload(false);
                // setFeedItem() signals (via EventBus) that the item has been updated,
                // so we do it after the enclosing media has been updated above,
                // to ensure subscribers will get the updated FeedMedia as well
                DBWriter.setFeedItem(item).get();
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "MediaHandlerThread was interrupted");
        } catch (ExecutionException e) {
            Log.e(TAG, "ExecutionException in MediaHandlerThread: " + e.getMessage());
            updatedStatus = new DownloadStatus(media, media.getEpisodeTitle(),
                    DownloadError.ERROR_DB_ACCESS_ERROR, false, e.getMessage());
        }


        if (GpodnetPreferences.loggedIn() && item != null) {
            GpodnetEpisodeAction action = new GpodnetEpisodeAction.Builder(item, GpodnetEpisodeAction.Action.DOWNLOAD)
                    .currentDeviceId()
                    .currentTimestamp()
                    .build();
            GpodnetPreferences.enqueueEpisodeAction(action);
        }
    }

    public DownloadStatus getUpdatedStatus() {
        return updatedStatus;
    }
}
