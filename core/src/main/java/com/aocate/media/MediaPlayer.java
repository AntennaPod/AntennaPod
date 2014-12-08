// Copyright 2011, Aocate, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.aocate.media;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import de.danoeh.antennapod.core.BuildConfig;

public class MediaPlayer {
    public static final String TAG = "com.aocate.media.MediaPlayer";

    public interface OnBufferingUpdateListener {
        public abstract void onBufferingUpdate(MediaPlayer arg0, int percent);
    }

    public interface OnCompletionListener {
        public abstract void onCompletion(MediaPlayer arg0);
    }

    public interface OnErrorListener {
        public abstract boolean onError(MediaPlayer arg0, int what, int extra);
    }

    public interface OnInfoListener {
        public abstract boolean onInfo(MediaPlayer arg0, int what, int extra);
    }

    public interface OnPitchAdjustmentAvailableChangedListener {
        /**
         * @param arg0                     The owning media player
         * @param pitchAdjustmentAvailable True if pitch adjustment is available, false if not
         */
        public abstract void onPitchAdjustmentAvailableChanged(
                MediaPlayer arg0, boolean pitchAdjustmentAvailable);
    }

    public interface OnPreparedListener {
        public abstract void onPrepared(MediaPlayer arg0);
    }

    public interface OnSeekCompleteListener {
        public abstract void onSeekComplete(MediaPlayer arg0);
    }

    public interface OnSpeedAdjustmentAvailableChangedListener {
        /**
         * @param arg0                     The owning media player
         * @param speedAdjustmentAvailable True if speed adjustment is available, false if not
         */
        public abstract void onSpeedAdjustmentAvailableChanged(
                MediaPlayer arg0, boolean speedAdjustmentAvailable);
    }

    public enum State {
        IDLE, INITIALIZED, PREPARED, STARTED, PAUSED, STOPPED, PREPARING, PLAYBACK_COMPLETED, END, ERROR
    }

    private static Uri SPEED_ADJUSTMENT_MARKET_URI = Uri
            .parse("market://details?id=com.aocate.presto");

    private static Intent prestoMarketIntent = null;

    public static final int MEDIA_ERROR_SERVER_DIED = android.media.MediaPlayer.MEDIA_ERROR_SERVER_DIED;
    public static final int MEDIA_ERROR_UNKNOWN = android.media.MediaPlayer.MEDIA_ERROR_UNKNOWN;
    public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = android.media.MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK;

    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     *
     * @param context The application's environment.
     * @param action  The Intent action to check for availability.
     * @return True if an Intent with the specified action can be sent and
     * responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentServices(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    /**
     * Returns an explicit Intent for a service that accepts the given Intent
     * or null if no such service was found.
     *
     * @param context The application's environment.
     * @param action  The Intent action to check for availability.
     * @return The explicit service Intent or null if no service was found.
     */
    public static Intent getPrestoServiceIntent(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent actionIntent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentServices(actionIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (list.size() > 0) {
            ResolveInfo first = list.get(0);
            if (first.serviceInfo != null) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(first.serviceInfo.packageName,
                        first.serviceInfo.name));
                Log.i(TAG, "Returning intent:" + intent.toString());
                return intent;
            } else {
                Log.e(TAG, "Found service that accepts " + action + ", but serviceInfo was null");
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Indicates whether the Presto library is installed
     *
     * @param context The context to use to query the package manager.
     * @return True if the Presto library is installed, false if not.
     */
    public static boolean isPrestoLibraryInstalled(Context context) {
        return isIntentAvailable(context, ServiceBackedMediaPlayer.INTENT_NAME);
    }

    /**
     * Return an Intent that opens the Android Market page for the speed
     * alteration library
     *
     * @return The Intent for the Presto library on the Android Market
     */
    public static Intent getPrestoMarketIntent() {
        if (prestoMarketIntent == null) {
            prestoMarketIntent = new Intent(Intent.ACTION_VIEW,
                    SPEED_ADJUSTMENT_MARKET_URI);
        }
        return prestoMarketIntent;
    }

    /**
     * Open the Android Market page for the Presto library
     *
     * @param context The context from which to open the Android Market page
     */
    public static void openPrestoMarketIntent(Context context) {
        context.startActivity(getPrestoMarketIntent());
    }

    private static final String MP_TAG = "AocateReplacementMediaPlayer";

    private static final double PITCH_STEP_CONSTANT = 1.0594630943593;

    private AndroidMediaPlayer amp = null;
    // This is whether speed adjustment should be enabled (by the Service)
    // To avoid the Service entirely, set useService to false
    protected boolean enableSpeedAdjustment = true;
    private int lastKnownPosition = 0;
    // In some cases, we're going to have to replace the
    // android.media.MediaPlayer on the fly, and we don't want to touch the
    // wrong media player, so lock it way too much.
    ReentrantLock lock = new ReentrantLock();
    private int mAudioStreamType = AudioManager.STREAM_MUSIC;
    private Context mContext;
    private boolean mIsLooping = false;
    private float mLeftVolume = 1f;
    private float mPitchStepsAdjustment = 0f;
    private float mRightVolume = 1f;
    private float mSpeedMultiplier = 1f;
    private int mWakeMode = 0;
    MediaPlayerImpl mpi = null;
    protected boolean pitchAdjustmentAvailable = false;
    private ServiceBackedMediaPlayer sbmp = null;
    protected boolean speedAdjustmentAvailable = false;

    private Handler mServiceDisconnectedHandler = null;

    // Some parts of state cannot be found by calling MediaPlayerImpl functions,
    // so store our own state. This also helps copy state when changing
    // implementations
    State state = State.INITIALIZED;
    String stringDataSource = null;
    Uri uriDataSource = null;
    private boolean useService = false;

    // Naming Convention for Listeners
    // Most listeners can both be set by clients and called by MediaPlayImpls
    // There are a few that have to do things in this class as well as calling
    // the function. In all cases, onX is what is called by MediaPlayerImpl
    // If there is work to be done in this class, then the listener that is
    // set by setX is X (with the first letter lowercase).
    OnBufferingUpdateListener onBufferingUpdateListener = null;
    OnCompletionListener onCompletionListener = null;
    OnErrorListener onErrorListener = null;
    OnInfoListener onInfoListener = null;

    // Special case. Pitch adjustment ceases to be available when we switch
    // to the android.media.MediaPlayer (though it is not guaranteed to be
    // available when using the ServiceBackedMediaPlayer)
    OnPitchAdjustmentAvailableChangedListener onPitchAdjustmentAvailableChangedListener = new OnPitchAdjustmentAvailableChangedListener() {
        public void onPitchAdjustmentAvailableChanged(MediaPlayer arg0,
                                                      boolean pitchAdjustmentAvailable) {
            lock.lock();
            try {
                Log
                        .d(
                                MP_TAG,
                                "onPitchAdjustmentAvailableChangedListener.onPitchAdjustmentAvailableChanged being called");
                if (MediaPlayer.this.pitchAdjustmentAvailable != pitchAdjustmentAvailable) {
                    Log.d(MP_TAG, "Pitch adjustment state has changed from "
                            + MediaPlayer.this.pitchAdjustmentAvailable
                            + " to " + pitchAdjustmentAvailable);
                    MediaPlayer.this.pitchAdjustmentAvailable = pitchAdjustmentAvailable;
                    if (MediaPlayer.this.pitchAdjustmentAvailableChangedListener != null) {
                        MediaPlayer.this.pitchAdjustmentAvailableChangedListener
                                .onPitchAdjustmentAvailableChanged(arg0,
                                        pitchAdjustmentAvailable);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    };
    OnPitchAdjustmentAvailableChangedListener pitchAdjustmentAvailableChangedListener = null;

    MediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer arg0) {
            Log.d(MP_TAG, "onPreparedListener 242 setting state to PREPARED");
            MediaPlayer.this.state = State.PREPARED;
            if (MediaPlayer.this.preparedListener != null) {
                Log.d(MP_TAG, "Calling preparedListener");
                MediaPlayer.this.preparedListener.onPrepared(arg0);
            }
            Log.d(MP_TAG, "Wrap up onPreparedListener");
        }
    };

    OnPreparedListener preparedListener = null;
    OnSeekCompleteListener onSeekCompleteListener = null;

    // Special case. Speed adjustment ceases to be available when we switch
    // to the android.media.MediaPlayer (though it is not guaranteed to be
    // available when using the ServiceBackedMediaPlayer)
    OnSpeedAdjustmentAvailableChangedListener onSpeedAdjustmentAvailableChangedListener = new OnSpeedAdjustmentAvailableChangedListener() {
        public void onSpeedAdjustmentAvailableChanged(MediaPlayer arg0,
                                                      boolean speedAdjustmentAvailable) {
            lock.lock();
            try {
                Log
                        .d(
                                MP_TAG,
                                "onSpeedAdjustmentAvailableChangedListener.onSpeedAdjustmentAvailableChanged being called");
                if (MediaPlayer.this.speedAdjustmentAvailable != speedAdjustmentAvailable) {
                    Log.d(MP_TAG, "Speed adjustment state has changed from "
                            + MediaPlayer.this.speedAdjustmentAvailable
                            + " to " + speedAdjustmentAvailable);
                    MediaPlayer.this.speedAdjustmentAvailable = speedAdjustmentAvailable;
                    if (MediaPlayer.this.speedAdjustmentAvailableChangedListener != null) {
                        MediaPlayer.this.speedAdjustmentAvailableChangedListener
                                .onSpeedAdjustmentAvailableChanged(arg0,
                                        speedAdjustmentAvailable);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    };
    OnSpeedAdjustmentAvailableChangedListener speedAdjustmentAvailableChangedListener = null;

    private int speedAdjustmentAlgorithm = SpeedAdjustmentAlgorithm.SONIC;

    public MediaPlayer(final Context context) {
        this(context, true);
    }

    public MediaPlayer(final Context context, boolean useService) {
        this.mContext = context;
        this.useService = useService;

        // So here's the major problem
        // Sometimes the service won't exist or won't be connected,
        // so start with an android.media.MediaPlayer, and when
        // the service is connected, use that from then on
        this.mpi = this.amp = new AndroidMediaPlayer(this, context);

        // setupMpi will go get the Service, if it can, then bring that
        // implementation into sync
        Log.d(MP_TAG, "setupMpi");
        setupMpi(context);
    }

    private boolean invalidServiceConnectionConfiguration() {
        if (!(this.mpi instanceof ServiceBackedMediaPlayer)) {
            if (this.useService && isPrestoLibraryInstalled()) {
                // In this case, the Presto library has been installed
                // or something while playing sound
                // We could be using the service, but we're not
                Log.d(MP_TAG, "We could be using the service, but we're not 316");
                return true;
            }
            // If useService is false, then we shouldn't be using the SBMP
            // If the Presto library isn't installed, ditto
            Log.d(MP_TAG, "this.mpi is not a ServiceBackedMediaPlayer, but we couldn't use it anyway 321");
            return false;
        } else {
            if (BuildConfig.DEBUG && !(this.mpi instanceof ServiceBackedMediaPlayer))
                throw new AssertionError();
            if (this.useService && isPrestoLibraryInstalled()) {
                // We should be using the service, and we are. Great!
                Log.d(MP_TAG, "We could be using a ServiceBackedMediaPlayer and we are 327");
                return false;
            }
            // We're trying to use the service when we shouldn't,
            // that's an invalid configuration
            Log.d(MP_TAG, "We're trying to use a ServiceBackedMediaPlayer but we shouldn't be 332");
            return true;
        }
    }

    private void setupMpi(final Context context) {
        lock.lock();
        try {
            Log.d(MP_TAG, "setupMpi 336");
            // Check if the client wants to use the service at all,
            // then if we're already using the right kind of media player
            if (this.useService && isPrestoLibraryInstalled()) {
                if ((this.mpi != null)
                        && (this.mpi instanceof ServiceBackedMediaPlayer)) {
                    Log.d(MP_TAG, "Already using ServiceBackedMediaPlayer");
                    return;
                }
                if (this.sbmp == null) {
                    Log.d(MP_TAG, "Instantiating new ServiceBackedMediaPlayer 346");
                    this.sbmp = new ServiceBackedMediaPlayer(this, context,
                            new ServiceConnection() {
                                public void onServiceConnected(
                                        ComponentName className,
                                        final IBinder service) {
                                    Thread t = new Thread(new Runnable() {
                                        public void run() {
                                            // This lock probably isn't granular
                                            // enough
                                            MediaPlayer.this.lock.lock();
                                            Log.d(MP_TAG,
                                                    "onServiceConnected 257");
                                            try {
                                                MediaPlayer.this
                                                        .switchMediaPlayerImpl(
                                                                MediaPlayer.this.amp,
                                                                MediaPlayer.this.sbmp);
                                                Log.d(MP_TAG, "End onServiceConnected 362");
                                            } finally {
                                                MediaPlayer.this.lock.unlock();
                                            }
                                        }
                                    });
                                    t.start();
                                }

                                public void onServiceDisconnected(
                                        ComponentName className) {
                                    MediaPlayer.this.lock.lock();
                                    try {
                                        // Can't get any more useful information
                                        // out of sbmp
                                        if (MediaPlayer.this.sbmp != null) {
                                            MediaPlayer.this.sbmp.release();
                                        }
                                        // Unlike most other cases, sbmp gets set
                                        // to null since there's nothing useful
                                        // backing it now
                                        MediaPlayer.this.sbmp = null;

                                        if (mServiceDisconnectedHandler == null) {
                                            mServiceDisconnectedHandler = new Handler(new Callback() {
                                                public boolean handleMessage(Message msg) {
                                                    // switchMediaPlayerImpl won't try to
                                                    // clone anything from null
                                                    lock.lock();
                                                    try {
                                                        if (MediaPlayer.this.amp == null) {
                                                            // This should never be in this state
                                                            MediaPlayer.this.amp = new AndroidMediaPlayer(
                                                                    MediaPlayer.this,
                                                                    MediaPlayer.this.mContext);
                                                        }
                                                        // Use sbmp instead of null in case by some miracle it's
                                                        // been restored in the meantime
                                                        MediaPlayer.this.switchMediaPlayerImpl(
                                                                MediaPlayer.this.sbmp,
                                                                MediaPlayer.this.amp);
                                                        return true;
                                                    } finally {
                                                        lock.unlock();
                                                    }
                                                }
                                            });
                                        }

                                        // This code needs to execute on the
                                        // original thread to instantiate
                                        // the new object in the right place
                                        mServiceDisconnectedHandler
                                                .sendMessage(
                                                        mServiceDisconnectedHandler
                                                                .obtainMessage());
                                        // Note that we do NOT want to set
                                        // useService. useService is about
                                        // what the user wants, not what they
                                        // get
                                    } finally {
                                        MediaPlayer.this.lock.unlock();
                                    }
                                }
                            }
                    );
                }
                switchMediaPlayerImpl(this.amp, this.sbmp);
            } else {
                if ((this.mpi != null)
                        && (this.mpi instanceof AndroidMediaPlayer)) {
                    Log.d(MP_TAG, "Already using AndroidMediaPlayer");
                    return;
                }
                if (this.amp == null) {
                    Log.d(MP_TAG, "Instantiating new AndroidMediaPlayer (this should be impossible)");
                    this.amp = new AndroidMediaPlayer(this, context);
                }
                switchMediaPlayerImpl(this.sbmp, this.amp);
            }
        } finally {
            lock.unlock();
        }
    }

    private void switchMediaPlayerImpl(MediaPlayerImpl from, MediaPlayerImpl to) {
        lock.lock();
        try {
            Log.d(MP_TAG, "switchMediaPlayerImpl");
            if ((from == to)
                    // Same object, nothing to synchronize
                    || (to == null)
                    // Nothing to copy to (maybe this should throw an error?)
                    || ((to instanceof ServiceBackedMediaPlayer) && !((ServiceBackedMediaPlayer) to).isConnected())
                    // ServiceBackedMediaPlayer hasn't yet connected, onServiceConnected will take care of the transition
                    || (MediaPlayer.this.state == State.END)) {
                // State.END is after a release(), no further functions should
                // be called on this class and from is likely to have problems
                // retrieving state that won't be used anyway
                return;
            }
            // Extract all that we can from the existing implementation
            // and copy it to the new implementation

            Log.d(MP_TAG, "switchMediaPlayerImpl(), current state is "
                    + this.state.toString());

            to.reset();

            // Do this first so we don't have to prepare the same
            // data file twice
            to.setEnableSpeedAdjustment(MediaPlayer.this.enableSpeedAdjustment);

            // This is a reasonable place to set all of these,
            // none of them require prepare() or the like first
            to.setAudioStreamType(this.mAudioStreamType);
            to.setSpeedAdjustmentAlgorithm(this.speedAdjustmentAlgorithm);
            to.setLooping(this.mIsLooping);
            to.setPitchStepsAdjustment(this.mPitchStepsAdjustment);
            Log.d(MP_TAG, "Setting playback speed to " + this.mSpeedMultiplier);
            to.setPlaybackSpeed(this.mSpeedMultiplier);
            to.setVolume(MediaPlayer.this.mLeftVolume,
                    MediaPlayer.this.mRightVolume);
            to.setWakeMode(this.mContext, this.mWakeMode);

            Log.d(MP_TAG, "asserting at least one data source is null");
            assert ((MediaPlayer.this.stringDataSource == null) || (MediaPlayer.this.uriDataSource == null));

            if (uriDataSource != null) {
                Log.d(MP_TAG, "switchMediaPlayerImpl(): uriDataSource != null");
                try {
                    to.setDataSource(this.mContext, uriDataSource);
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (stringDataSource != null) {
                Log.d(MP_TAG,
                        "switchMediaPlayerImpl(): stringDataSource != null");
                try {
                    to.setDataSource(stringDataSource);
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if ((this.state == State.PREPARED)
                    || (this.state == State.PREPARING)
                    || (this.state == State.PAUSED)
                    || (this.state == State.STOPPED)
                    || (this.state == State.STARTED)
                    || (this.state == State.PLAYBACK_COMPLETED)) {
                Log.d(MP_TAG, "switchMediaPlayerImpl(): prepare and seek");
                // Use prepare here instead of prepareAsync so that
                // we wait for it to be ready before we try to use it
                try {
                    to.muteNextOnPrepare();
                    to.prepare();
                } catch (IllegalStateException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                int seekPos = 0;
                if (from != null) {
                    seekPos = from.getCurrentPosition();
                } else if (this.lastKnownPosition < to.getDuration()) {
                    // This can happen if the Service unexpectedly
                    // disconnected. Because it would result in too much
                    // information being passed around, we don't constantly
                    // poll for the lastKnownPosition, but we'll save it
                    // when getCurrentPosition is called
                    seekPos = this.lastKnownPosition;
                }
                to.muteNextSeek();
                to.seekTo(seekPos);
            }
            if ((from != null)
                    && from.isPlaying()) {
                from.pause();
            }
            if ((this.state == State.STARTED)
                    || (this.state == State.PAUSED)
                    || (this.state == State.STOPPED)) {
                Log.d(MP_TAG, "switchMediaPlayerImpl(): start");
                if (to != null) {
                    to.start();
                }
            }

            if (this.state == State.PAUSED) {
                Log.d(MP_TAG, "switchMediaPlayerImpl(): paused");
                if (to != null) {
                    to.pause();
                }
            } else if (this.state == State.STOPPED) {
                Log.d(MP_TAG, "switchMediaPlayerImpl(): stopped");
                if (to != null) {
                    to.stop();
                }
            }

            this.mpi = to;

            // Cheating here by relying on the side effect in
            // on(Pitch|Speed)AdjustmentAvailableChanged
            if ((to.canSetPitch() != this.pitchAdjustmentAvailable)
                    && (this.onPitchAdjustmentAvailableChangedListener != null)) {
                this.onPitchAdjustmentAvailableChangedListener
                        .onPitchAdjustmentAvailableChanged(this, to
                                .canSetPitch());
            }
            if ((to.canSetSpeed() != this.speedAdjustmentAvailable)
                    && (this.onSpeedAdjustmentAvailableChangedListener != null)) {
                this.onSpeedAdjustmentAvailableChangedListener
                        .onSpeedAdjustmentAvailableChanged(this, to
                                .canSetSpeed());
            }
            Log.d(MP_TAG, "switchMediaPlayerImpl() 625 " + this.state.toString());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns true if pitch can be changed at this moment
     *
     * @return True if pitch can be changed
     */
    public boolean canSetPitch() {
        lock.lock();
        try {
            return this.mpi.canSetPitch();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns true if speed can be changed at this moment
     *
     * @return True if speed can be changed
     */
    public boolean canSetSpeed() {
        lock.lock();
        try {
            return this.mpi.canSetSpeed();
        } finally {
            lock.unlock();
        }
    }

    protected void finalize() throws Throwable {
        lock.lock();
        try {
            Log.d(MP_TAG, "finalize() 626");
            this.release();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the number of steps (in a musical scale) by which playback is
     * currently shifted. When greater than zero, pitch is shifted up. When less
     * than zero, pitch is shifted down.
     *
     * @return The number of steps pitch is currently shifted by
     */
    public float getCurrentPitchStepsAdjustment() {
        lock.lock();
        try {
            return this.mpi.getCurrentPitchStepsAdjustment();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.getCurrentPosition()
     * Accurate only to frame size of encoded data (26 ms for MP3s)
     *
     * @return Current position (in milliseconds)
     */
    public int getCurrentPosition() {
        lock.lock();
        try {
            return (this.lastKnownPosition = this.mpi.getCurrentPosition());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the current speed multiplier. Defaults to 1.0 (normal speed)
     *
     * @return The current speed multiplier
     */
    public float getCurrentSpeedMultiplier() {
        lock.lock();
        try {
            return this.mpi.getCurrentSpeedMultiplier();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.getDuration()
     *
     * @return Length of the track (in milliseconds)
     */
    public int getDuration() {
        lock.lock();
        try {
            return this.mpi.getDuration();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the maximum value that can be passed to setPlaybackSpeed
     *
     * @return The maximum speed multiplier
     */
    public float getMaxSpeedMultiplier() {
        lock.lock();
        try {
            return this.mpi.getMaxSpeedMultiplier();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the minimum value that can be passed to setPlaybackSpeed
     *
     * @return The minimum speed multiplier
     */
    public float getMinSpeedMultiplier() {
        lock.lock();
        try {
            return this.mpi.getMinSpeedMultiplier();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the version code of the backing service
     *
     * @return -1 if ServiceBackedMediaPlayer is not used, 0 if the service is not
     * connected, otherwise the version code retrieved from the service
     */
    public int getServiceVersionCode() {
        lock.lock();
        try {
            if (this.mpi instanceof ServiceBackedMediaPlayer) {
                return ((ServiceBackedMediaPlayer) this.mpi).getServiceVersionCode();
            } else {
                return -1;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the version name of the backing service
     *
     * @return null if ServiceBackedMediaPlayer is not used, empty string if
     * the service is not connected, otherwise the version name retrieved from
     * the service
     */
    public String getServiceVersionName() {
        lock.lock();
        try {
            if (this.mpi instanceof ServiceBackedMediaPlayer) {
                return ((ServiceBackedMediaPlayer) this.mpi).getServiceVersionName();
            } else {
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.isLooping()
     *
     * @return True if the track is looping
     */
    public boolean isLooping() {
        lock.lock();
        try {
            return this.mpi.isLooping();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.isPlaying()
     *
     * @return True if the track is playing
     */
    public boolean isPlaying() {
        lock.lock();
        try {
            return this.mpi.isPlaying();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns true if this MediaPlayer has access to the Presto
     * library
     *
     * @return True if the Presto library is installed
     */
    public boolean isPrestoLibraryInstalled() {
        if ((this.mpi == null) || (this.mpi.mContext == null)) {
            return false;
        }
        return isPrestoLibraryInstalled(this.mpi.mContext);
    }

    /**
     * Open the Android Market page in the same context as this MediaPlayer
     */
    public void openPrestoMarketIntent() {
        if ((this.mpi != null) && (this.mpi.mContext != null)) {
            openPrestoMarketIntent(this.mpi.mContext);
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.pause() Pauses the
     * track
     */
    public void pause() {
        lock.lock();
        try {
            if (invalidServiceConnectionConfiguration()) {
                setupMpi(this.mpi.mContext);
            }
            this.state = State.PAUSED;
            this.mpi.pause();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.prepare() Prepares the
     * track. This or prepareAsync must be called before start()
     */
    public void prepare() throws IllegalStateException, IOException {
        lock.lock();
        try {
            Log.d(MP_TAG, "prepare() 746 using " + ((this.mpi == null) ? "null (this shouldn't happen)" : this.mpi.getClass().toString()) + " state " + this.state.toString());
            Log.d(MP_TAG, "onPreparedListener is: " + ((this.onPreparedListener == null) ? "null" : "non-null"));
            Log.d(MP_TAG, "preparedListener is: " + ((this.preparedListener == null) ? "null" : "non-null"));
            if (invalidServiceConnectionConfiguration()) {
                setupMpi(this.mpi.mContext);
            }
            this.mpi.prepare();
            this.state = State.PREPARED;
            Log.d(MP_TAG, "prepare() finished 778");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.prepareAsync()
     * Prepares the track. This or prepare must be called before start()
     */
    public void prepareAsync() {
        lock.lock();
        try {
            Log.d(MP_TAG, "prepareAsync() 779");
            if (invalidServiceConnectionConfiguration()) {
                setupMpi(this.mpi.mContext);
            }
            this.state = State.PREPARING;
            this.mpi.prepareAsync();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.release() Releases the
     * underlying resources used by the media player.
     */
    public void release() {
        lock.lock();
        try {
            Log.d(MP_TAG, "Releasing MediaPlayer 791");

            this.state = State.END;
            if (this.amp != null) {
                this.amp.release();
            }
            if (this.sbmp != null) {
                this.sbmp.release();
            }

            this.onBufferingUpdateListener = null;
            this.onCompletionListener = null;
            this.onErrorListener = null;
            this.onInfoListener = null;
            this.preparedListener = null;
            this.onPitchAdjustmentAvailableChangedListener = null;
            this.pitchAdjustmentAvailableChangedListener = null;
            Log.d(MP_TAG, "Setting onSeekCompleteListener to null 871");
            this.onSeekCompleteListener = null;
            this.onSpeedAdjustmentAvailableChangedListener = null;
            this.speedAdjustmentAvailableChangedListener = null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.reset() Resets the
     * track to idle state
     */
    public void reset() {
        lock.lock();
        try {
            this.state = State.IDLE;
            this.stringDataSource = null;
            this.uriDataSource = null;
            this.mpi.reset();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.seekTo(int msec) Seeks
     * to msec in the track
     */
    public void seekTo(int msec) throws IllegalStateException {
        lock.lock();
        try {
            this.mpi.seekTo(msec);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.setAudioStreamType(int
     * streamtype) Sets the audio stream type.
     */
    public void setAudioStreamType(int streamtype) {
        lock.lock();
        try {
            this.mAudioStreamType = streamtype;
            this.mpi.setAudioStreamType(streamtype);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.setDataSource(Context
     * context, Uri uri) Sets uri as data source in the context given
     */
    public void setDataSource(Context context, Uri uri)
            throws IllegalArgumentException, IllegalStateException, IOException {
        lock.lock();
        try {
            Log.d(MP_TAG, "In setDataSource(context, " + uri.toString() + "), using " + this.mpi.getClass().toString());
            if (invalidServiceConnectionConfiguration()) {
                setupMpi(this.mpi.mContext);
            }
            this.state = State.INITIALIZED;
            this.stringDataSource = null;
            this.uriDataSource = uri;
            this.mpi.setDataSource(context, uri);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.setDataSource(String
     * path) Sets the data source of the track to a file given.
     */
    public void setDataSource(String path) throws IllegalArgumentException,
            IllegalStateException, IOException {
        lock.lock();
        try {
            Log.d(MP_TAG, "In setDataSource(context, " + path + ")");
            if (invalidServiceConnectionConfiguration()) {
                setupMpi(this.mpi.mContext);
            }
            this.state = State.INITIALIZED;
            this.stringDataSource = path;
            this.uriDataSource = null;
            this.mpi.setDataSource(path);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sets whether to use speed adjustment or not. Speed adjustment on is more
     * computation-intensive than with it off.
     *
     * @param enableSpeedAdjustment Whether speed adjustment should be supported.
     */
    public void setEnableSpeedAdjustment(boolean enableSpeedAdjustment) {
        lock.lock();
        try {
            this.enableSpeedAdjustment = enableSpeedAdjustment;
            this.mpi.setEnableSpeedAdjustment(enableSpeedAdjustment);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.setLooping(boolean
     * loop) Sets the track to loop infinitely if loop is true, play once if
     * loop is false
     */
    public void setLooping(boolean loop) {
        lock.lock();
        try {
            this.mIsLooping = loop;
            this.mpi.setLooping(loop);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sets the number of steps (in a musical scale) by which playback is
     * currently shifted. When greater than zero, pitch is shifted up. When less
     * than zero, pitch is shifted down.
     *
     * @param pitchSteps The number of steps by which to shift playback
     */
    public void setPitchStepsAdjustment(float pitchSteps) {
        lock.lock();
        try {
            this.mPitchStepsAdjustment = pitchSteps;
            this.mpi.setPitchStepsAdjustment(pitchSteps);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Set the algorithm to use for changing the speed and pitch of audio
     * See SpeedAdjustmentAlgorithm constants for more details
     *
     * @param algorithm The algorithm to use.
     */
    public void setSpeedAdjustmentAlgorithm(int algorithm) {
        lock.lock();
        try {
            this.speedAdjustmentAlgorithm = algorithm;
            if (this.mpi != null) {
                this.mpi.setSpeedAdjustmentAlgorithm(algorithm);
            }
        } finally {
            lock.unlock();
        }
    }

    private static float getPitchStepsAdjustment(float pitch) {
        return (float) (Math.log(pitch) / (2 * Math.log(PITCH_STEP_CONSTANT)));
    }

    /**
     * Sets the percentage by which pitch is currently shifted. When greater
     * than zero, pitch is shifted up. When less than zero, pitch is shifted
     * down
     *
     * @param f The percentage to shift pitch
     */
    public void setPlaybackPitch(float pitch) {
        lock.lock();
        try {
            this.mPitchStepsAdjustment = getPitchStepsAdjustment(pitch);
            this.mpi.setPlaybackPitch(pitch);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Set playback speed. 1.0 is normal speed, 2.0 is double speed, and so on.
     * Speed should never be set to 0 or below.
     *
     * @param f The speed multiplier to use for further playback
     */
    public void setPlaybackSpeed(float f) {
        lock.lock();
        try {
            this.mSpeedMultiplier = f;
            this.mpi.setPlaybackSpeed(f);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sets whether to use speed adjustment or not. Speed adjustment on is more
     * computation-intensive than with it off.
     *
     * @param enableSpeedAdjustment Whether speed adjustment should be supported.
     */
    public void setUseService(boolean useService) {
        lock.lock();
        try {
            this.useService = useService;
            setupMpi(this.mpi.mContext);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.setVolume(float
     * leftVolume, float rightVolume) Sets the stereo volume
     */
    public void setVolume(float leftVolume, float rightVolume) {
        lock.lock();
        try {
            this.mLeftVolume = leftVolume;
            this.mRightVolume = rightVolume;
            this.mpi.setVolume(leftVolume, rightVolume);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.setWakeMode(Context
     * context, int mode) Acquires a wake lock in the context given. You must
     * request the appropriate permissions in your AndroidManifest.xml file.
     */
    public void setWakeMode(Context context, int mode) {
        lock.lock();
        try {
            this.mWakeMode = mode;
            this.mpi.setWakeMode(context, mode);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to
     * android.media.MediaPlayer.setOnCompletionListener(OnCompletionListener
     * listener) Sets a listener to be used when a track completes playing.
     */
    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        lock.lock();
        try {
            this.onBufferingUpdateListener = listener;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to
     * android.media.MediaPlayer.setOnCompletionListener(OnCompletionListener
     * listener) Sets a listener to be used when a track completes playing.
     */
    public void setOnCompletionListener(OnCompletionListener listener) {
        lock.lock();
        try {
            this.onCompletionListener = listener;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to
     * android.media.MediaPlayer.setOnErrorListener(OnErrorListener listener)
     * Sets a listener to be used when a track encounters an error.
     */
    public void setOnErrorListener(OnErrorListener listener) {
        lock.lock();
        try {
            this.onErrorListener = listener;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to
     * android.media.MediaPlayer.setOnInfoListener(OnInfoListener listener) Sets
     * a listener to be used when a track has info.
     */
    public void setOnInfoListener(OnInfoListener listener) {
        lock.lock();
        try {
            this.onInfoListener = listener;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sets a listener that will fire when pitch adjustment becomes available or
     * stops being available
     */
    public void setOnPitchAdjustmentAvailableChangedListener(
            OnPitchAdjustmentAvailableChangedListener listener) {
        lock.lock();
        try {
            this.pitchAdjustmentAvailableChangedListener = listener;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to
     * android.media.MediaPlayer.setOnPreparedListener(OnPreparedListener
     * listener) Sets a listener to be used when a track finishes preparing.
     */
    public void setOnPreparedListener(OnPreparedListener listener) {
        lock.lock();
        Log.d(MP_TAG, " ++++++++++++++++++++++++++++++++++++++++++++ setOnPreparedListener");
        try {
            this.preparedListener = listener;
            // For this one, we do not explicitly set the MediaPlayer or the
            // Service listener. This is because in addition to calling the
            // listener provided by the client, it's necessary to change
            // state to PREPARED. See prepareAsync for implementation details
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to
     * android.media.MediaPlayer.setOnSeekCompleteListener
     * (OnSeekCompleteListener listener) Sets a listener to be used when a track
     * finishes seeking.
     */
    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        lock.lock();
        try {
            this.onSeekCompleteListener = listener;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sets a listener that will fire when speed adjustment becomes available or
     * stops being available
     */
    public void setOnSpeedAdjustmentAvailableChangedListener(
            OnSpeedAdjustmentAvailableChangedListener listener) {
        lock.lock();
        try {
            this.speedAdjustmentAvailableChangedListener = listener;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.start() Starts a track
     * playing
     */
    public void start() {
        lock.lock();
        try {
            Log.d(MP_TAG, "start() 1149");
            if (invalidServiceConnectionConfiguration()) {
                setupMpi(this.mpi.mContext);
            }
            this.state = State.STARTED;
            Log.d(MP_TAG, "start() 1154");
            this.mpi.start();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Functions identically to android.media.MediaPlayer.stop() Stops a track
     * playing and resets its position to the start.
     */
    public void stop() {
        lock.lock();
        try {
            if (invalidServiceConnectionConfiguration()) {
                setupMpi(this.mpi.mContext);
            }
            this.state = State.STOPPED;
            this.mpi.stop();
        } finally {
            lock.unlock();
        }
    }
}