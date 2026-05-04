package de.danoeh.antennapod;

import android.util.Log;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import de.danoeh.antennapod.playback.service.PlaybackService;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import com.google.android.gms.wearable.WearableListenerService;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import androidx.media3.common.MediaItem;
import android.content.Intent;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.sync.wearinterface.WearDataPaths;
import de.danoeh.antennapod.net.sync.wearinterface.WearSerializer;
import de.danoeh.antennapod.playback.service.PlaybackServiceStarter;
import de.danoeh.antennapod.storage.database.DBReader;

import java.util.List;

public class WearListenerService extends WearableListenerService {
    private static final String TAG = "WearListenerService";
    private static final int MAX_ITEMS = 100;

    @Override
    public void onMessageReceived(MessageEvent event) {
        String path = event.getPath();
        String sourceNodeId = event.getSourceNodeId();
        Log.d(TAG, "Message received: " + path + " from " + sourceNodeId);
        Completable.fromAction(() -> handleMessage(path, sourceNodeId))
                .subscribeOn(Schedulers.computation())
                .subscribe(
                        () -> { },
                        throwable -> Log.e(TAG, "Failed to handle wearable message: " + path, throwable));
    }

    private void handleMessage(String path, String sourceNodeId) {
        switch (path) {
            case WearDataPaths.NOW_PLAYING:
                FeedMedia media = DBReader.getFeedMedia(PlaybackPreferences.getCurrentlyPlayingFeedMediaId());
                if (!PlaybackService.isRunning) {
                    reply(sourceNodeId, WearDataPaths.NOW_PLAYING,
                            WearSerializer.nowPlayingToBytes(media.getItem(), false));
                    return;
                }
                PlaybackController.bindToMedia3Service(this, controller -> {
                    media.setPosition((int) controller.getCurrentPosition());
                    if (controller.getDuration() > 0) {
                        media.setDuration((int) controller.getDuration());
                    }
                    reply(sourceNodeId, WearDataPaths.NOW_PLAYING,
                            WearSerializer.nowPlayingToBytes(media.getItem(), true));
                });
                break;
            case WearDataPaths.PAUSE:
                PlaybackController.bindToMedia3Service(this, controller -> controller.pause());
                break;
            case WearDataPaths.QUEUE:
                reply(sourceNodeId, path, WearSerializer.episodesToBytes(DBReader.getQueue()));
                break;
            case WearDataPaths.DOWNLOADS:
                reply(sourceNodeId, path, WearSerializer.episodesToBytes(DBReader.getEpisodes(0, MAX_ITEMS,
                        new FeedItemFilter(FeedItemFilter.DOWNLOADED), SortOrder.DATE_NEW_OLD)));
                break;
            case WearDataPaths.EPISODES:
                reply(sourceNodeId, path, WearSerializer.episodesToBytes(DBReader.getEpisodes(0, MAX_ITEMS,
                        FeedItemFilter.unfiltered(), SortOrder.DATE_NEW_OLD)));
                break;
            case WearDataPaths.SUBSCRIPTIONS:
                reply(sourceNodeId, path, WearSerializer.feedsToBytes(DBReader.getFeedList()));
                break;
            default:
                if (path.startsWith(WearDataPaths.PLAY_PREFIX)) {
                    try {
                        long itemId = Long.parseLong(path.substring(WearDataPaths.PLAY_PREFIX.length()));
                        playItem(itemId);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Ignoring malformed play path: " + path, e);
                    }
                } else if (path.startsWith(WearDataPaths.FEED_EPISODES_PREFIX)) {
                    try {
                        long feedId = Long.parseLong(path.substring(WearDataPaths.FEED_EPISODES_PREFIX.length()));
                        Feed feed = DBReader.getFeed(feedId, false, 0, MAX_ITEMS);
                        List<FeedItem> feedItems = feed != null ? feed.getItems() : java.util.Collections.emptyList();
                        reply(sourceNodeId, path, WearSerializer.episodesToBytes(feedItems));
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Ignoring malformed feed episodes path: " + path, e);
                    }
                } else if (path.startsWith(WearDataPaths.OPEN_ON_PHONE_PREFIX)) {
                    try {
                        long itemId = Long.parseLong(path.substring(WearDataPaths.OPEN_ON_PHONE_PREFIX.length()));
                        Intent intent = new MainActivityStarter(this).withOpenEpisode(itemId).getIntent();
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Ignoring malformed open on phone path: " + path, e);
                    }
                } else {
                    Log.d(TAG, "Ignoring unknown path: " + path);
                }
                break;
        }
    }

    private void playItem(long itemId) {
        FeedItem item = DBReader.getFeedItem(itemId);
        if (item == null || item.getMedia() == null) {
            Log.w(TAG, "Item or media not found for id " + itemId);
            return;
        }
        Log.d(TAG, "Starting playback for: " + item.getTitle());
        new PlaybackServiceStarter(this, item.getMedia())
                .callEvenIfRunning(true)
                .start();
    }

    private void reply(String nodeId, String path, byte[] payload) {
        Log.d(TAG, "Sending reply to " + nodeId + " at " + path + " (" + payload.length + " bytes)");
        Wearable.getMessageClient(this).sendMessage(nodeId, path, payload)
                .addOnSuccessListener(id -> Log.d(TAG, "Reply sent: " + path))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to send reply to " + path, e));
    }
}
