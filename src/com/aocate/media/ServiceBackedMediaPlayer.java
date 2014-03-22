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

import java.io.IOException;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.aocate.media.MediaPlayer.State;
import com.aocate.presto.service.IDeathCallback_0_8;
import com.aocate.presto.service.IOnBufferingUpdateListenerCallback_0_8;
import com.aocate.presto.service.IOnCompletionListenerCallback_0_8;
import com.aocate.presto.service.IOnErrorListenerCallback_0_8;
import com.aocate.presto.service.IOnInfoListenerCallback_0_8;
import com.aocate.presto.service.IOnPitchAdjustmentAvailableChangedListenerCallback_0_8;
import com.aocate.presto.service.IOnPreparedListenerCallback_0_8;
import com.aocate.presto.service.IOnSeekCompleteListenerCallback_0_8;
import com.aocate.presto.service.IOnSpeedAdjustmentAvailableChangedListenerCallback_0_8;
import com.aocate.presto.service.IPlayMedia_0_8;

/**
 * Class for connecting to remote speed-altering, media playing Service
 * Note that there is unusually high coupling between MediaPlayer and this 
 * class.  This is an unfortunate compromise, since the alternative was to
 * track state in two different places in this code (plus the internal state
 * of the remote media player).
 * @author aocate
 *
 */
public class ServiceBackedMediaPlayer extends MediaPlayerImpl {
	static final String INTENT_NAME = "com.aocate.intent.PLAY_AUDIO_ADJUST_SPEED_0_8";
	
	private static final String SBMP_TAG = "AocateServiceBackedMediaPlayer";

	private ServiceConnection mPlayMediaServiceConnection = null;
	protected IPlayMedia_0_8 pmInterface = null;
	private Intent playMediaServiceIntent = null;
	// In some cases, we're going to have to replace the
	// android.media.MediaPlayer on the fly, and we don't want to touch the
	// wrong media player.

	private long sessionId = 0;
	private boolean isErroring = false;
	private int mAudioStreamType = AudioManager.STREAM_MUSIC;

	private WakeLock mWakeLock = null;

	// So here's the major problem
	// Sometimes the service won't exist or won't be connected,
	// so start with an android.media.MediaPlayer, and when
	// the service is connected, use that from then on
	public ServiceBackedMediaPlayer(MediaPlayer owningMediaPlayer, final Context context, final ServiceConnection serviceConnection) {
		super(owningMediaPlayer, context);
		Log.d(SBMP_TAG, "Instantiating ServiceBackedMediaPlayer 87");
		this.playMediaServiceIntent = 
			new Intent(INTENT_NAME);
		this.mPlayMediaServiceConnection = new ServiceConnection() {
			public void onServiceConnected(ComponentName name, IBinder service) {
				IPlayMedia_0_8 tmpPlayMediaInterface = IPlayMedia_0_8.Stub.asInterface((IBinder) service);
				
				Log.d(SBMP_TAG, "Setting up pmInterface 94");
				if (ServiceBackedMediaPlayer.this.sessionId == 0) {
					try {
						// The IDeathCallback isn't a conventional callback.
						// It exists so that if the client ceases to exist,
						// the Service becomes aware of that and can shut
						// down whatever it needs to shut down
						ServiceBackedMediaPlayer.this.sessionId = tmpPlayMediaInterface.startSession(new IDeathCallback_0_8.Stub() {
						});
						// This is really bad if this fails
					} catch (RemoteException e) {
						e.printStackTrace();
						ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
					}
				}

				Log.d(SBMP_TAG, "Assigning pmInterface");
				
				ServiceBackedMediaPlayer.this.setOnBufferingUpdateCallback(tmpPlayMediaInterface);
				ServiceBackedMediaPlayer.this.setOnCompletionCallback(tmpPlayMediaInterface);
				ServiceBackedMediaPlayer.this.setOnErrorCallback(tmpPlayMediaInterface);
				ServiceBackedMediaPlayer.this.setOnInfoCallback(tmpPlayMediaInterface);
				ServiceBackedMediaPlayer.this.setOnPitchAdjustmentAvailableChangedListener(tmpPlayMediaInterface);
				ServiceBackedMediaPlayer.this.setOnPreparedCallback(tmpPlayMediaInterface);
				ServiceBackedMediaPlayer.this.setOnSeekCompleteCallback(tmpPlayMediaInterface);
				ServiceBackedMediaPlayer.this.setOnSpeedAdjustmentAvailableChangedCallback(tmpPlayMediaInterface);
				
				// In order to avoid race conditions from the sessionId or listener not being assigned
				pmInterface = tmpPlayMediaInterface;
				
				Log.d(SBMP_TAG, "Invoking onServiceConnected");
				serviceConnection.onServiceConnected(name, service);
			}
			
			public void onServiceDisconnected(ComponentName name) {
				Log.d(SBMP_TAG, "onServiceDisconnected 114");
				
				pmInterface = null;
				
				sessionId = 0;
				
				serviceConnection.onServiceDisconnected(name);
			}
		};
		
		Log.d(SBMP_TAG, "Connecting PlayMediaService 124");
		if (!ConnectPlayMediaService()) {
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}

	private boolean ConnectPlayMediaService() {
		Log.d(SBMP_TAG, "ConnectPlayMediaService()");

		if (MediaPlayer.isIntentAvailable(mContext, INTENT_NAME)) {
			Log.d(SBMP_TAG, INTENT_NAME + " is available");
			if (pmInterface == null) {
				try {
					Log.d(SBMP_TAG, "Binding service");
					return mContext.bindService(playMediaServiceIntent, mPlayMediaServiceConnection, Context.BIND_AUTO_CREATE);
				} catch (Exception e) {
					return false;
				}
			} else {
				Log.d(SBMP_TAG, "Service already bound");
				return true;
			}
		}
		else {
			Log.d(SBMP_TAG, INTENT_NAME + " is not available");
			return false;
		}
	}

	/**
	 * Returns true if pitch can be changed at this moment
	 * @return True if pitch can be changed
	 */
	@Override
	public boolean canSetPitch() {
		Log.d(SBMP_TAG, "canSetPitch() 155");

		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		if (pmInterface != null) {
			// Can't set pitch if the service isn't connected
			try {
				return pmInterface.canSetPitch(ServiceBackedMediaPlayer.this.sessionId);
			} catch (RemoteException e) {
				e.printStackTrace();
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		return false;
	}

	/**
	 * Returns true if speed can be changed at this moment
	 * @return True if speed can be changed
	 */
	@Override
	public boolean canSetSpeed() {
		Log.d(SBMP_TAG, "canSetSpeed() 180");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		if (pmInterface != null) {
			// Can't set speed if the service isn't connected
			try {
				return pmInterface.canSetSpeed(ServiceBackedMediaPlayer.this.sessionId);
			} catch (RemoteException e) {
				e.printStackTrace();
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		return false;
	}
	
	void error(int what, int extra) {
		owningMediaPlayer.lock.lock();
		Log.e(SBMP_TAG, "error(" + what + ", " + extra + ")");
		try {
			if (!this.isErroring) {
				this.isErroring = true;
				owningMediaPlayer.state = State.ERROR;
				if (owningMediaPlayer.onErrorListener != null) {
					if (owningMediaPlayer.onErrorListener.onError(owningMediaPlayer, what, extra)) {
						return;
					}
				}
				if (owningMediaPlayer.onCompletionListener != null) {
					owningMediaPlayer.onCompletionListener.onCompletion(owningMediaPlayer);
				}
			}
		}
		finally {
			this.isErroring = false;
			owningMediaPlayer.lock.unlock();
		}
	}
	
	protected void finalize() throws Throwable {
		owningMediaPlayer.lock.lock();
		try {
			Log.d(SBMP_TAG, "finalize() 224");
			this.release();
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}

	/**
	 * Returns the number of steps (in a musical scale) by which playback is
	 * currently shifted.  When greater than zero, pitch is shifted up.
	 * When less than zero, pitch is shifted down.
	 * @return The number of steps pitch is currently shifted by
	 */
	@Override
	public float getCurrentPitchStepsAdjustment() {
		Log.d(SBMP_TAG, "getCurrentPitchStepsAdjustment() 240");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		if (pmInterface != null) {
			// Can't set pitch if the service isn't connected
			try {
				return pmInterface.getCurrentPitchStepsAdjustment(
					ServiceBackedMediaPlayer.this.sessionId);
			} catch (RemoteException e) {
				e.printStackTrace();
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		return 0f;
	}
	
	/**
	 * Functions identically to android.media.MediaPlayer.getCurrentPosition()
	 * @return Current position (in milliseconds)
	 */
	@Override
	public int getCurrentPosition() {
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		try {
			return pmInterface.getCurrentPosition(
				ServiceBackedMediaPlayer.this.sessionId);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
		return 0;
	}

	/**
	 * Returns the current speed multiplier.  Defaults to 1.0 (normal speed)
	 * @return The current speed multiplier
	 */
	@Override
	public float getCurrentSpeedMultiplier() {
		Log.d(SBMP_TAG, "getCurrentSpeedMultiplier() 286");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		if (pmInterface != null) {
			// Can't set speed if the service isn't connected
			try {
				return pmInterface.getCurrentSpeedMultiplier(
					ServiceBackedMediaPlayer.this.sessionId);
			} catch (RemoteException e) {
				e.printStackTrace();
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		return 1;
	}

	/**
	 * Functions identically to android.media.MediaPlayer.getDuration()
	 * @return Length of the track (in milliseconds)
	 */
	@Override
	public int getDuration() {
		Log.d(SBMP_TAG, "getDuration() 311");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		try {
			return pmInterface.getDuration(ServiceBackedMediaPlayer.this.sessionId);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
		return 0;
	}

	/**
	 * Get the maximum value that can be passed to setPlaybackSpeed
	 * @return The maximum speed multiplier
	 */
	@Override
	public float getMaxSpeedMultiplier() {
		Log.d(SBMP_TAG, "getMaxSpeedMultiplier() 332");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		if (pmInterface != null) {
			// Can't set speed if the Service isn't connected
			try {
				return pmInterface.getMaxSpeedMultiplier(
					ServiceBackedMediaPlayer.this.sessionId);
			} catch (RemoteException e) {
				e.printStackTrace();
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		return 1f;
	}

	/**
	 * Get the minimum value that can be passed to setPlaybackSpeed
	 * @return The minimum speed multiplier
	 */
	@Override
	public float getMinSpeedMultiplier() {
		Log.d(SBMP_TAG, "getMinSpeedMultiplier() 357");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		if (pmInterface != null) {
			// Can't set speed if the Service isn't connected
			try {
				return pmInterface.getMinSpeedMultiplier(
					ServiceBackedMediaPlayer.this.sessionId);
			} catch (RemoteException e) {
				e.printStackTrace();
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		return 1f;
	}
	
	public int getServiceVersionCode() {
		Log.d(SBMP_TAG, "getVersionCode");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		try {
			return pmInterface.getVersionCode();
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
		return 0;
	}
	
	public String getServiceVersionName() {
		Log.d(SBMP_TAG, "getVersionName");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		try {
			return pmInterface.getVersionName();
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
		return "";
	}
	
	public boolean isConnected() {
		return (pmInterface != null);
	}
	
	/**
	 * Functions identically to android.media.MediaPlayer.isLooping()
	 * @return True if the track is looping
	 */
	@Override
	public boolean isLooping() {
		Log.d(SBMP_TAG, "isLooping() 382");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		try {
			return pmInterface.isLooping(ServiceBackedMediaPlayer.this.sessionId);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
		return false;
	}

	/**
	 * Functions identically to android.media.MediaPlayer.isPlaying()
	 * @return True if the track is playing
	 */
	@Override
	public boolean isPlaying() {
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		if (pmInterface != null) {
			try {
				return pmInterface.isPlaying(ServiceBackedMediaPlayer.this.sessionId);
			} catch (RemoteException e) {
				e.printStackTrace();
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		return false;
	}
	
	/**
	 * Functions identically to android.media.MediaPlayer.pause()
	 * Pauses the track
	 */
	@Override
	public void pause() {
		Log.d(SBMP_TAG, "pause() 424");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		try {
			pmInterface.pause(ServiceBackedMediaPlayer.this.sessionId);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}
	
	/**
	 * Functions identically to android.media.MediaPlayer.prepare()
	 * Prepares the track.  This or prepareAsync must be called before start()
	 */
	@Override
	public void prepare() throws IllegalStateException, IOException {
		Log.d(SBMP_TAG, "prepare() 444");
		Log.d(SBMP_TAG, "onPreparedCallback is: " + ((this.mOnPreparedCallback == null) ? "null" : "non-null"));
		if (pmInterface == null) {
			Log.d(SBMP_TAG, "prepare: pmInterface is null");
			if (!ConnectPlayMediaService()) {
				Log.d(SBMP_TAG, "prepare: Failed to connect play media service");
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		if (pmInterface != null) {
			Log.d(SBMP_TAG, "prepare: pmInterface isn't null");
			try {
				Log.d(SBMP_TAG, "prepare: Remote invoke pmInterface.prepare(" + ServiceBackedMediaPlayer.this.sessionId + ")");
				pmInterface.prepare(ServiceBackedMediaPlayer.this.sessionId);
				Log.d(SBMP_TAG, "prepare: prepared");
			} catch (RemoteException e) {
				Log.d(SBMP_TAG, "prepare: RemoteException");
				e.printStackTrace();
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		Log.d(SBMP_TAG, "Done with prepare()");
	}
	
	/**
	 * Functions identically to android.media.MediaPlayer.prepareAsync()
	 * Prepares the track.  This or prepare must be called before start()
	 */
	@Override
	public void prepareAsync() {
		Log.d(SBMP_TAG, "prepareAsync() 469");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		try {
			pmInterface.prepareAsync(ServiceBackedMediaPlayer.this.sessionId);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}

	/**
	 * Functions identically to android.media.MediaPlayer.release()
	 * Releases the underlying resources used by the media player.
	 */
	@Override
	public void release() {
		Log.d(SBMP_TAG, "release() 492");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		if (pmInterface != null) {
			Log.d(SBMP_TAG, "release() 500");
			try {
				pmInterface.release(ServiceBackedMediaPlayer.this.sessionId);
			} catch (RemoteException e) {
				e.printStackTrace();
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
			mContext.unbindService(this.mPlayMediaServiceConnection);
			// Don't try to keep awake (if we were)
			this.setWakeMode(mContext, 0);
			pmInterface = null;
			this.sessionId = 0;
		}
		
		if ((this.mWakeLock != null) && this.mWakeLock.isHeld()) {
			Log.d(SBMP_TAG, "Releasing wakelock");
			this.mWakeLock.release();
		}
	}

	/**
	 * Functions identically to android.media.MediaPlayer.reset()
	 * Resets the track to idle state
	 */
	@Override
	public void reset() {
		Log.d(SBMP_TAG, "reset() 523");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		try {
			pmInterface.reset(ServiceBackedMediaPlayer.this.sessionId);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}

	/**
	 * Functions identically to android.media.MediaPlayer.seekTo(int msec)
	 * Seeks to msec in the track
	 */
	@Override
	public void seekTo(int msec) throws IllegalStateException {
		Log.d(SBMP_TAG, "seekTo(" + msec + ")");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		try {
			pmInterface.seekTo(ServiceBackedMediaPlayer.this.sessionId, msec);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}
	
	/**
	 * Functions identically to android.media.MediaPlayer.setAudioStreamType(int streamtype)
	 * Sets the audio stream type.
	 */
	@Override
	public void setAudioStreamType(int streamtype) {
		Log.d(SBMP_TAG, "setAudioStreamType(" + streamtype + ")");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		try {
			pmInterface.setAudioStreamType(
				ServiceBackedMediaPlayer.this.sessionId,
				this.mAudioStreamType);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}


	/**
	 * Functions identically to android.media.MediaPlayer.setDataSource(Context context, Uri uri)
	 * Sets uri as data source in the context given
	 */
	@Override
	public void setDataSource(Context context, Uri uri) throws IllegalArgumentException, IllegalStateException, IOException {
		Log.d(SBMP_TAG, "setDataSource(context, uri)");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		try {
			pmInterface.setDataSourceUri(
				ServiceBackedMediaPlayer.this.sessionId,
				uri);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}

	/**
	 * Functions identically to android.media.MediaPlayer.setDataSource(String path)
	 * Sets the data source of the track to a file given.
	 */
	@Override
	public void setDataSource(String path) throws IllegalArgumentException, IllegalStateException, IOException {
		Log.d(SBMP_TAG, "setDataSource(path)");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		if (pmInterface == null) {
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
		else {
			try {
				pmInterface.setDataSourceString(
					ServiceBackedMediaPlayer.this.sessionId,
					path);
			} catch (RemoteException e) {
				e.printStackTrace();
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
	}

	/**
	 * Sets whether to use speed adjustment or not.  Speed adjustment on is 
	 * more computation-intensive than with it off. 
	 * @param enableSpeedAdjustment Whether speed adjustment should be supported.
	 */
	@Override
	public void setEnableSpeedAdjustment(boolean enableSpeedAdjustment) {
		// TODO: This has no business being here, I think
		owningMediaPlayer.lock.lock();
		Log.d(SBMP_TAG, "setEnableSpeedAdjustment(enableSpeedAdjustment)");
		try {
			if (pmInterface == null) {
				if (!ConnectPlayMediaService()) {
					ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
				}
			}
			if (pmInterface != null) {
				// Can't set speed if the Service isn't connected
				try {
					pmInterface.setEnableSpeedAdjustment(
						ServiceBackedMediaPlayer.this.sessionId,
						enableSpeedAdjustment);
				} catch (RemoteException e) {
					e.printStackTrace();
					ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
				}
			}
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}


	/**
	 * Functions identically to android.media.MediaPlayer.setLooping(boolean loop)
	 * Sets the track to loop infinitely if loop is true, play once if loop is false
	 */
	@Override
	public void setLooping(boolean loop) {
		Log.d(SBMP_TAG, "setLooping(" + loop + ")");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		try {
			pmInterface.setLooping(ServiceBackedMediaPlayer.this.sessionId, loop);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}

	/**
	 * Sets the number of steps (in a musical scale) by which playback is
	 * currently shifted.  When greater than zero, pitch is shifted up.
	 * When less than zero, pitch is shifted down.
	 * 
	 * @param pitchSteps The number of steps by which to shift playback
	 */
	@Override
	public void setPitchStepsAdjustment(float pitchSteps) {
		Log.d(SBMP_TAG, "setPitchStepsAdjustment(" + pitchSteps + ")");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		if (pmInterface != null) {
			// Can't set speed if the Service isn't connected
			try {
				pmInterface.setPitchStepsAdjustment(
					ServiceBackedMediaPlayer.this.sessionId,
					pitchSteps);
			} catch (RemoteException e) {
				e.printStackTrace();
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
	}

	/**
	 * Sets the percentage by which pitch is currently shifted.  When
	 * greater than zero, pitch is shifted up.  When less than zero, pitch
	 * is shifted down
	 * @param f The percentage to shift pitch
	 */
	@Override
	public void setPlaybackPitch(float f) {
		Log.d(SBMP_TAG, "setPlaybackPitch(" + f + ")");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		if (pmInterface != null) {
			// Can't set speed if the Service isn't connected
			try {
				pmInterface.setPlaybackPitch(
					ServiceBackedMediaPlayer.this.sessionId,
					f);
			} catch (RemoteException e) {
				e.printStackTrace();
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
	}

	/**
	 * Set playback speed.  1.0 is normal speed, 2.0 is double speed, and so
	 * on.  Speed should never be set to 0 or below.
	 * @param f The speed multiplier to use for further playback
	 */
	@Override
	public void setPlaybackSpeed(float f) {
		Log.d(SBMP_TAG, "setPlaybackSpeed(" + f + ")");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		if (pmInterface != null) {
			// Can't set speed if the Service isn't connected
			try {
				pmInterface.setPlaybackSpeed(
					ServiceBackedMediaPlayer.this.sessionId,
					f);
			} catch (RemoteException e) {
				e.printStackTrace();
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
	}
	
	@Override
	public void setSpeedAdjustmentAlgorithm(int algorithm) {
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		try {
			pmInterface.setSpeedAdjustmentAlgorithm(
				ServiceBackedMediaPlayer.this.sessionId,
				algorithm);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}

	/**
	 * Functions identically to android.media.MediaPlayer.setVolume(float leftVolume, float rightVolume)
	 * Sets the stereo volume
	 */
	@Override
	public void setVolume(float leftVolume, float rightVolume) {
		Log.d(SBMP_TAG, "setVolume(" + leftVolume + ", " + rightVolume + ")");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		try {
			pmInterface.setVolume(
				ServiceBackedMediaPlayer.this.sessionId,
				leftVolume,
				rightVolume);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}
	
	/**
	 * Functions identically to android.media.MediaPlayer.setWakeMode(Context context, int mode)
	 * Acquires a wake lock in the context given.  You must request the appropriate permissions
	 * in your AndroidManifest.xml file.
	 */
	@Override
	// This does not just call .setWakeMode() in the Service because doing so 
	// would add a permission requirement to the Service.  Do it here, and it's
	// the client app's responsibility to request that permission
	public void setWakeMode(Context context, int mode) {
		Log.d(SBMP_TAG, "setWakeMode(context, " + mode + ")");
		if ((this.mWakeLock != null)
			&& (this.mWakeLock.isHeld())) {
			this.mWakeLock.release();
		}
		if (mode != 0) {
			if (this.mWakeLock == null) {
				PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
				// Since mode can't be changed on the fly, we have to allocate a new one
				this.mWakeLock = pm.newWakeLock(mode, this.getClass().getName());
			}

			this.mWakeLock.acquire();
		}
	}

	private IOnBufferingUpdateListenerCallback_0_8.Stub mOnBufferingUpdateCallback = null;
	private void setOnBufferingUpdateCallback(IPlayMedia_0_8 iface) {
		try {
			if (this.mOnBufferingUpdateCallback == null) {
				mOnBufferingUpdateCallback = new IOnBufferingUpdateListenerCallback_0_8.Stub() {
					public void onBufferingUpdate(int percent)
					throws RemoteException {
						owningMediaPlayer.lock.lock();
						try {
							if ((owningMediaPlayer.onBufferingUpdateListener != null)
								&& (owningMediaPlayer.mpi == ServiceBackedMediaPlayer.this)) {
								owningMediaPlayer.onBufferingUpdateListener.onBufferingUpdate(owningMediaPlayer, percent);
							}
						}
						finally {
							owningMediaPlayer.lock.unlock();
						}
					}
				};
			}
			iface.registerOnBufferingUpdateCallback(
				ServiceBackedMediaPlayer.this.sessionId,
				mOnBufferingUpdateCallback);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}

	private IOnCompletionListenerCallback_0_8.Stub mOnCompletionCallback = null;
	private void setOnCompletionCallback(IPlayMedia_0_8 iface) {
		try {
			if (this.mOnCompletionCallback == null) {
				this.mOnCompletionCallback = new IOnCompletionListenerCallback_0_8.Stub() {
					public void onCompletion() throws RemoteException {
						owningMediaPlayer.lock.lock();
						Log.d(SBMP_TAG, "onCompletionListener being called");
						try {
							if (owningMediaPlayer.onCompletionListener != null) {
								owningMediaPlayer.onCompletionListener.onCompletion(owningMediaPlayer);
							}
						}
						finally {
							owningMediaPlayer.lock.unlock();
						}
					}
				};
			}
			iface.registerOnCompletionCallback(
				ServiceBackedMediaPlayer.this.sessionId,
				this.mOnCompletionCallback);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}
	
	private IOnErrorListenerCallback_0_8.Stub mOnErrorCallback = null;
	private void setOnErrorCallback(IPlayMedia_0_8 iface) {
		try {
			if (this.mOnErrorCallback == null) {
				this.mOnErrorCallback = new IOnErrorListenerCallback_0_8.Stub() {
					public boolean onError(int what, int extra) throws RemoteException {
						owningMediaPlayer.lock.lock();
						try {
							if (owningMediaPlayer.onErrorListener != null) {
								return owningMediaPlayer.onErrorListener.onError(owningMediaPlayer, what, extra);
							}
							return false;
						}
						finally {
							owningMediaPlayer.lock.unlock();
						}
					}
				};
			}
			iface.registerOnErrorCallback(
				ServiceBackedMediaPlayer.this.sessionId,
				this.mOnErrorCallback);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}
	
	private IOnInfoListenerCallback_0_8.Stub mOnInfoCallback = null;
	private void setOnInfoCallback(IPlayMedia_0_8 iface) {
		try {
			if (this.mOnInfoCallback == null) {
				this.mOnInfoCallback = new IOnInfoListenerCallback_0_8.Stub() {
					public boolean onInfo(int what, int extra) throws RemoteException {
						owningMediaPlayer.lock.lock();
						try {
							if ((owningMediaPlayer.onInfoListener != null)
								&& (owningMediaPlayer.mpi == ServiceBackedMediaPlayer.this)) {
								return owningMediaPlayer.onInfoListener.onInfo(owningMediaPlayer, what, extra);
							}
						}
						finally {
							owningMediaPlayer.lock.unlock();
						}
						return false;
					}
				};
			}
			iface.registerOnInfoCallback(
				ServiceBackedMediaPlayer.this.sessionId,
				this.mOnInfoCallback);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}

	private IOnPitchAdjustmentAvailableChangedListenerCallback_0_8.Stub mOnPitchAdjustmentAvailableChangedCallback = null;
	private void setOnPitchAdjustmentAvailableChangedListener(IPlayMedia_0_8 iface) {
		try {
			if (this.mOnPitchAdjustmentAvailableChangedCallback == null) {
				this.mOnPitchAdjustmentAvailableChangedCallback = new IOnPitchAdjustmentAvailableChangedListenerCallback_0_8.Stub() {
					public void onPitchAdjustmentAvailableChanged(
							boolean pitchAdjustmentAvailable)
							throws RemoteException {
						owningMediaPlayer.lock.lock();
						try {
							if (owningMediaPlayer.onPitchAdjustmentAvailableChangedListener != null) {
								owningMediaPlayer.onPitchAdjustmentAvailableChangedListener.onPitchAdjustmentAvailableChanged(owningMediaPlayer, pitchAdjustmentAvailable);
							}
						}
						finally {
							owningMediaPlayer.lock.unlock();
						}
					}
				};
			}
			iface.registerOnPitchAdjustmentAvailableChangedCallback(
				ServiceBackedMediaPlayer.this.sessionId,
				this.mOnPitchAdjustmentAvailableChangedCallback);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}

	private IOnPreparedListenerCallback_0_8.Stub mOnPreparedCallback = null;
	private void setOnPreparedCallback(IPlayMedia_0_8 iface) {
		try {
			if (this.mOnPreparedCallback == null) {
				this.mOnPreparedCallback = new IOnPreparedListenerCallback_0_8.Stub() {
					public void onPrepared() throws RemoteException {
						owningMediaPlayer.lock.lock();
						Log.d(SBMP_TAG, "setOnPreparedCallback.mOnPreparedCallback.onPrepared 1050");
						try {
							Log.d(SBMP_TAG, "owningMediaPlayer.onPreparedListener is " + ((owningMediaPlayer.onPreparedListener == null) ? "null" : "non-null"));
							Log.d(SBMP_TAG, "owningMediaPlayer.mpi is " + ((owningMediaPlayer.mpi == ServiceBackedMediaPlayer.this) ? "this" : "not this"));
							ServiceBackedMediaPlayer.this.lockMuteOnPreparedCount.lock();
							try {
								if (ServiceBackedMediaPlayer.this.muteOnPreparedCount > 0) {
									ServiceBackedMediaPlayer.this.muteOnPreparedCount--;
								}
								else {
									ServiceBackedMediaPlayer.this.muteOnPreparedCount = 0;
									if (ServiceBackedMediaPlayer.this.owningMediaPlayer.onPreparedListener != null) {
										owningMediaPlayer.onPreparedListener.onPrepared(owningMediaPlayer);
									}
								}
							}
							finally {
								ServiceBackedMediaPlayer.this.lockMuteOnPreparedCount.unlock();
							}
						}
						finally {
							owningMediaPlayer.lock.unlock();
						}
					}
				};
			}
			iface.registerOnPreparedCallback(
				ServiceBackedMediaPlayer.this.sessionId,
				this.mOnPreparedCallback);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}

	private IOnSeekCompleteListenerCallback_0_8.Stub mOnSeekCompleteCallback = null;
	private void setOnSeekCompleteCallback(IPlayMedia_0_8 iface) {
		try {
			if (this.mOnSeekCompleteCallback == null) {
				this.mOnSeekCompleteCallback = new IOnSeekCompleteListenerCallback_0_8.Stub() {
					public void onSeekComplete() throws RemoteException {
						Log.d(SBMP_TAG, "onSeekComplete() 941");
						owningMediaPlayer.lock.lock();
						try {
							if (ServiceBackedMediaPlayer.this.muteOnSeekCount > 0) {
								Log.d(SBMP_TAG, "The next " + ServiceBackedMediaPlayer.this.muteOnSeekCount + " seek events are muted (counting this one)");
								ServiceBackedMediaPlayer.this.muteOnSeekCount--;
							}
							else {
								ServiceBackedMediaPlayer.this.muteOnSeekCount = 0;
								Log.d(SBMP_TAG, "Attempting to invoke next seek event");
								if (ServiceBackedMediaPlayer.this.owningMediaPlayer.onSeekCompleteListener != null) {
									Log.d(SBMP_TAG, "Invoking onSeekComplete");
									owningMediaPlayer.onSeekCompleteListener.onSeekComplete(owningMediaPlayer);
								}
							}
						}
						finally {
							owningMediaPlayer.lock.unlock();
						}
					}
				};
			}
			iface.registerOnSeekCompleteCallback(
				ServiceBackedMediaPlayer.this.sessionId,
				this.mOnSeekCompleteCallback);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}
	
	private IOnSpeedAdjustmentAvailableChangedListenerCallback_0_8.Stub mOnSpeedAdjustmentAvailableChangedCallback = null;
	private void setOnSpeedAdjustmentAvailableChangedCallback(IPlayMedia_0_8 iface) {
		try {
			Log.d(SBMP_TAG, "Setting the service of on speed adjustment available changed");
			if (this.mOnSpeedAdjustmentAvailableChangedCallback == null) {
				this.mOnSpeedAdjustmentAvailableChangedCallback = new IOnSpeedAdjustmentAvailableChangedListenerCallback_0_8.Stub() {
					public void onSpeedAdjustmentAvailableChanged(
							boolean speedAdjustmentAvailable)
							throws RemoteException {
						owningMediaPlayer.lock.lock();
						try {
							if (owningMediaPlayer.onSpeedAdjustmentAvailableChangedListener != null) {
								owningMediaPlayer.onSpeedAdjustmentAvailableChangedListener.onSpeedAdjustmentAvailableChanged(owningMediaPlayer, speedAdjustmentAvailable);
							}
						}
						finally {
							owningMediaPlayer.lock.unlock();
						}
					}
				};
			}
			iface.registerOnSpeedAdjustmentAvailableChangedCallback(
				ServiceBackedMediaPlayer.this.sessionId,
				this.mOnSpeedAdjustmentAvailableChangedCallback);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}

	/**
	 * Functions identically to android.media.MediaPlayer.start()
	 * Starts a track playing
	 */
	@Override
	public void start() {
		Log.d(SBMP_TAG, "start()");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		try {
			pmInterface.start(ServiceBackedMediaPlayer.this.sessionId);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}

	/**
	 * Functions identically to android.media.MediaPlayer.stop()
	 * Stops a track playing and resets its position to the start.
	 */
	@Override
	public void stop() {
		Log.d(SBMP_TAG, "stop()");
		if (pmInterface == null) {
			if (!ConnectPlayMediaService()) {
				ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			}
		}
		try {
			pmInterface.stop(ServiceBackedMediaPlayer.this.sessionId);
		} catch (RemoteException e) {
			e.printStackTrace();
			ServiceBackedMediaPlayer.this.error(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
		}
	}
}