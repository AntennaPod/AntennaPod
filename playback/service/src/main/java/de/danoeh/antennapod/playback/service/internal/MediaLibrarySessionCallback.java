package de.danoeh.antennapod.playback.service.internal;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.utils.MediaConstants;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.CommandButton;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaSession;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionCommands;
import androidx.media3.session.SessionError;
import androidx.media3.session.SessionResult;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.playback.service.MediaItemAdapter;
import de.danoeh.antennapod.playback.service.R;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.Collections;
import java.util.List;

public class MediaLibrarySessionCallback implements MediaLibraryService.MediaLibrarySession.Callback {
    private static final String TAG = "M3SessionCallback";
    private static final int MAX_ITEMS_PER_LIST = 100;

    private static final String MEDIA_ID_ROOT = "root";
    private static final String MEDIA_ID_QUEUE = "queue";
    private static final String MEDIA_ID_DOWNLOADS = "downloads";
    private static final String MEDIA_ID_EPISODES = "episodes";
    private static final String MEDIA_ID_SUBSCRIPTIONS = "subscriptions";
    private static final ImmutableList<String> BROWSABLE_MEDIA_IDS = ImmutableList.of(
            MEDIA_ID_ROOT, MEDIA_ID_QUEUE, MEDIA_ID_DOWNLOADS, MEDIA_ID_EPISODES, MEDIA_ID_SUBSCRIPTIONS);

    protected static final SessionCommand SESSION_COMMAND_REWIND
            = new SessionCommand("rewind", Bundle.EMPTY);
    protected static final SessionCommand SESSION_COMMAND_FAST_FORWARD
            = new SessionCommand("fast_forward", Bundle.EMPTY);
    protected static final SessionCommand SESSION_COMMAND_PLAYBACK_SPEED
            = new SessionCommand("playback_speed", Bundle.EMPTY);
    protected static final SessionCommand SESSION_COMMAND_SKIP_TO_NEXT
            = new SessionCommand("skip_to_next", Bundle.EMPTY);
    protected static final SessionCommand SESSION_COMMAND_NEXT_CHAPTER
            = new SessionCommand("next_chapter", Bundle.EMPTY);

    private final Context context;
    private Disposable mediaLoaderDisposable;

    public MediaLibrarySessionCallback(Context context) {
        this.context = context;
    }

    @Override
    @NonNull
    @UnstableApi
    public MediaSession.ConnectionResult onConnect(@NonNull MediaSession session,
                                                   @NonNull MediaSession.ControllerInfo controller) {
        SessionCommands sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()
                .add(SESSION_COMMAND_REWIND)
                .add(SESSION_COMMAND_FAST_FORWARD)
                .add(SESSION_COMMAND_PLAYBACK_SPEED)
                .add(SESSION_COMMAND_SKIP_TO_NEXT)
                .add(SESSION_COMMAND_NEXT_CHAPTER)
                .build();
        Player.Commands playerCommands = new Player.Commands.Builder()
                .addAllCommands()
                .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                .build();
        return new MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setCustomLayout(buildCustomLayout())
                .setAvailablePlayerCommands(playerCommands)
                .build();
    }

    @Override
    @UnstableApi
    public void onPostConnect(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller) {
        session.setCustomLayout(buildCustomLayout());
    }

    @UnstableApi
    private ImmutableList<CommandButton> buildCustomLayout() {
        ImmutableList.Builder<CommandButton> buttons = ImmutableList.builder();

        buttons.add(new CommandButton.Builder()
                .setSessionCommand(SESSION_COMMAND_REWIND)
                .setIconResId(R.drawable.ic_notification_fast_rewind)
                .setDisplayName(context.getString(R.string.rewind_label))
                .build());

        buttons.add(new CommandButton.Builder()
                .setSessionCommand(SESSION_COMMAND_FAST_FORWARD)
                .setIconResId(R.drawable.ic_notification_fast_forward)
                .setDisplayName(context.getString(R.string.fast_forward_label))
                .build());

        if (UserPreferences.showPlaybackSpeedOnFullNotification()) {
            buttons.add(new CommandButton.Builder()
                    .setSessionCommand(SESSION_COMMAND_PLAYBACK_SPEED)
                    .setIconResId(R.drawable.ic_notification_playback_speed)
                    .setDisplayName(context.getString(R.string.playback_speed))
                    .build());
        }

        if (UserPreferences.showNextChapterOnFullNotification()) {
            buttons.add(new CommandButton.Builder()
                    .setSessionCommand(SESSION_COMMAND_NEXT_CHAPTER)
                    .setIconResId(R.drawable.ic_notification_next_chapter)
                    .setDisplayName(context.getString(R.string.next_chapter))
                    .build());
        }

        if (UserPreferences.showSkipOnFullNotification()) {
            buttons.add(new CommandButton.Builder()
                    .setSessionCommand(SESSION_COMMAND_SKIP_TO_NEXT)
                    .setIconResId(R.drawable.ic_notification_skip)
                    .setDisplayName(context.getString(R.string.skip_episode_label))
                    .build());
        }

        return buttons.build();
    }

    @Override
    @NonNull
    @UnstableApi
    public ListenableFuture<SessionResult> onCustomCommand(@NonNull MediaSession session,
                                                           @NonNull MediaSession.ControllerInfo controller,
                                                           @NonNull SessionCommand customCommand,
                                                           @NonNull Bundle args) {
        if (customCommand.customAction.equals(SESSION_COMMAND_REWIND.customAction)) {
            session.getPlayer().seekBack();
            return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
        } else if (customCommand.customAction.equals(SESSION_COMMAND_FAST_FORWARD.customAction)) {
            session.getPlayer().seekForward();
            return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
        }
        return Futures.immediateFuture(new SessionResult(SessionError.ERROR_NOT_SUPPORTED));
    }

    @Override
    @NonNull
    @UnstableApi
    public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onSetMediaItems(
            @NonNull MediaSession mediaSession, @NonNull MediaSession.ControllerInfo controller,
            @NonNull List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
        // Uncomment to make playing from search work but position restoration not
        // return MediaLibrarySession.Callback.super.onSetMediaItems(
        //     mediaSession, controller, mediaItems, startIndex, startPositionMs);

        int index = startIndex == C.INDEX_UNSET ? 0 : startIndex;
        MediaSession.MediaItemsWithStartPosition fallbackResult =
                new MediaSession.MediaItemsWithStartPosition(mediaItems, index, startPositionMs);

        if (mediaItems.isEmpty()) {
            return Futures.immediateFuture(fallbackResult);
        }
        long mediaId;
        try {
            mediaId = Long.parseLong(mediaItems.get(index).mediaId);
        } catch (NumberFormatException e) {
            return Futures.immediateFuture(fallbackResult);
        }

        if (mediaLoaderDisposable != null) {
            mediaLoaderDisposable.dispose();
        }
        SettableFuture<MediaSession.MediaItemsWithStartPosition> future = SettableFuture.create();
        mediaLoaderDisposable = Single.fromCallable(() -> DBReader.getFeedMedia(mediaId))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                        media -> {
                            MediaSession.MediaItemsWithStartPosition result =
                                    new MediaSession.MediaItemsWithStartPosition(mediaItems, index,
                                            media.getPosition() > 0 ? media.getPosition() :
                                                    (startPositionMs > 0 ? startPositionMs : 0));
                            future.set(result);
                        },
                        error -> {
                            Log.e(TAG, "Failed to load media with id " + mediaId, error);
                            future.set(fallbackResult);
                        }
                );
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<List<MediaItem>> onAddMediaItems(@NonNull MediaSession mediaSession,
            @NonNull MediaSession.ControllerInfo controller, List<MediaItem> mediaItems) {

        if (mediaItems.isEmpty()) {
            return Futures.immediateFuture(Collections.emptyList());
        }
        long mediaId;
        try {
            mediaId = Long.parseLong(mediaItems.get(0).mediaId);
        } catch (NumberFormatException e) {
            return Futures.immediateFuture(Collections.emptyList());
        }

        if (mediaLoaderDisposable != null) {
            mediaLoaderDisposable.dispose();
        }
        SettableFuture<List<MediaItem>> future = SettableFuture.create();
        mediaLoaderDisposable = Single.fromCallable(() -> DBReader.getFeedMedia(mediaId))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                        media -> future.set(Collections.singletonList(MediaItemAdapter.fromPlayable(media))),
                        error -> {
                            Log.e(TAG, "Failed to load media with id " + mediaId, error);
                            future.set(Collections.emptyList());
                        }
                );
        return future;
    }

    @UnstableApi
    @Override
    @NonNull
    public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onPlaybackResumption(
            @NonNull MediaSession mediaSession, @NonNull MediaSession.ControllerInfo controller) {
        SettableFuture<MediaSession.MediaItemsWithStartPosition> future = SettableFuture.create();
        mediaLoaderDisposable = Single.fromCallable(() ->
                        DBReader.getFeedMedia(PlaybackPreferences.getCurrentlyPlayingFeedMediaId()))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                        media -> {
                            MediaSession.MediaItemsWithStartPosition result =
                                    new MediaSession.MediaItemsWithStartPosition(
                                            Collections.singletonList(MediaItemAdapter.fromPlayable(media)),
                                            0, media.getPosition());
                            future.set(result);
                        },
                        future::setException
                );
        return future;
    }

    @Override
    @NonNull
    @UnstableApi
    public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(
            @NonNull MediaLibraryService.MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser,
            @Nullable MediaLibraryService.LibraryParams params) {
        Bundle rootExtras = new Bundle();
        rootExtras.putBoolean(MediaConstants.BROWSER_SERVICE_EXTRAS_KEY_SEARCH_SUPPORTED, true);
        rootExtras.putBoolean(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, true);
        rootExtras.putBoolean(MediaConstants.BROWSER_SERVICE_EXTRAS_KEY_SEARCH_SUPPORTED, true);
        final MediaLibraryService.LibraryParams libraryParams = new MediaLibraryService.LibraryParams.Builder()
                .setExtras(rootExtras).build();
        if ("com.google.android.googlequicksearchbox".equals(browser.getPackageName())) {
            // Android Auto "for you" screen
        }
        return Futures.immediateFuture(LibraryResult.ofItem(createBrowsableMediaItem(MEDIA_ID_ROOT), libraryParams));
    }

    @Override
    @NonNull
    public ListenableFuture<LibraryResult<MediaItem>> onGetItem(
            @NonNull MediaLibraryService.MediaLibrarySession session,
            @NonNull MediaSession.ControllerInfo browser, @NonNull String mediaId) {
        if (BROWSABLE_MEDIA_IDS.contains(mediaId) || mediaId.startsWith("FeedId:")) {
            SettableFuture<LibraryResult<MediaItem>> future = SettableFuture.create();
            mediaLoaderDisposable = Single.fromCallable(() -> createBrowsableMediaItem(mediaId))
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(item -> future.set(LibraryResult.ofItem(item, null)),
                            future::setException);
            return future;
        }
        return MediaLibraryService.MediaLibrarySession.Callback.super.onGetItem(session, browser, mediaId);
    }

    @Override
    @NonNull
    public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(
            @NonNull MediaLibraryService.MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser,
            @NonNull String parentId, int page, int pageSize, @Nullable MediaLibraryService.LibraryParams params) {
        SettableFuture<LibraryResult<ImmutableList<MediaItem>>> future = SettableFuture.create();

        if (MEDIA_ID_ROOT.equals(parentId)) {
            mediaLoaderDisposable = Single.fromCallable(() -> ImmutableList.of(
                            createBrowsableMediaItem(MEDIA_ID_QUEUE),
                            createBrowsableMediaItem(MEDIA_ID_DOWNLOADS),
                            createBrowsableMediaItem(MEDIA_ID_EPISODES),
                            createBrowsableMediaItem(MEDIA_ID_SUBSCRIPTIONS)))
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(items -> future.set(LibraryResult.ofItemList(items, params)),
                            future::setException);
            return future;
        } else if (MEDIA_ID_SUBSCRIPTIONS.equals(parentId)) {
            mediaLoaderDisposable = Single.fromCallable(() -> DBReader.getFeedList())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(items -> {
                                ImmutableList.Builder builder = new ImmutableList.Builder();
                                for (Feed feed : items) {
                                    builder.add(createBrowsableMediaItem("FeedId:" + feed.getId(),
                                            feed.getTitle(), R.drawable.ic_feed_black, null));
                                }
                                future.set(LibraryResult.ofItemList(builder.build(), params));
                            },
                            future::setException);
            return future;
        } else { // Episodes lists
            mediaLoaderDisposable = Single.fromCallable(() -> {
                        if (parentId.startsWith("FeedId:")) {
                            long feedId = Long.parseLong(parentId.split(":")[1]);
                            return DBReader.getFeed(feedId, true, 0, MAX_ITEMS_PER_LIST).getItems();
                        }
                        switch (parentId) {
                            case MEDIA_ID_QUEUE:
                                return DBReader.getQueue();
                            case MEDIA_ID_DOWNLOADS:
                                return DBReader.getEpisodes(0, MAX_ITEMS_PER_LIST,
                                        new FeedItemFilter(FeedItemFilter.DOWNLOADED),
                                        UserPreferences.getDownloadsSortedOrder());
                            case MEDIA_ID_EPISODES:
                                return DBReader.getEpisodes(0, MAX_ITEMS_PER_LIST,
                                        new FeedItemFilter(UserPreferences.getPrefFilterAllEpisodes()),
                                        UserPreferences.getAllEpisodesSortOrder());
                            default:
                                throw new IllegalArgumentException("Unknown parentId: " + parentId);
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(items -> future.set(createItemListResult(items, params)),
                            future::setException);
        }
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetSearchResult(
            @NonNull MediaLibraryService.MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser,
            @NonNull String query, int page, int pageSize, @Nullable MediaLibraryService.LibraryParams params) {
        SettableFuture<LibraryResult<ImmutableList<MediaItem>>> future = SettableFuture.create();
        mediaLoaderDisposable = Single.fromCallable(() ->
                        DBReader.searchFeedItems(0, query, Feed.STATE_SUBSCRIBED))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(items -> future.set(createItemListResult(items, params)),
                        future::setException);
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<LibraryResult<Void>> onSearch(@NonNull MediaLibraryService.MediaLibrarySession session,
              @NonNull MediaSession.ControllerInfo browser, @NonNull String query,
              @Nullable MediaLibraryService.LibraryParams params) {
        return Futures.immediateFuture(LibraryResult.ofVoid());
    }

    private MediaItem createBrowsableMediaItem(String id) {
        if (id.startsWith("FeedId:")) {
            long feedId = Long.parseLong(id.split(":")[1]);
            Feed feed = DBReader.getFeed(feedId, false, 0, 0);
            return createBrowsableMediaItem(id, feed.getTitle(), R.drawable.ic_notification, null);
        }
        switch (id) {
            case MEDIA_ID_ROOT:
                return createBrowsableMediaItem(MEDIA_ID_ROOT,
                        context.getString(R.string.app_name), R.drawable.ic_notification, null);
            case MEDIA_ID_QUEUE: {
                int numEpisodes = DBReader.getTotalEpisodeCount(new FeedItemFilter(FeedItemFilter.QUEUED));
                return createBrowsableMediaItem(MEDIA_ID_QUEUE,
                        context.getString(R.string.queue_label), R.drawable.ic_playlist_play_black,
                        context.getResources().getQuantityString(R.plurals.num_episodes, numEpisodes, numEpisodes));
            }
            case MEDIA_ID_DOWNLOADS: {
                int numEpisodes = DBReader.getTotalEpisodeCount(new FeedItemFilter(FeedItemFilter.DOWNLOADED));
                return createBrowsableMediaItem(MEDIA_ID_DOWNLOADS,
                        context.getString(R.string.downloads_label), R.drawable.ic_download_black,
                        context.getResources().getQuantityString(R.plurals.num_episodes, numEpisodes, numEpisodes));
            }
            case MEDIA_ID_EPISODES: {
                int numEpisodes = DBReader.getTotalEpisodeCount(new FeedItemFilter());
                return createBrowsableMediaItem(MEDIA_ID_EPISODES,
                        context.getString(R.string.episodes_label), R.drawable.ic_feed_black,
                        context.getResources().getQuantityString(R.plurals.num_episodes, numEpisodes, numEpisodes));
            }
            case MEDIA_ID_SUBSCRIPTIONS:
                return createBrowsableMediaItem(MEDIA_ID_SUBSCRIPTIONS,
                        context.getString(R.string.subscriptions_label), R.drawable.ic_subscriptions_black, null);
            default:
                throw new IllegalArgumentException("ID not known: " + id);
        }
    }

    private MediaItem createBrowsableMediaItem(String id, String title,
                                               @DrawableRes int iconResId, @Nullable String subtitle) {
        Uri iconUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.getResources().getResourcePackageName(iconResId))
                .appendPath(context.getResources().getResourceTypeName(iconResId))
                .appendPath(context.getResources().getResourceEntryName(iconResId))
                .build();

        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
        metadataBuilder.setTitle(title);
        metadataBuilder.setArtworkUri(iconUri);
        if (subtitle != null) {
            metadataBuilder.setSubtitle(subtitle);
        }
        metadataBuilder.setIsBrowsable(true);
        metadataBuilder.setIsPlayable(false);
        return new MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(metadataBuilder.build())
                .build();
    }

    private LibraryResult<ImmutableList<MediaItem>> createItemListResult(List<FeedItem> feedItems,
                                                 @Nullable MediaLibraryService.LibraryParams params) {
        ImmutableList.Builder<MediaItem> itemsBuilder = ImmutableList.builder();
        for (FeedItem item : feedItems) {
            itemsBuilder.add(MediaItemAdapter.fromPlayable(item.getMedia()));
        }
        ImmutableList<MediaItem> items = itemsBuilder.build();
        return LibraryResult.ofItemList(items, params);
    }
}
