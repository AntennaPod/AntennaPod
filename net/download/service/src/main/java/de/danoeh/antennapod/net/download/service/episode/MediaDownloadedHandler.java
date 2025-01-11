package de.danoeh.antennapod.net.download.service.episode;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import androidx.annotation.NonNull;

import de.danoeh.antennapod.model.MediaMetadataRetrieverCompat;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.ui.chapters.ChapterUtils;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.InterruptedIOException;
import java.util.concurrent.ExecutionException;

import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.model.download.DownloadRequest;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.sync.serviceinterface.EpisodeAction;
import de.danoeh.antennapod.ui.transcript.TranscriptUtils;

/**
 * Handles a completed media download.
 */
public class MediaDownloadedHandler implements Runnable {
    private static final String TAG = "MediaDownloadedHandler";
    private final DownloadRequest request;
    private final Context context;
    private DownloadResult updatedStatus;

    public MediaDownloadedHandler(@NonNull Context context, @NonNull DownloadResult status,
                                  @NonNull DownloadRequest request) {
        this.request = request;
        this.context = context;
        this.updatedStatus = status;
    }

    @Override
    public void run() {
        FeedMedia media = DBReader.getFeedMedia(request.getFeedfileId());
        if (media == null) {
            Log.e(TAG, "Could not find downloaded media object in database");
            return;
        }
        // media.setDownloaded modifies played state
        boolean broadcastUnreadStateUpdate = media.getItem() != null && media.getItem().isNew();
        media.setDownloaded(true, System.currentTimeMillis());
        media.setLocalFileUrl(request.getDestination());
        media.setSize(new File(request.getDestination()).length());
        media.checkEmbeddedPicture(); // enforce check

        try {
            // Cache chapters if file has them
            if (media.getItem() != null && !media.getItem().hasChapters()) {
                media.setChapters(ChapterUtils.loadChaptersFromMediaFile(media, context));
            }
            if (media.getItem() != null && media.getItem().getPodcastIndexChapterUrl() != null) {
                ChapterUtils.loadChaptersFromUrl(media.getItem().getPodcastIndexChapterUrl(), false);
            }
            FeedItem item = media.getItem();
            if (item != null && item.getTranscriptUrl() != null) {
                String transcript = TranscriptUtils.loadTranscriptFromUrl(item.getTranscriptUrl(), true);
                if (!StringUtils.isEmpty(transcript)) {
                    item.setPodcastIndexTranscriptText(transcript);
                    TranscriptUtils.storeTranscript(media, transcript);
                }
            }
        } catch (InterruptedIOException ignore) {
            // Ignore
        }

        // Get duration
        String durationStr = null;
        try (MediaMetadataRetrieverCompat mmr = new MediaMetadataRetrieverCompat()) {
            mmr.setDataSource(media.getLocalFileUrl());
            durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            media.setDuration(Integer.parseInt(durationStr));
            Log.d(TAG, "Duration of file is " + media.getDuration());
        } catch (NumberFormatException e) {
            Log.d(TAG, "Invalid file duration: " + durationStr);
        } catch (Exception e) {
            Log.e(TAG, "Get duration failed", e);
        }

        final FeedItem item = media.getItem();

        try {
            DBWriter.setFeedMedia(media).get();

            // we've received the media, we don't want to autodownload it again
            if (item != null) {
                item.disableAutoDownload();
                // setFeedItem() signals (via EventBus) that the item has been updated,
                // so we do it after the enclosing media has been updated above,
                // to ensure subscribers will get the updated FeedMedia as well
                DBWriter.setFeedItem(item).get();
                if (broadcastUnreadStateUpdate) {
                    EventBus.getDefault().post(new UnreadItemsUpdateEvent());
                }
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "MediaHandlerThread was interrupted");
        } catch (ExecutionException e) {
            Log.e(TAG, "ExecutionException in MediaHandlerThread: " + e.getMessage());
            updatedStatus = new DownloadResult(media.getEpisodeTitle(), media.getId(),
                    FeedMedia.FEEDFILETYPE_FEEDMEDIA, false, DownloadError.ERROR_DB_ACCESS_ERROR, e.getMessage());
        }

        if (item != null && item.getFeed().getState() == Feed.STATE_SUBSCRIBED) {
            SynchronizationQueue.getInstance().enqueueEpisodeAction(
                    new EpisodeAction.Builder(item, EpisodeAction.DOWNLOAD)
                        .currentTimestamp()
                        .build());
        }
    }

    @NonNull
    public DownloadResult getUpdatedStatus() {
        return updatedStatus;
    }
}
