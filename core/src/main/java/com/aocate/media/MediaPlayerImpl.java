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
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

public abstract class MediaPlayerImpl {
	private static final String MPI_TAG = "AocateMediaPlayerImpl";
	protected final MediaPlayer owningMediaPlayer;
	protected final Context mContext;
	protected int muteOnPreparedCount = 0;
	protected int muteOnSeekCount = 0;

	public MediaPlayerImpl(MediaPlayer owningMediaPlayer, Context context) {
		this.owningMediaPlayer = owningMediaPlayer;

		this.mContext = context;
	}

	public abstract boolean canSetPitch();
	
	public abstract boolean canSetSpeed();
	
	public abstract float getCurrentPitchStepsAdjustment();
	
	public abstract int getCurrentPosition();
	
	public abstract float getCurrentSpeedMultiplier();
	
	public abstract int getDuration();
	
	public abstract float getMaxSpeedMultiplier();
	
	public abstract float getMinSpeedMultiplier();
	
	public abstract boolean isLooping();
	
	public abstract boolean isPlaying();
	
	public abstract void pause();
	
	public abstract void prepare() throws IllegalStateException, IOException;
	
	public abstract void prepareAsync();
	
	public abstract void release();
	
	public abstract void reset();
	
	public abstract void seekTo(int msec) throws IllegalStateException;
	
	public abstract void setAudioStreamType(int streamtype);
	
	public abstract void setDataSource(Context context, Uri uri) throws IllegalArgumentException, IllegalStateException, IOException;
	
	public abstract void setDataSource(String path) throws IllegalArgumentException, IllegalStateException, IOException;
	
	public abstract void setEnableSpeedAdjustment(boolean enableSpeedAdjustment);
	
	public abstract void setLooping(boolean loop);
	
	public abstract void setPitchStepsAdjustment(float pitchSteps);
	
	public abstract void setPlaybackPitch(float f);
	
	public abstract void setPlaybackSpeed(float f);
	
	public abstract void setSpeedAdjustmentAlgorithm(int algorithm);
	
	public abstract void setVolume(float leftVolume, float rightVolume);
	
	public abstract void setWakeMode(Context context, int mode);
	
	public abstract void start();
	
	public abstract void stop();

	protected ReentrantLock lockMuteOnPreparedCount = new ReentrantLock(); 
	public void muteNextOnPrepare() {
		lockMuteOnPreparedCount.lock();
		Log.d(MPI_TAG, "muteNextOnPrepare()");
		try {
			this.muteOnPreparedCount++;
		}
		finally {
			lockMuteOnPreparedCount.unlock();
		}
	}

	protected ReentrantLock lockMuteOnSeekCount = new ReentrantLock();
	public void muteNextSeek() {
		lockMuteOnSeekCount.lock();
		Log.d(MPI_TAG, "muteNextOnSeek()");
		try {
			this.muteOnSeekCount++;
		}
		finally {
			lockMuteOnSeekCount.unlock();
		}
	}
}
