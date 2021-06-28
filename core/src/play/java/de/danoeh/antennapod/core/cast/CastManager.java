/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ------------------------------------------------------------------------
 *
 * Changes made by Domingos Lopes <domingos86lopes@gmail.com>
 *
 * original can be found at http://www.github.com/googlecast/CastCompanionLibrary-android
 */

package de.danoeh.antennapod.core.cast;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.view.ActionProvider;
import androidx.core.view.MenuItemCompat;
import androidx.mediarouter.media.MediaRouter;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.libraries.cast.companionlibrary.cast.BaseCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.OnFailedListener;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;

import static com.google.android.gms.cast.RemoteMediaPlayer.RESUME_STATE_PLAY;
import static com.google.android.gms.cast.RemoteMediaPlayer.RESUME_STATE_UNCHANGED;

/**
 * A subclass of {@link BaseCastManager} that is suitable for casting video contents (it
 * also provides a single custom data channel/namespace if an out-of-band communication is
 * needed).
 * <p>
 * Clients need to initialize this class by calling
 * {@link #init(android.content.Context)} in the Application's
 * {@code onCreate()} method. To access the (singleton) instance of this class, clients
 * need to call {@link #getInstance()}.
 * <p>This
 * class manages various states of the remote cast device. Client applications, however, can
 * complement the default behavior of this class by hooking into various callbacks that it provides
 * (see {@link CastConsumer}).
 * Since the number of these callbacks is usually much larger than what a single application might
 * be interested in, there is a no-op implementation of this interface (see
 * {@link DefaultCastConsumer}) that applications can subclass to override only those methods that
 * they are interested in. Since this library depends on the cast functionalities provided by the
 * Google Play services, the library checks to ensure that the right version of that service is
 * installed. It also provides a simple static method {@code checkGooglePlayServices()} that clients
 * can call at an early stage of their applications to provide a dialog for users if they need to
 * update/activate their Google Play Services library.
 *
 * @see CastConfiguration
 */
public class CastManager extends BaseCastManager implements OnFailedListener {
    public static final String TAG = "CastManager";

    public static final String CAST_APP_ID = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;

    private MediaStatus mediaStatus;
    private static CastManager INSTANCE;
    private RemoteMediaPlayer remoteMediaPlayer;
    private int state = MediaStatus.PLAYER_STATE_IDLE;
    private final Set<CastConsumer> castConsumers = new CopyOnWriteArraySet<>();

    public static final int QUEUE_OPERATION_LOAD = 1;
    public static final int QUEUE_OPERATION_APPEND = 9;

    private CastManager(Context context, CastConfiguration castConfiguration) {
        super(context, castConfiguration);
        Log.d(TAG, "CastManager is instantiated");
    }

    public static synchronized CastManager init(Context context) {
        if (INSTANCE == null) {
            CastConfiguration castConfiguration = new CastConfiguration.Builder(CAST_APP_ID)
                    .enableDebug()
                    .enableAutoReconnect()
                    .enableWifiReconnection()
                    .setLaunchOptions(true, Locale.getDefault())
                    .setMediaRouteDialogFactory(ClientConfig.castCallbacks.getMediaRouterDialogFactory())
                    .build();
            Log.d(TAG, "New instance of CastManager is created");
            if (ConnectionResult.SUCCESS != GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(context)) {
                Log.e(TAG, "Couldn't find the appropriate version of Google Play Services");
            }
            INSTANCE = new CastManager(context, castConfiguration);
        }
        return INSTANCE;
    }

    /**
     * Returns a (singleton) instance of this class. Clients should call this method in order to
     * get a hold of this singleton instance, only after it is initialized. If it is not initialized
     * yet, an {@link IllegalStateException} will be thrown.
     *
     */
    public static CastManager getInstance() {
        if (INSTANCE == null) {
            String msg = "No CastManager instance was found, did you forget to initialize it?";
            Log.e(TAG, msg);
            throw new IllegalStateException(msg);
        }
        return INSTANCE;
    }

    public static boolean isInitialized() {
        return INSTANCE != null;
    }

    /**
     * Returns the active {@link RemoteMediaPlayer} instance. Since there are a number of media
     * control APIs that this library do not provide a wrapper for, client applications can call
     * those methods directly after obtaining an instance of the active {@link RemoteMediaPlayer}.
     */
    public final RemoteMediaPlayer getRemoteMediaPlayer() {
        return remoteMediaPlayer;
    }

    /*
     * A simple check to make sure remoteMediaPlayer is not null
     */
    private void checkRemoteMediaPlayerAvailable() throws NoConnectionException {
        if (remoteMediaPlayer == null) {
            throw new NoConnectionException();
        }
    }

    /**
     * Indicates if the remote media is currently playing (or buffering).
     *
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public boolean isRemoteMediaPlaying() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        return state == MediaStatus.PLAYER_STATE_BUFFERING
                || state == MediaStatus.PLAYER_STATE_PLAYING;
    }

    /**
     * Returns <code>true</code> if the remote connected device is playing a movie.
     *
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public boolean isRemoteMediaPaused() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        return state == MediaStatus.PLAYER_STATE_PAUSED;
    }

    /**
     * Returns <code>true</code> only if there is a media on the remote being played, paused or
     * buffered.
     *
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public boolean isRemoteMediaLoaded() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        return isRemoteMediaPaused() || isRemoteMediaPlaying();
    }

    /**
     * Gets the remote's system volume. It internally detects what type of volume is used.
     *
     * @throws NoConnectionException If no connectivity to the device exists
     * @throws TransientNetworkDisconnectionException If framework is still trying to recover from
     * a possibly transient loss of network
     */
    public double getStreamVolume() throws TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        checkRemoteMediaPlayerAvailable();
        return remoteMediaPlayer.getMediaStatus().getStreamVolume();
    }

    /**
     * Sets the stream volume.
     *
     * @param volume Should be a value between 0 and 1, inclusive.
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     * @throws CastException If setting system volume fails
     */
    public void setStreamVolume(double volume) throws CastException,
            TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        if (volume > 1.0) {
            volume = 1.0;
        } else if (volume < 0) {
            volume = 0.0;
        }

        RemoteMediaPlayer mediaPlayer = getRemoteMediaPlayer();
        if (mediaPlayer == null) {
            throw new NoConnectionException();
        }
        mediaPlayer.setStreamVolume(mApiClient, volume).setResultCallback(
                (result) -> {
                    if (!result.getStatus().isSuccess()) {
                        onFailed(R.string.cast_failed_setting_volume,
                                result.getStatus().getStatusCode());
                    } else {
                        CastManager.this.onStreamVolumeChanged();
                    }
                });
    }

    /**
     * Returns <code>true</code> if remote Stream is muted.
     *
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public boolean isStreamMute() throws TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        checkRemoteMediaPlayerAvailable();
        return remoteMediaPlayer.getMediaStatus().isMute();
    }

    /**
     * Returns the duration of the media that is loaded, in milliseconds.
     *
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public long getMediaDuration() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        checkRemoteMediaPlayerAvailable();
        return remoteMediaPlayer.getStreamDuration();
    }

    /**
     * Returns the current (approximate) position of the current media, in milliseconds.
     *
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public long getCurrentMediaPosition() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        checkRemoteMediaPlayerAvailable();
        return remoteMediaPlayer.getApproximateStreamPosition();
    }

    public int getApplicationStandbyState() throws IllegalStateException {
        Log.d(TAG, "getApplicationStandbyState()");
        return Cast.CastApi.getStandbyState(mApiClient);
    }

    private void onApplicationDisconnected(int errorCode) {
        Log.d(TAG, "onApplicationDisconnected() reached with error code: " + errorCode);
        mApplicationErrorCode = errorCode;
        for (CastConsumer consumer : castConsumers) {
            consumer.onApplicationDisconnected(errorCode);
        }
        if (mMediaRouter != null) {
            Log.d(TAG, "onApplicationDisconnected(): Cached RouteInfo: " + getRouteInfo());
            Log.d(TAG, "onApplicationDisconnected(): Selected RouteInfo: "
                    + mMediaRouter.getSelectedRoute());
            if (getRouteInfo() == null || mMediaRouter.getSelectedRoute().equals(getRouteInfo())) {
                Log.d(TAG, "onApplicationDisconnected(): Setting route to default");
                mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
            }
        }
        onDeviceSelected(null /* CastDevice */, null /* RouteInfo */);
    }

    private void onApplicationStatusChanged() {
        if (!isConnected()) {
            return;
        }
        try {
            String appStatus = Cast.CastApi.getApplicationStatus(mApiClient);
            Log.d(TAG, "onApplicationStatusChanged() reached: " + appStatus);
            for (CastConsumer consumer : castConsumers) {
                consumer.onApplicationStatusChanged(appStatus);
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "onApplicationStatusChanged()", e);
        }
    }

    private void onDeviceVolumeChanged() {
        Log.d(TAG, "onDeviceVolumeChanged() reached");
        double volume;
        try {
            volume = getDeviceVolume();
            boolean isMute = isDeviceMute();
            for (CastConsumer consumer : castConsumers) {
                consumer.onVolumeChanged(volume, isMute);
            }
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Failed to get volume", e);
        }

    }

    private void onStreamVolumeChanged() {
        Log.d(TAG, "onStreamVolumeChanged() reached");
        double volume;
        try {
            volume = getStreamVolume();
            boolean isMute = isStreamMute();
            for (CastConsumer consumer : castConsumers) {
                consumer.onStreamVolumeChanged(volume, isMute);
            }
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Failed to get volume", e);
        }
    }

    @Override
    protected void onApplicationConnected(ApplicationMetadata appMetadata,
                                          String applicationStatus, String sessionId, boolean wasLaunched) {
        Log.d(TAG, "onApplicationConnected() reached with sessionId: " + sessionId
                + ", and mReconnectionStatus=" + mReconnectionStatus);
        mApplicationErrorCode = NO_APPLICATION_ERROR;
        if (mReconnectionStatus == RECONNECTION_STATUS_IN_PROGRESS) {
            // we have tried to reconnect and successfully launched the app, so
            // it is time to select the route and make the cast icon happy :-)
            List<MediaRouter.RouteInfo> routes = mMediaRouter.getRoutes();
            if (routes != null) {
                String routeId = mPreferenceAccessor.getStringFromPreference(PREFS_KEY_ROUTE_ID);
                for (MediaRouter.RouteInfo routeInfo : routes) {
                    if (routeId.equals(routeInfo.getId())) {
                        // found the right route
                        Log.d(TAG, "Found the correct route during reconnection attempt");
                        mReconnectionStatus = RECONNECTION_STATUS_FINALIZED;
                        mMediaRouter.selectRoute(routeInfo);
                        break;
                    }
                }
            }
        }
        try {
            //attachDataChannel();
            attachMediaChannel();
            mSessionId = sessionId;
            // saving device for future retrieval; we only save the last session info
            mPreferenceAccessor.saveStringToPreference(PREFS_KEY_SESSION_ID, mSessionId);
            remoteMediaPlayer.requestStatus(mApiClient).setResultCallback(result -> {
                if (!result.getStatus().isSuccess()) {
                    onFailed(R.string.cast_failed_status_request,
                            result.getStatus().getStatusCode());
                }
            });
            for (CastConsumer consumer : castConsumers) {
                consumer.onApplicationConnected(appMetadata, mSessionId, wasLaunched);
            }
        } catch (TransientNetworkDisconnectionException e) {
            Log.e(TAG, "Failed to attach media/data channel due to network issues", e);
            onFailed(R.string.cast_failed_no_connection_trans, NO_STATUS_CODE);
        } catch (NoConnectionException e) {
            Log.e(TAG, "Failed to attach media/data channel due to network issues", e);
            onFailed(R.string.cast_failed_no_connection, NO_STATUS_CODE);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.google.android.libraries.cast.companionlibrary.cast.BaseCastManager
     * #onConnectivityRecovered()
     */
    @Override
    public void onConnectivityRecovered() {
        reattachMediaChannel();
        //reattachDataChannel();
        super.onConnectivityRecovered();
    }

    /*
     * (non-Javadoc)
     * @see com.google.android.gms.cast.CastClient.Listener#onApplicationStopFailed (int)
     */
    @Override
    public void onApplicationStopFailed(int errorCode) {
        for (CastConsumer consumer : castConsumers) {
            consumer.onApplicationStopFailed(errorCode);
        }
    }

    @Override
    public void onApplicationConnectionFailed(int errorCode) {
        Log.d(TAG, "onApplicationConnectionFailed() reached with errorCode: " + errorCode);
        mApplicationErrorCode = errorCode;
        if (mReconnectionStatus == RECONNECTION_STATUS_IN_PROGRESS) {
            if (errorCode == CastStatusCodes.APPLICATION_NOT_RUNNING) {
                // while trying to re-establish session, we found out that the app is not running
                // so we need to disconnect
                mReconnectionStatus = RECONNECTION_STATUS_INACTIVE;
                onDeviceSelected(null /* CastDevice */, null /* RouteInfo */);
            }
        } else {
            for (CastConsumer consumer : castConsumers) {
                consumer.onApplicationConnectionFailed(errorCode);
            }
            onDeviceSelected(null /* CastDevice */, null /* RouteInfo */);
            if (mMediaRouter != null) {
                Log.d(TAG, "onApplicationConnectionFailed(): Setting route to default");
                mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
            }
        }
    }

    /**
     * Loads a media. For this to succeed, you need to have successfully launched the application.
     *
     * @param media The media to be loaded
     * @param autoPlay If <code>true</code>, playback starts after load
     * @param position Where to start the playback (only used if autoPlay is <code>true</code>.
     * Units is milliseconds.
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void loadMedia(MediaInfo media, boolean autoPlay, int position)
            throws TransientNetworkDisconnectionException, NoConnectionException {
        loadMedia(media, autoPlay, position, null);
    }

    /**
     * Loads a media. For this to succeed, you need to have successfully launched the application.
     *
     * @param media The media to be loaded
     * @param autoPlay If <code>true</code>, playback starts after load
     * @param position Where to start the playback (only used if autoPlay is <code>true</code>).
     * Units is milliseconds.
     * @param customData Optional {@link JSONObject} data to be passed to the cast device
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void loadMedia(MediaInfo media, boolean autoPlay, int position, JSONObject customData)
            throws TransientNetworkDisconnectionException, NoConnectionException {
        loadMedia(media, null, autoPlay, position, customData);
    }

    /**
     * Loads a media. For this to succeed, you need to have successfully launched the application.
     *
     * @param media The media to be loaded
     * @param activeTracks An array containing the list of track IDs to be set active for this
     * media upon a successful load
     * @param autoPlay If <code>true</code>, playback starts after load
     * @param position Where to start the playback (only used if autoPlay is <code>true</code>).
     * Units is milliseconds.
     * @param customData Optional {@link JSONObject} data to be passed to the cast device
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void loadMedia(MediaInfo media, final long[] activeTracks, boolean autoPlay,
                          int position, JSONObject customData)
            throws TransientNetworkDisconnectionException, NoConnectionException {
        Log.d(TAG, "loadMedia");
        checkConnectivity();
        if (media == null) {
            return;
        }
        if (remoteMediaPlayer == null) {
            Log.e(TAG, "Trying to load a video with no active media session");
            throw new NoConnectionException();
        }

        Log.d(TAG, "remoteMediaPlayer.load() with media=" + media.getMetadata().getString(MediaMetadata.KEY_TITLE)
                + ", position=" + position + ", autoplay=" + autoPlay);
        remoteMediaPlayer.load(mApiClient, media, autoPlay, position, activeTracks, customData)
                .setResultCallback(result -> {
                    for (CastConsumer consumer : castConsumers) {
                        consumer.onMediaLoadResult(result.getStatus().getStatusCode());
                    }
                });
    }

    /**
     * Loads and optionally starts playback of a new queue of media items.
     *
     * @param items Array of items to load, in the order that they should be played. Must not be
     *              {@code null} or empty.
     * @param startIndex The array index of the item in the {@code items} array that should be
     *                   played first (i.e., it will become the currentItem).If {@code repeatMode}
     *                   is {@link MediaStatus#REPEAT_MODE_REPEAT_OFF} playback will end when the
     *                   last item in the array is played.
     *                   <p>
     *                   This may be useful for continuation scenarios where the user was already
     *                   using the sender application and in the middle decides to cast. This lets
     *                   the sender application avoid mapping between the local and remote queue
     *                   positions and/or avoid issuing an extra request to update the queue.
     *                   <p>
     *                   This value must be less than the length of {@code items}.
     * @param repeatMode The repeat playback mode for the queue. One of
     *                   {@link MediaStatus#REPEAT_MODE_REPEAT_OFF},
     *                   {@link MediaStatus#REPEAT_MODE_REPEAT_ALL},
     *                   {@link MediaStatus#REPEAT_MODE_REPEAT_SINGLE} and
     *                   {@link MediaStatus#REPEAT_MODE_REPEAT_ALL_AND_SHUFFLE}.
     * @param customData Custom application-specific data to pass along with the request, may be
     *                   {@code null}.
     * @throws TransientNetworkDisconnectionException
     * @throws NoConnectionException
     */
    public void queueLoad(final MediaQueueItem[] items, final int startIndex, final int repeatMode,
                          final JSONObject customData)
            throws TransientNetworkDisconnectionException, NoConnectionException {
        Log.d(TAG, "queueLoad");
        checkConnectivity();
        if (items == null || items.length == 0) {
            return;
        }
        if (remoteMediaPlayer == null) {
            Log.e(TAG, "Trying to queue one or more videos with no active media session");
            throw new NoConnectionException();
        }
        Log.d(TAG, "remoteMediaPlayer.queueLoad() with " + items.length + "items, starting at "
                + startIndex);
        remoteMediaPlayer
                .queueLoad(mApiClient, items, startIndex, repeatMode, customData)
                .setResultCallback(result -> {
                    for (CastConsumer consumer : castConsumers) {
                        consumer.onMediaQueueOperationResult(QUEUE_OPERATION_LOAD,
                                result.getStatus().getStatusCode());
                    }
                });
    }

    /**
     * Plays the loaded media.
     *
     * @param position Where to start the playback. Units is milliseconds.
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void play(int position) throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        Log.d(TAG, "attempting to play media at position " + position + " seconds");
        if (remoteMediaPlayer == null) {
            Log.e(TAG, "Trying to play a video with no active media session");
            throw new NoConnectionException();
        }
        seekAndPlay(position);
    }

    /**
     * Resumes the playback from where it was left (can be the beginning).
     *
     * @param customData Optional {@link JSONObject} data to be passed to the cast device
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void play(JSONObject customData) throws
            TransientNetworkDisconnectionException, NoConnectionException {
        Log.d(TAG, "play(customData)");
        checkConnectivity();
        if (remoteMediaPlayer == null) {
            Log.e(TAG, "Trying to play a video with no active media session");
            throw new NoConnectionException();
        }
        remoteMediaPlayer.play(mApiClient, customData)
                .setResultCallback(result -> {
                    if (!result.getStatus().isSuccess()) {
                        onFailed(R.string.cast_failed_to_play,
                                result.getStatus().getStatusCode());
                    }
                });
    }

    /**
     * Resumes the playback from where it was left (can be the beginning).
     *
     * @throws CastException
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void play() throws CastException, TransientNetworkDisconnectionException,
            NoConnectionException {
        play(null);
    }

    /**
     * Stops the playback of media/stream
     *
     * @param customData Optional {@link JSONObject}
     * @throws TransientNetworkDisconnectionException
     * @throws NoConnectionException
     */
    public void stop(JSONObject customData) throws
            TransientNetworkDisconnectionException, NoConnectionException {
        Log.d(TAG, "stop()");
        checkConnectivity();
        if (remoteMediaPlayer == null) {
            Log.e(TAG, "Trying to stop a stream with no active media session");
            throw new NoConnectionException();
        }
        remoteMediaPlayer.stop(mApiClient, customData).setResultCallback(
                result -> {
                    if (!result.getStatus().isSuccess()) {
                        onFailed(R.string.cast_failed_to_stop,
                                result.getStatus().getStatusCode());
                    }
                }
        );
    }

    /**
     * Stops the playback of media/stream
     *
     * @throws CastException
     * @throws TransientNetworkDisconnectionException
     * @throws NoConnectionException
     */
    public void stop() throws CastException,
            TransientNetworkDisconnectionException, NoConnectionException {
        stop(null);
    }

    /**
     * Pauses the playback.
     *
     * @throws CastException
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void pause() throws CastException, TransientNetworkDisconnectionException,
            NoConnectionException {
        pause(null);
    }

    /**
     * Pauses the playback.
     *
     * @param customData Optional {@link JSONObject} data to be passed to the cast device
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void pause(JSONObject customData) throws
            TransientNetworkDisconnectionException, NoConnectionException {
        Log.d(TAG, "attempting to pause media");
        checkConnectivity();
        if (remoteMediaPlayer == null) {
            Log.e(TAG, "Trying to pause a video with no active media session");
            throw new NoConnectionException();
        }
        remoteMediaPlayer.pause(mApiClient, customData)
                .setResultCallback(result -> {
                    if (!result.getStatus().isSuccess()) {
                        onFailed(R.string.cast_failed_to_pause,
                                result.getStatus().getStatusCode());
                    }
                });
    }

    /**
     * Seeks to the given point without changing the state of the player, i.e. after seek is
     * completed, it resumes what it was doing before the start of seek.
     *
     * @param position in milliseconds
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void seek(int position) throws TransientNetworkDisconnectionException,
            NoConnectionException {
        Log.d(TAG, "attempting to seek media");
        checkConnectivity();
        if (remoteMediaPlayer == null) {
            Log.e(TAG, "Trying to seek a video with no active media session");
            throw new NoConnectionException();
        }
        Log.d(TAG, "remoteMediaPlayer.seek() to position " + position);
        remoteMediaPlayer.seek(mApiClient, position, RESUME_STATE_UNCHANGED).setResultCallback(result -> {
            if (!result.getStatus().isSuccess()) {
                onFailed(R.string.cast_failed_seek, result.getStatus().getStatusCode());
            }
        });
    }

    /**
     * Fast forwards the media by the given amount. If {@code lengthInMillis} is negative, it
     * rewinds the media.
     *
     * @param lengthInMillis The amount to fast forward the media, given in milliseconds
     * @throws TransientNetworkDisconnectionException
     * @throws NoConnectionException
     */
    public void forward(int lengthInMillis) throws TransientNetworkDisconnectionException,
            NoConnectionException {
        Log.d(TAG, "forward(): attempting to forward media by " + lengthInMillis);
        checkConnectivity();
        if (remoteMediaPlayer == null) {
            Log.e(TAG, "Trying to seek a video with no active media session");
            throw new NoConnectionException();
        }
        long position = remoteMediaPlayer.getApproximateStreamPosition() + lengthInMillis;
        seek((int) position);
    }

    /**
     * Seeks to the given point and starts playback regardless of the starting state.
     *
     * @param position in milliseconds
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void seekAndPlay(int position) throws TransientNetworkDisconnectionException,
            NoConnectionException {
        Log.d(TAG, "attempting to seek media");
        checkConnectivity();
        if (remoteMediaPlayer == null) {
            Log.e(TAG, "Trying to seekAndPlay a video with no active media session");
            throw new NoConnectionException();
        }
        Log.d(TAG, "remoteMediaPlayer.seek() to position " + position + "and play");
        remoteMediaPlayer.seek(mApiClient, position, RESUME_STATE_PLAY)
                .setResultCallback(result -> {
                    if (!result.getStatus().isSuccess()) {
                        onFailed(R.string.cast_failed_seek, result.getStatus().getStatusCode());
                    }
                });
    }

    private void attachMediaChannel() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        Log.d(TAG, "attachMediaChannel()");
        checkConnectivity();
        if (remoteMediaPlayer == null) {
            remoteMediaPlayer = new RemoteMediaPlayer();

            remoteMediaPlayer.setOnStatusUpdatedListener(
                    () -> {
                        Log.d(TAG, "RemoteMediaPlayer::onStatusUpdated() is reached");
                        CastManager.this.onRemoteMediaPlayerStatusUpdated();
                    }
            );

            remoteMediaPlayer.setOnPreloadStatusUpdatedListener(
                    () -> {
                        Log.d(TAG, "RemoteMediaPlayer::onPreloadStatusUpdated() is reached");
                        CastManager.this.onRemoteMediaPreloadStatusUpdated();
                    });


            remoteMediaPlayer.setOnMetadataUpdatedListener(
                    () -> {
                        Log.d(TAG, "RemoteMediaPlayer::onMetadataUpdated() is reached");
                        CastManager.this.onRemoteMediaPlayerMetadataUpdated();
                    }
            );

            remoteMediaPlayer.setOnQueueStatusUpdatedListener(
                    () -> {
                        Log.d(TAG, "RemoteMediaPlayer::onQueueStatusUpdated() is reached");
                        mediaStatus = remoteMediaPlayer.getMediaStatus();
                        if (mediaStatus != null
                                && mediaStatus.getQueueItems() != null) {
                            List<MediaQueueItem> queueItems = mediaStatus
                                    .getQueueItems();
                            int itemId = mediaStatus.getCurrentItemId();
                            MediaQueueItem item = mediaStatus
                                    .getQueueItemById(itemId);
                            int repeatMode = mediaStatus.getQueueRepeatMode();
                            onQueueUpdated(queueItems, item, repeatMode, false);
                        } else {
                            onQueueUpdated(null, null,
                                    MediaStatus.REPEAT_MODE_REPEAT_OFF, false);
                        }
                    });

        }
        try {
            Log.d(TAG, "Registering MediaChannel namespace");
            Cast.CastApi.setMessageReceivedCallbacks(mApiClient, remoteMediaPlayer.getNamespace(),
                    remoteMediaPlayer);
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "attachMediaChannel()", e);
        }
    }

    private void reattachMediaChannel() {
        if (remoteMediaPlayer != null && mApiClient != null) {
            try {
                Log.d(TAG, "Registering MediaChannel namespace");
                Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                        remoteMediaPlayer.getNamespace(), remoteMediaPlayer);
            } catch (IOException | IllegalStateException e) {
                Log.e(TAG, "reattachMediaChannel()", e);
            }
        }
    }

    private void detachMediaChannel() {
        Log.d(TAG, "trying to detach media channel");
        if (remoteMediaPlayer != null) {
            try {
                Cast.CastApi.removeMessageReceivedCallbacks(mApiClient,
                        remoteMediaPlayer.getNamespace());
            } catch (IOException | IllegalStateException e) {
                Log.e(TAG, "detachMediaChannel()", e);
            }
            remoteMediaPlayer = null;
        }
    }

    /**
     * Returns the latest retrieved value for the {@link MediaStatus}. This value is updated
     * whenever the onStatusUpdated callback is called.
     */
    public final MediaStatus getMediaStatus() {
        return mediaStatus;
    }

    /*
     * This is called by onStatusUpdated() of the RemoteMediaPlayer
     */
    private void onRemoteMediaPlayerStatusUpdated() {
        Log.d(TAG, "onRemoteMediaPlayerStatusUpdated() reached");
        if (mApiClient == null || remoteMediaPlayer == null) {
            Log.d(TAG, "mApiClient or remoteMediaPlayer is null, so will not proceed");
            return;
        }
        mediaStatus = remoteMediaPlayer.getMediaStatus();
        if (mediaStatus == null) {
            Log.d(TAG, "MediaStatus is null, so will not proceed");
            return;
        } else {
            List<MediaQueueItem> queueItems = mediaStatus.getQueueItems();
            if (queueItems != null) {
                int itemId = mediaStatus.getCurrentItemId();
                MediaQueueItem item = mediaStatus.getQueueItemById(itemId);
                int repeatMode = mediaStatus.getQueueRepeatMode();
                onQueueUpdated(queueItems, item, repeatMode, false);
            } else {
                onQueueUpdated(null, null, MediaStatus.REPEAT_MODE_REPEAT_OFF, false);
            }
            state = mediaStatus.getPlayerState();
            int idleReason = mediaStatus.getIdleReason();

            if (state == MediaStatus.PLAYER_STATE_PLAYING) {
                Log.d(TAG, "onRemoteMediaPlayerStatusUpdated(): Player status = playing");
            } else if (state == MediaStatus.PLAYER_STATE_PAUSED) {
                Log.d(TAG, "onRemoteMediaPlayerStatusUpdated(): Player status = paused");
            } else if (state == MediaStatus.PLAYER_STATE_IDLE) {
                Log.d(TAG, "onRemoteMediaPlayerStatusUpdated(): Player status = IDLE with reason: "
                        + idleReason);
                if (idleReason == MediaStatus.IDLE_REASON_ERROR) {
                    // something bad happened on the cast device
                    Log.d(TAG, "onRemoteMediaPlayerStatusUpdated(): IDLE reason = ERROR");
                    onFailed(R.string.cast_failed_receiver_player_error, NO_STATUS_CODE);
                }
            } else if (state == MediaStatus.PLAYER_STATE_BUFFERING) {
                Log.d(TAG, "onRemoteMediaPlayerStatusUpdated(): Player status = buffering");
            } else {
                Log.d(TAG, "onRemoteMediaPlayerStatusUpdated(): Player status = unknown");
            }
        }
        for (CastConsumer consumer : castConsumers) {
            consumer.onRemoteMediaPlayerStatusUpdated();
        }
        if (mediaStatus != null) {
            double volume = mediaStatus.getStreamVolume();
            boolean isMute = mediaStatus.isMute();
            for (CastConsumer consumer : castConsumers) {
                consumer.onStreamVolumeChanged(volume, isMute);
            }
        }
    }

    private void onRemoteMediaPreloadStatusUpdated() {
        MediaQueueItem item = null;
        mediaStatus = remoteMediaPlayer.getMediaStatus();
        if (mediaStatus != null) {
            item = mediaStatus.getQueueItemById(mediaStatus.getPreloadedItemId());
        }
        Log.d(TAG, "onRemoteMediaPreloadStatusUpdated() " + item);
        for (CastConsumer consumer : castConsumers) {
            consumer.onRemoteMediaPreloadStatusUpdated(item);
        }
    }

    /*
    * This is called by onQueueStatusUpdated() of RemoteMediaPlayer
    */
    private void onQueueUpdated(List<MediaQueueItem> queueItems, MediaQueueItem item,
                                int repeatMode, boolean shuffle) {
        Log.d(TAG, "onQueueUpdated() reached");
        Log.d(TAG, String.format(Locale.US, "Queue Items size: %d, Item: %s, Repeat Mode: %d, Shuffle: %s",
                queueItems == null ? 0 : queueItems.size(), item, repeatMode, shuffle));
        for (CastConsumer consumer : castConsumers) {
            consumer.onMediaQueueUpdated(queueItems, item, repeatMode, shuffle);
        }
    }

    /*
     * This is called by onMetadataUpdated() of RemoteMediaPlayer
     */
    public void onRemoteMediaPlayerMetadataUpdated() {
        Log.d(TAG, "onRemoteMediaPlayerMetadataUpdated() reached");
        for (CastConsumer consumer : castConsumers) {
            consumer.onRemoteMediaPlayerMetadataUpdated();
        }
    }

    /**
     * Registers a {@link CastConsumer} interface with this class.
     * Registered listeners will be notified of changes to a variety of
     * lifecycle and media status changes through the callbacks that the interface provides.
     *
     * @see DefaultCastConsumer
     */
    public synchronized void addCastConsumer(CastConsumer listener) {
        if (listener != null) {
            addBaseCastConsumer(listener);
            castConsumers.add(listener);
            Log.d(TAG, "Successfully added the new CastConsumer listener " + listener);
        }
    }

    /**
     * Unregisters a {@link CastConsumer}.
     */
    public synchronized void removeCastConsumer(CastConsumer listener) {
        if (listener != null) {
            removeBaseCastConsumer(listener);
            castConsumers.remove(listener);
        }
    }

    @Override
    protected void onDeviceUnselected() {
        detachMediaChannel();
        //removeDataChannel();
        state = MediaStatus.PLAYER_STATE_IDLE;
        mediaStatus = null;
    }

    @Override
    protected Cast.CastOptions.Builder getCastOptionBuilder(CastDevice device) {
        Cast.CastOptions.Builder builder = new Cast.CastOptions.Builder(mSelectedCastDevice, new CastListener());
        if (isFeatureEnabled(CastConfiguration.FEATURE_DEBUGGING)) {
            builder.setVerboseLoggingEnabled(true);
        }
        return builder;
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        super.onConnectionFailed(result);
        state = MediaStatus.PLAYER_STATE_IDLE;
        mediaStatus = null;
    }

    @Override
    public void onDisconnected(boolean stopAppOnExit, boolean clearPersistedConnectionData,
                               boolean setDefaultRoute) {
        super.onDisconnected(stopAppOnExit, clearPersistedConnectionData, setDefaultRoute);
        state = MediaStatus.PLAYER_STATE_IDLE;
        mediaStatus = null;
    }

    class CastListener extends Cast.Listener {

        /*
         * (non-Javadoc)
         * @see com.google.android.gms.cast.Cast.Listener#onApplicationDisconnected (int)
         */
        @Override
        public void onApplicationDisconnected(int statusCode) {
            CastManager.this.onApplicationDisconnected(statusCode);
        }

        /*
         * (non-Javadoc)
         * @see com.google.android.gms.cast.Cast.Listener#onApplicationStatusChanged ()
         */
        @Override
        public void onApplicationStatusChanged() {
            CastManager.this.onApplicationStatusChanged();
        }

        @Override
        public void onVolumeChanged() {
            CastManager.this.onDeviceVolumeChanged();
        }
    }

    @Override
    public void onFailed(int resourceId, int statusCode) {
        Log.d(TAG, "onFailed: " + mContext.getString(resourceId) + ", code: " + statusCode);
        super.onFailed(resourceId, statusCode);
    }

    /**
     * Checks whether the selected Cast Device has the specified audio or video capabilities.
     *
     * @param capability capability from:
     * <ul>
     *     <li>{@link CastDevice#CAPABILITY_AUDIO_IN}</li>
     *     <li>{@link CastDevice#CAPABILITY_AUDIO_OUT}</li>
     *     <li>{@link CastDevice#CAPABILITY_VIDEO_IN}</li>
     *     <li>{@link CastDevice#CAPABILITY_VIDEO_OUT}</li>
     * </ul>
     * @param defaultVal value to return whenever there's no device selected.
     * @return {@code true} if the selected device has the specified capability,
     * {@code false} otherwise.
     */
    public boolean hasCapability(final int capability, final boolean defaultVal) {
        if (mSelectedCastDevice != null) {
            return mSelectedCastDevice.hasCapability(capability);
        } else {
            return defaultVal;
        }
    }

    /**
     * Adds and wires up the Switchable Media Router cast button. It returns a reference to the
     * {@link SwitchableMediaRouteActionProvider} associated with the button if the caller needs
     * such reference. It is assumed that the enclosing
     * {@link android.app.Activity} inherits (directly or indirectly) from
     * {@link androidx.appcompat.app.AppCompatActivity}.
     *
     * @param menuItem MenuItem of the Media Router cast button.
     */
    public final SwitchableMediaRouteActionProvider addMediaRouterButton(@NonNull MenuItem menuItem) {
        ActionProvider actionProvider = MenuItemCompat.getActionProvider(menuItem);
        if (!(actionProvider instanceof SwitchableMediaRouteActionProvider)) {
            Log.wtf(TAG, "MenuItem provided to addMediaRouterButton() is not compatible with " +
                    "SwitchableMediaRouteActionProvider." +
                    ((actionProvider == null) ? " Its action provider is null!" : ""),
                    new ClassCastException());
            return null;
        }
        SwitchableMediaRouteActionProvider mediaRouteActionProvider =
                (SwitchableMediaRouteActionProvider) actionProvider;
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        if (mCastConfiguration.getMediaRouteDialogFactory() != null) {
            mediaRouteActionProvider.setDialogFactory(mCastConfiguration.getMediaRouteDialogFactory());
        }
        return mediaRouteActionProvider;
    }

    /* (non-Javadoc)
     * These methods startReconnectionService and stopReconnectionService simply override the ones
     * from BaseCastManager with empty implementations because we handle the service ourselves, but
     * need to allow BaseCastManager to save current network information.
     */
    @Override
    protected void startReconnectionService(long mediaDurationLeft) {
        // Do nothing
    }

    @Override
    protected void stopReconnectionService() {
        // Do nothing
    }
}
