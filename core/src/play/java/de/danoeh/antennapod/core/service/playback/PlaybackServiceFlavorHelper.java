package de.danoeh.antennapod.core.service.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.danoeh.antennapod.core.event.MessageEvent;
import de.danoeh.antennapod.playback.base.PlaybackServiceMediaPlayer;
import de.danoeh.antennapod.playback.base.PlayerStatus;
import de.danoeh.antennapod.playback.cast.CastPsmp;
import org.greenrobot.eventbus.EventBus;

/**
 * Class intended to work along PlaybackService and provide support for different flavors.
 */
public class PlaybackServiceFlavorHelper {
    public static final String TAG = "PlaybackSrvFlavorHelper";

    /**
     * Time in seconds during which the CastManager will try to reconnect to the Cast Device after
     * the Wifi Connection is regained.
     */
    private static final int RECONNECTION_ATTEMPT_PERIOD_S = 15;
    /**
     * Stores the state of the cast playback just before it disconnects.
     */
    private volatile PlaybackServiceMediaPlayer.PSMPInfo infoBeforeCastDisconnection;

    private boolean wifiConnectivity = true;
    private BroadcastReceiver wifiBroadcastReceiver;

    //private PlaybackService.FlavorHelperCallback callback;

    //PlaybackServiceFlavorHelper(Context context, PlaybackService.FlavorHelperCallback callback) {
    //    this.callback = callback;
    //    setCastConsumer(context);
    //}

//    void initializeMediaPlayer(Context context) {
//        //if (!CastManager.isInitialized()) {
//            callback.setMediaPlayer(new LocalPSMP(context, callback.getMediaPlayerCallback()));
//        //    return;
//        //}
//        //castManager = CastManager.getInstance();
//        //castManager.addCastConsumer(castConsumer);
//        //boolean isCasting = castManager.isConnected();
//        //callback.setIsCasting(isCasting);
//        //if (isCasting) {
//        //    if (UserPreferences.isCastEnabled()) {
//        //        onCastAppConnected(context, false);
//        //    } else {
//        //        castManager.disconnect();
//        //    }
//        //} else {
//        //    callback.setMediaPlayer(new LocalPSMP(context, callback.getMediaPlayerCallback()));
//        //}
//    }
//
//    void removeCastConsumer() {
//        //if (!CastManager.isInitialized()) {
//        //    return;
//        //}
//        //castManager.removeCastConsumer(castConsumer);
//    }
//
//    boolean castDisconnect(boolean castDisconnect) {
//        //if (!CastManager.isInitialized()) {
//        //    return false;
//        //}
//        //if (castDisconnect) {
//        //    castManager.disconnect();
//        //}
//        return castDisconnect;
//    }
//
//    boolean onMediaPlayerInfo(Context context, int code, @StringRes int resourceId) {
//        switch (code) {
//            case CastPsmp.CAST_ERROR:
//                EventBus.getDefault().post(new MessageEvent(context.getString(resourceId)));
//                return true;
//            case CastPsmp.CAST_ERROR_PRIORITY_HIGH:
//                Toast.makeText(context, resourceId, Toast.LENGTH_SHORT).show();
//                return true;
//            default:
//                return false;
//        }
//    }
//
//    private void setCastConsumer(Context context) {
//        /*castConsumer = new DefaultCastConsumer() {
//            @Override
//            public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId, boolean wasLaunched) {
//                onCastAppConnected(context, wasLaunched);
//            }
//
//            @Override
//            public void onDisconnectionReason(int reason) {
//                Log.d(TAG, "onDisconnectionReason() with code " + reason);
//                // This is our final chance to update the underlying stream position
//                // In onDisconnected(), the underlying CastPlayback#mVideoCastConsumer
//                // is disconnected and hence we update our local value of stream position
//                // to the latest position.
//                PlaybackServiceMediaPlayer mediaPlayer = callback.getMediaPlayer();
//                if (mediaPlayer != null) {
//                    callback.saveCurrentPosition(true, null, PlaybackServiceMediaPlayer.INVALID_TIME);
//                    infoBeforeCastDisconnection = mediaPlayer.getPSMPInfo();
//                    if (reason != BaseCastManager.DISCONNECT_REASON_EXPLICIT &&
//                            infoBeforeCastDisconnection.playerStatus == PlayerStatus.PLAYING) {
//                        // If it's NOT based on user action, we shouldn't automatically resume local playback
//                        infoBeforeCastDisconnection.playerStatus = PlayerStatus.PAUSED;
//                    }
//                }
//            }
//
//            @Override
//            public void onDisconnected() {
//                Log.d(TAG, "onDisconnected()");
//                callback.setIsCasting(false);
//                PlaybackServiceMediaPlayer.PSMPInfo info = infoBeforeCastDisconnection;
//                infoBeforeCastDisconnection = null;
//                PlaybackServiceMediaPlayer mediaPlayer = callback.getMediaPlayer();
//                if (info == null && mediaPlayer != null) {
//                    info = mediaPlayer.getPSMPInfo();
//                }
//                if (info == null) {
//                    info = new PlaybackServiceMediaPlayer.PSMPInfo(PlayerStatus.INDETERMINATE,
//                            PlayerStatus.STOPPED, null);
//                }
//                switchMediaPlayer(new LocalPSMP(context, callback.getMediaPlayerCallback()),
//                        info, true);
//                if (info.playable != null) {
//                    callback.sendNotificationBroadcast(PlaybackService.NOTIFICATION_TYPE_RELOAD,
//                            info.playable.getMediaType() == MediaType.AUDIO ?
//                                    PlaybackService.EXTRA_CODE_AUDIO : PlaybackService.EXTRA_CODE_VIDEO);
//                } else {
//                    Log.d(TAG, "Cast session disconnected, but no current media");
//                    callback.sendNotificationBroadcast(PlaybackService.NOTIFICATION_TYPE_PLAYBACK_END, 0);
//                }
//                // hardware volume buttons control the local device volume
//                mediaRouter.setMediaSessionCompat(null);
//                unregisterWifiBroadcastReceiver();
//                callback.setupNotification(false, info);
//            }
//        };*/
//    }
//
//    private void onCastAppConnected(Context context, boolean wasLaunched) {
//        Log.d(TAG, "A cast device application was " + (wasLaunched ? "launched" : "joined"));
//        callback.setIsCasting(true);
//        PlaybackServiceMediaPlayer.PSMPInfo info = null;
//        PlaybackServiceMediaPlayer mediaPlayer = callback.getMediaPlayer();
//        if (mediaPlayer != null) {
//            info = mediaPlayer.getPSMPInfo();
//            if (info.playerStatus == PlayerStatus.PLAYING) {
//                // could be pause, but this way we make sure the new player will get the correct position,
//                // since pause runs asynchronously and we could be directing the new player to play even before
//                // the old player gives us back the position.
//                callback.saveCurrentPosition(true, null, PlaybackServiceMediaPlayer.INVALID_TIME);
//            }
//        }
//        if (info == null) {
//            info = new PlaybackServiceMediaPlayer.PSMPInfo(PlayerStatus.INDETERMINATE, PlayerStatus.STOPPED, null);
//        }
//        callback.sendNotificationBroadcast(PlaybackService.NOTIFICATION_TYPE_RELOAD,
//                PlaybackService.EXTRA_CODE_CAST);
//        CastPsmp remotePSMP = new CastPsmp(context, callback.getMediaPlayerCallback());
//        switchMediaPlayer(remotePSMP, info, wasLaunched);
//        remotePSMP.init();
//        // hardware volume buttons control the remote device volume
//        //mediaRouter.setMediaSessionCompat(callback.getMediaSession());
//        registerWifiBroadcastReceiver();
//        callback.setupNotification(true, info);
//    }
//
//    private void switchMediaPlayer(@NonNull PlaybackServiceMediaPlayer newPlayer,
//                                   @NonNull PlaybackServiceMediaPlayer.PSMPInfo info,
//                                   boolean wasLaunched) {
//        PlaybackServiceMediaPlayer mediaPlayer = callback.getMediaPlayer();
//        if (mediaPlayer != null) {
//            try {
//                mediaPlayer.stopPlayback(false).get(2, TimeUnit.SECONDS);
//            } catch (InterruptedException | ExecutionException | TimeoutException e) {
//                Log.e(TAG, "There was a problem stopping playback while switching media players", e);
//            }
//            mediaPlayer.shutdownQuietly();
//        }
//        mediaPlayer = newPlayer;
//        callback.setMediaPlayer(mediaPlayer);
//        Log.d(TAG, "switched to " + mediaPlayer.getClass().getSimpleName());
//        if (!wasLaunched) {
//            PlaybackServiceMediaPlayer.PSMPInfo candidate = mediaPlayer.getPSMPInfo();
//            if (candidate.playable != null &&
//                    candidate.playerStatus.isAtLeast(PlayerStatus.PREPARING)) {
//                // do not automatically send new media to cast device
//                info.playable = null;
//            }
//        }
//        if (info.playable != null) {
//            mediaPlayer.playMediaObject(info.playable,
//                    !info.playable.localFileAvailable(),
//                    info.playerStatus == PlayerStatus.PLAYING,
//                    info.playerStatus.isAtLeast(PlayerStatus.PREPARING));
//        }
//    }
//
//    void registerWifiBroadcastReceiver() {
//        /*if (!CastManager.isInitialized()) {
//            return;
//        }
//        if (wifiBroadcastReceiver != null) {
//            return;
//        }
//        wifiBroadcastReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
//                    NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
//                    boolean isConnected = info.isConnected();
//                    //apparently this method gets called twice when a change happens, but one run is enough.
//                    if (isConnected && !wifiConnectivity) {
//                        wifiConnectivity = true;
//                        castManager.startCastDiscovery();
//                        castManager.reconnectSessionIfPossible(RECONNECTION_ATTEMPT_PERIOD_S, NetworkUtils.getWifiSsid());
//                    } else {
//                        wifiConnectivity = isConnected;
//                    }
//                }
//            }
//        };
//        callback.registerReceiver(wifiBroadcastReceiver,
//                new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));*/
//    }
//
//    void unregisterWifiBroadcastReceiver() {
//        /*if (!CastManager.isInitialized()) {
//            return;
//        }
//        if (wifiBroadcastReceiver != null) {
//            callback.unregisterReceiver(wifiBroadcastReceiver);
//            wifiBroadcastReceiver = null;
//        }*/
//    }
//
//    boolean onSharedPreference(String key) {
//        /*if (!CastManager.isInitialized()) {
//            return false;
//        }
//        if (UserPreferences.PREF_CAST_ENABLED.equals(key)) {
//            if (!UserPreferences.isCastEnabled()) {
//                if (castManager.isConnecting() || castManager.isConnected()) {
//                    Log.d(TAG, "Disconnecting cast device due to a change in user preferences");
//                    castManager.disconnect();
//                }
//            }
//            return true;
//        }*/
//        return false;
//    }
//
//    void sessionStateAddActionForWear(PlaybackStateCompat.Builder sessionState, String actionName, CharSequence name, int icon) {
//        /*if (!CastManager.isInitialized()) {
//            return;
//        }
//        PlaybackStateCompat.CustomAction.Builder actionBuilder =
//            new PlaybackStateCompat.CustomAction.Builder(actionName, name, icon);
//        Bundle actionExtras = new Bundle();
//        actionExtras.putBoolean(MediaControlConstants.EXTRA_CUSTOM_ACTION_SHOW_ON_WEAR, true);
//        actionBuilder.setExtras(actionExtras);
//
//        sessionState.addCustomAction(actionBuilder.build());*/
//    }
//
//    void mediaSessionSetExtraForWear(MediaSessionCompat mediaSession) {
//        /*if (!CastManager.isInitialized()) {
//            return;
//        }
//        Bundle sessionExtras = new Bundle();
//        sessionExtras.putBoolean(MediaControlConstants.EXTRA_RESERVE_SLOT_SKIP_TO_PREVIOUS, true);
//        sessionExtras.putBoolean(MediaControlConstants.EXTRA_RESERVE_SLOT_SKIP_TO_NEXT, true);
//        mediaSession.setExtras(sessionExtras);*/
//    }
}
