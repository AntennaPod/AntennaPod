package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.util.Pair;
import androidx.media.utils.MediaConstants;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
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
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.playback.service.R;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.Collections;
import java.util.List;

public class MediaLibrarySessionCallback implements MediaLibraryService.MediaLibrarySession.Callback {
    private static final String TAG = "M3SessionCallback";
    private static final String MEDIA_ID_ROOT = "root";
    private static final String MEDIA_ID_QUEUE = "queue";
    private static final String MEDIA_ID_DOWNLOADS = "downloads";
    private static final String MEDIA_ID_EPISODES = "episodes";
    private static final String MEDIA_ID_SUBSCRIPTIONS = "subscriptions";
    private static final String MEDIA_ID_CURRENT = "current";
    private static final ImmutableList<String> BROWSABLE_MEDIA_IDS = ImmutableList.of(
            MEDIA_ID_ROOT, MEDIA_ID_QUEUE, MEDIA_ID_DOWNLOADS, MEDIA_ID_EPISODES,
            MEDIA_ID_SUBSCRIPTIONS, MEDIA_ID_CURRENT);

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
    private final CompositeDisposable disposables = new CompositeDisposable();

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

        buttons.add(new CommandButton.Builder(CommandButton.ICON_REWIND)
                .setSessionCommand(SESSION_COMMAND_REWIND)
                .setDisplayName(context.getString(R.string.rewind_label))
                .build());

        buttons.add(new CommandButton.Builder(CommandButton.ICON_FAST_FORWARD)
                .setSessionCommand(SESSION_COMMAND_FAST_FORWARD)
                .setDisplayName(context.getString(R.string.fast_forward_label))
                .build());

        if (UserPreferences.showPlaybackSpeedOnFullNotification()) {
            buttons.add(new CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                    .setSessionCommand(SESSION_COMMAND_PLAYBACK_SPEED)
                    .setCustomIconResId(R.drawable.ic_notification_playback_speed)
                    .setDisplayName(context.getString(R.string.playback_speed))
                    .build());
        }

        if (UserPreferences.showNextChapterOnFullNotification()) {
            buttons.add(new CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                    .setSessionCommand(SESSION_COMMAND_NEXT_CHAPTER)
                    .setCustomIconResId(R.drawable.ic_notification_next_chapter)
                    .setDisplayName(context.getString(R.string.next_chapter))
                    .build());
        }

        if (UserPreferences.showSkipOnFullNotification()) {
            buttons.add(new CommandButton.Builder(CommandButton.ICON_NEXT)
                    .setSessionCommand(SESSION_COMMAND_SKIP_TO_NEXT)
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
        int index = startIndex == C.INDEX_UNSET ? 0 : startIndex;
        if (mediaItems.isEmpty()) {
            return Futures.immediateFuture(new MediaSession.MediaItemsWithStartPosition(
                    mediaItems, index, startPositionMs));
        }
        SettableFuture<MediaSession.MediaItemsWithStartPosition> future = SettableFuture.create();
        disposables.add(Single.fromCallable(
                () -> {
                    List<MediaItem> updatedItems = onAddMediaItems(mediaSession, controller, mediaItems).get();
                    long mediaId = Long.parseLong(updatedItems.get(index).mediaId);
                    FeedMedia mediaDetails = DBReader.getFeedMedia(mediaId);
                    return new Pair<>(updatedItems, mediaDetails);
                })
                .subscribeOn(Schedulers.io())
                .subscribe(result ->
                                future.set(new MediaSession.MediaItemsWithStartPosition(result.first, index,
                                        result.second.getPosition() > 0 ? result.second.getPosition() :
                                                (startPositionMs > 0 ? startPositionMs : 0))),
                        error -> {
                            Log.e(TAG, "Failed to load media", error);
                            future.set(new MediaSession.MediaItemsWithStartPosition(
                                    mediaItems, index, startPositionMs));
                        }
                ));
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<List<MediaItem>> onAddMediaItems(@NonNull MediaSession mediaSession,
            @NonNull MediaSession.ControllerInfo controller, @NonNull List<MediaItem> mediaItems) {

        if (mediaItems.isEmpty()) {
            return Futures.immediateFuture(Collections.emptyList());
        }
        long mediaId;
        try {
            mediaId = Long.parseLong(mediaItems.get(0).mediaId);
        } catch (NumberFormatException e) {
            return Futures.immediateFuture(Collections.emptyList());
        }

        SettableFuture<List<MediaItem>> future = SettableFuture.create();
        disposables.add(Single.fromCallable(() -> DBReader.getFeedMedia(mediaId))
                .subscribeOn(Schedulers.io())
                .subscribe(
                        media -> future.set(Collections.singletonList(MediaItemAdapter.fromPlayable(media))),
                        error -> {
                            Log.e(TAG, "Failed to load media with id " + mediaId, error);
                            future.set(Collections.emptyList());
                        }
                ));
        return future;
    }

    @UnstableApi
    @Override
    @NonNull
    public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onPlaybackResumption(
            @NonNull MediaSession mediaSession, @NonNull MediaSession.ControllerInfo controller) {
        SettableFuture<MediaSession.MediaItemsWithStartPosition> future = SettableFuture.create();
        disposables.add(Single.fromCallable(() ->
                        DBReader.getFeedMedia(PlaybackPreferences.getCurrentlyPlayingFeedMediaId()))
                .subscribeOn(Schedulers.io())
                .subscribe(
                        media -> {
                            MediaSession.MediaItemsWithStartPosition result =
                                    new MediaSession.MediaItemsWithStartPosition(
                                            Collections.singletonList(MediaItemAdapter.fromPlayable(media)),
                                            0, media.getPosition());
                            future.set(result);
                        },
                        future::setException
                ));
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
        final MediaLibraryService.LibraryParams libraryParams = new MediaLibraryService.LibraryParams.Builder()
                .setExtras(rootExtras).build();
        if ("com.google.android.googlequicksearchbox".equals(browser.getPackageName())) {
            // Android Auto "for you" screen
            return Futures.immediateFuture(LibraryResult.ofItem(
                    createBrowsableMediaItem(MEDIA_ID_CURRENT), libraryParams));
        }
        return Futures.immediateFuture(LibraryResult.ofItem(createBrowsableMediaItem(MEDIA_ID_ROOT), libraryParams));
    }

    @Override
    @NonNull
    public ListenableFuture<LibraryResult<MediaItem>> onGetItem(
            @NonNull MediaLibraryService.MediaLibrarySession session,
            @NonNull MediaSession.ControllerInfo browser, @NonNull String mediaId) {
        if (BROWSABLE_MEDIA_IDS.contains(mediaId) || mediaId.startsWith(MediaItemAdapter.MEDIA_ID_FEED_PREFIX)) {
            SettableFuture<LibraryResult<MediaItem>> future = SettableFuture.create();
            disposables.add(Single.fromCallable(() -> createBrowsableMediaItem(mediaId))
                    .subscribeOn(Schedulers.io())
                    .subscribe(item -> future.set(LibraryResult.ofItem(item, null)),
                            future::setException));
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

        switch (parentId) {
            case MEDIA_ID_ROOT:
                disposables.add(Single.fromCallable(() -> ImmutableList.of(
                                createBrowsableMediaItem(MEDIA_ID_CURRENT),
                                createBrowsableMediaItem(MEDIA_ID_QUEUE),
                                createBrowsableMediaItem(MEDIA_ID_DOWNLOADS),
                                createBrowsableMediaItem(MEDIA_ID_EPISODES),
                                createBrowsableMediaItem(MEDIA_ID_SUBSCRIPTIONS)))
                        .subscribeOn(Schedulers.io())
                        .subscribe(items -> future.set(LibraryResult.ofItemList(items, params)),
                                future::setException));
                return future;
            case MEDIA_ID_SUBSCRIPTIONS:
                disposables.add(Single.fromCallable(DBReader::getFeedList)
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                items -> {
                                    ImmutableList.Builder<MediaItem> builder = new ImmutableList.Builder<>();
                                    for (Feed feed : items) {
                                        if (feed.getState() == Feed.STATE_SUBSCRIBED) {
                                            builder.add(MediaItemAdapter.fromFeed(feed));
                                        }
                                    }
                                    future.set(LibraryResult.ofItemList(builder.build(), params));
                                },
                                future::setException));
                return future;
            case MEDIA_ID_CURRENT:
                disposables.add(Single.fromCallable(() ->
                                DBReader.getFeedMedia(PlaybackPreferences.getCurrentlyPlayingFeedMediaId()))
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                media -> {
                                    future.set(LibraryResult.ofItemList(
                                            ImmutableList.of(MediaItemAdapter.fromPlayable(media)), params));
                                },
                                error -> {
                                    Log.e(TAG, "Failed to load currently playing media", error);
                                    future.set(LibraryResult.ofItemList(ImmutableList.of(), params));
                                }
                        ));
                return future;
            default: // Episodes lists
                disposables.add(Single.fromCallable(() -> {
                    if (parentId.startsWith(MediaItemAdapter.MEDIA_ID_FEED_PREFIX)) {
                        long feedId = Long.parseLong(parentId.split(":")[1]);
                        return DBReader.getFeed(feedId, true, page * pageSize, pageSize).getItems();
                    }
                    return switch (parentId) {
                        case MEDIA_ID_QUEUE -> DBReader.getQueue();
                        case MEDIA_ID_DOWNLOADS -> DBReader.getEpisodes(page * pageSize, pageSize,
                                new FeedItemFilter(FeedItemFilter.DOWNLOADED),
                                UserPreferences.getDownloadsSortedOrder());
                        case MEDIA_ID_EPISODES -> DBReader.getEpisodes(page * pageSize, pageSize,
                                new FeedItemFilter(UserPreferences.getPrefFilterAllEpisodes()),
                                UserPreferences.getAllEpisodesSortOrder());
                        default -> throw new IllegalArgumentException("Unknown parentId: " + parentId);
                    };
                })
                        .subscribeOn(Schedulers.io())
                        .subscribe(items -> future.set(LibraryResult.ofItemList(
                                        MediaItemAdapter.fromItemList(items), params)),
                                future::setException));
                return future;
        }
    }

    @Override
    @NonNull
    public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetSearchResult(
            @NonNull MediaLibraryService.MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser,
            @NonNull String query, int page, int pageSize, @Nullable MediaLibraryService.LibraryParams params) {
        SettableFuture<LibraryResult<ImmutableList<MediaItem>>> future = SettableFuture.create();
        disposables.add(Single.fromCallable(() ->
                        DBReader.searchFeedItems(0, query, Feed.STATE_SUBSCRIBED))
                .subscribeOn(Schedulers.io())
                .subscribe(items -> future.set(LibraryResult.ofItemList(
                                MediaItemAdapter.fromItemList(items), params)),
                        future::setException));
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<LibraryResult<Void>> onSearch(@NonNull MediaLibraryService.MediaLibrarySession session,
              @NonNull MediaSession.ControllerInfo browser, @NonNull String query,
              @Nullable MediaLibraryService.LibraryParams params) {
        return Futures.immediateFuture(LibraryResult.ofVoid());
    }

    @WorkerThread
    private MediaItem createBrowsableMediaItem(String id) {
        if (id.startsWith(MediaItemAdapter.MEDIA_ID_FEED_PREFIX)) {
            long feedId = Long.parseLong(id.split(":")[1]);
            Feed feed = DBReader.getFeed(feedId, false, 0, 0);
            return MediaItemAdapter.fromFeed(feed);
        }
        switch (id) {
            case MEDIA_ID_ROOT:
                return MediaItemAdapter.from(context, MEDIA_ID_ROOT,
                        context.getString(R.string.app_name), R.drawable.ic_notification, null);
            case MEDIA_ID_QUEUE: {
                int numEpisodes = DBReader.getTotalEpisodeCount(new FeedItemFilter(FeedItemFilter.QUEUED));
                return MediaItemAdapter.from(context, MEDIA_ID_QUEUE,
                        context.getString(R.string.queue_label), R.drawable.ic_playlist_play_black,
                        context.getResources().getQuantityString(R.plurals.num_episodes, numEpisodes, numEpisodes));
            }
            case MEDIA_ID_DOWNLOADS: {
                int numEpisodes = DBReader.getTotalEpisodeCount(new FeedItemFilter(FeedItemFilter.DOWNLOADED));
                return MediaItemAdapter.from(context, MEDIA_ID_DOWNLOADS,
                        context.getString(R.string.downloads_label), R.drawable.ic_download_black,
                        context.getResources().getQuantityString(R.plurals.num_episodes, numEpisodes, numEpisodes));
            }
            case MEDIA_ID_EPISODES: {
                int numEpisodes = DBReader.getTotalEpisodeCount(new FeedItemFilter());
                return MediaItemAdapter.from(context, MEDIA_ID_EPISODES,
                        context.getString(R.string.episodes_label), R.drawable.ic_feed_black,
                        context.getResources().getQuantityString(R.plurals.num_episodes, numEpisodes, numEpisodes));
            }
            case MEDIA_ID_SUBSCRIPTIONS:
                return MediaItemAdapter.from(context, MEDIA_ID_SUBSCRIPTIONS,
                        context.getString(R.string.subscriptions_label), R.drawable.ic_subscriptions_black, null);
            case MEDIA_ID_CURRENT:
                return MediaItemAdapter.from(context, MEDIA_ID_CURRENT,
                        context.getString(R.string.current_playing_episode), R.drawable.ic_play_48dp_black, null);
            default:
                throw new IllegalArgumentException("ID not known: " + id);
        }
    }
}
