package de.danoeh.antennapod.playback.service;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
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
import androidx.media3.session.SessionResult;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.Collections;
import java.util.List;

public class MediaLibrarySessionCallback implements MediaLibraryService.MediaLibrarySession.Callback {
    private static final String TAG = "M3SessionCallback";
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
        return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED));
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
                .observeOn(AndroidSchedulers.mainThread())
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
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        media -> {
                            future.set(Collections.singletonList(MediaItemAdapter.fromPlayable(media)));
                        },
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
                .observeOn(AndroidSchedulers.mainThread())
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
            // Android Auto for you screen
        }
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
        metadataBuilder.setTitle("AntennaPod");
        metadataBuilder.setIsBrowsable(true);
        metadataBuilder.setIsPlayable(false);
        MediaItem rootItem = new MediaItem.Builder()
                .setMediaId("rootaaa")
                .setMediaMetadata(metadataBuilder.build())
                .build();
        return Futures.immediateFuture(LibraryResult.ofItem(rootItem, libraryParams));
    }

    @Override
    @NonNull
    public ListenableFuture<LibraryResult<MediaItem>> onGetItem(
            @NonNull MediaLibraryService.MediaLibrarySession session,
            @NonNull MediaSession.ControllerInfo browser, @NonNull String mediaId) {
        Log.d("aaaaaa", "aaaaaaaaaaaaaaaaaaaaaaa onGetItem: mediaId=" + mediaId);
        return MediaLibraryService.MediaLibrarySession.Callback.super.onGetItem(session, browser, mediaId);
    }

    @Override
    @NonNull
    public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(
            @NonNull MediaLibraryService.MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser,
            @NonNull String parentId, int page, int pageSize, @Nullable MediaLibraryService.LibraryParams params) {
        Log.d("aaaaaa", "aaaaaaaaaaaaaaaaaaaaaaa onGetChildren: parentId=" + parentId);
        SettableFuture<LibraryResult<ImmutableList<MediaItem>>> future = SettableFuture.create();
        mediaLoaderDisposable = Single.fromCallable(DBReader::getQueue)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        queue -> {
                            ImmutableList.Builder<MediaItem> itemsBuilder = ImmutableList.builder();
                            for (FeedItem item : queue) {
                                itemsBuilder.add(MediaItemAdapter.fromPlayable(item.getMedia()));
                            }
                            ImmutableList<MediaItem> items = itemsBuilder.build();
                            LibraryResult<ImmutableList<MediaItem>> result =
                                    LibraryResult.ofItemList(items, params);
                            future.set(result);
                        },
                        future::setException
                );
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
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        queue -> {
                            ImmutableList.Builder<MediaItem> itemsBuilder = ImmutableList.builder();
                            for (FeedItem item : queue) {
                                itemsBuilder.add(MediaItemAdapter.fromPlayable(item.getMedia()));
                            }
                            ImmutableList<MediaItem> items = itemsBuilder.build();
                            future.set(LibraryResult.ofItemList(items, params));
                        },
                        future::setException
                );
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<LibraryResult<Void>> onSearch(@NonNull MediaLibraryService.MediaLibrarySession session,
              @NonNull MediaSession.ControllerInfo browser, @NonNull String query,
              @Nullable MediaLibraryService.LibraryParams params) {
        return Futures.immediateFuture(LibraryResult.ofVoid());
    }
}
