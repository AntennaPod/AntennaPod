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

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

public class AndroidMediaPlayer extends MediaPlayerImpl {
	private final static String AMP_TAG = "AocateAndroidMediaPlayer";

	// private static final long TIMEOUT_DURATION_MS = 500;
	
	android.media.MediaPlayer mp = null;
	
	private android.media.MediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener = new android.media.MediaPlayer.OnBufferingUpdateListener() {
		public void onBufferingUpdate(android.media.MediaPlayer mp, int percent) {
			if (owningMediaPlayer != null) {
				owningMediaPlayer.lock.lock();
				try {
					if ((owningMediaPlayer.onBufferingUpdateListener != null)
						&& (owningMediaPlayer.mpi == AndroidMediaPlayer.this)) {
						owningMediaPlayer.onBufferingUpdateListener.onBufferingUpdate(owningMediaPlayer, percent);
					}
				}
				finally {
					owningMediaPlayer.lock.unlock();
				}
			}
				
		}
	};
	
	private android.media.MediaPlayer.OnCompletionListener onCompletionListener = new android.media.MediaPlayer.OnCompletionListener() {
		public void onCompletion(android.media.MediaPlayer mp) {
			Log.d(AMP_TAG, "onCompletionListener being called");
			if (owningMediaPlayer != null) {
				owningMediaPlayer.lock.lock();
				try {
					if (owningMediaPlayer.onCompletionListener != null) {
						owningMediaPlayer.onCompletionListener.onCompletion(owningMediaPlayer);
					}
				}
				finally {
					owningMediaPlayer.lock.unlock();
				}
			}
		}
	};
	
	private android.media.MediaPlayer.OnErrorListener onErrorListener = new android.media.MediaPlayer.OnErrorListener() {
		public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
			// Once we're in errored state, any received messages are going to be junked
			if (owningMediaPlayer != null) {
				owningMediaPlayer.lock.lock();
				try {
					if (owningMediaPlayer.onErrorListener != null) {
						return owningMediaPlayer.onErrorListener.onError(owningMediaPlayer, what, extra);					
					}
				}
				finally {
					owningMediaPlayer.lock.unlock();
				}
			}
			return false;
		}
	};	
	
	private android.media.MediaPlayer.OnInfoListener onInfoListener = new android.media.MediaPlayer.OnInfoListener() {
		public boolean onInfo(android.media.MediaPlayer mp, int what, int extra) {
			if (owningMediaPlayer != null) {
				owningMediaPlayer.lock.lock();
				try {
					if ((owningMediaPlayer.onInfoListener != null)
						&& (owningMediaPlayer.mpi == AndroidMediaPlayer.this)) { 
						return owningMediaPlayer.onInfoListener.onInfo(owningMediaPlayer, what, extra);
					}
				}
				finally { 
					owningMediaPlayer.lock.unlock();
				}
			}
			return false;
		}
	};
	
	// We have to assign this.onPreparedListener because the
	// onPreparedListener in owningMediaPlayer sets the state
	// to PREPARED.  Due to prepareAsync, that's the only
	// reasonable place to do it
	// The others it just didn't make sense to have a setOnXListener that didn't use the parameter
	private android.media.MediaPlayer.OnPreparedListener onPreparedListener = new android.media.MediaPlayer.OnPreparedListener() {
			public void onPrepared(android.media.MediaPlayer mp) {
				Log.d(AMP_TAG, "Calling onPreparedListener.onPrepared()");
				if (AndroidMediaPlayer.this.owningMediaPlayer != null) {
					AndroidMediaPlayer.this.lockMuteOnPreparedCount.lock();
					try {
						if (AndroidMediaPlayer.this.muteOnPreparedCount > 0) {
							AndroidMediaPlayer.this.muteOnPreparedCount--;
						}
						else {
							AndroidMediaPlayer.this.muteOnPreparedCount = 0;
							if (AndroidMediaPlayer.this.owningMediaPlayer.onPreparedListener != null) {
								Log.d(AMP_TAG, "Invoking AndroidMediaPlayer.this.owningMediaPlayer.onPreparedListener.onPrepared");
								AndroidMediaPlayer.this.owningMediaPlayer.onPreparedListener.onPrepared(AndroidMediaPlayer.this.owningMediaPlayer);
							}
						}
					}
					finally {
						AndroidMediaPlayer.this.lockMuteOnPreparedCount.unlock();
					}
					if (owningMediaPlayer.mpi != AndroidMediaPlayer.this) {
						Log.d(AMP_TAG, "owningMediaPlayer has changed implementation");
					}
				}
			}
		};

	private android.media.MediaPlayer.OnSeekCompleteListener onSeekCompleteListener = new android.media.MediaPlayer.OnSeekCompleteListener() {
		public void onSeekComplete(android.media.MediaPlayer mp) {
			if (owningMediaPlayer != null) {
				owningMediaPlayer.lock.lock();
				try {
					lockMuteOnSeekCount.lock();
					try {
						if (AndroidMediaPlayer.this.muteOnSeekCount > 0) {
							AndroidMediaPlayer.this.muteOnSeekCount--;
						}
						else {
							AndroidMediaPlayer.this.muteOnSeekCount = 0;
							if (AndroidMediaPlayer.this.owningMediaPlayer.onSeekCompleteListener != null) {
								owningMediaPlayer.onSeekCompleteListener.onSeekComplete(owningMediaPlayer);
							}
						}
					}
					finally {
						lockMuteOnSeekCount.unlock();
					}
				}
				finally {
					owningMediaPlayer.lock.unlock();
				}
			}
		}
	};
	
	public AndroidMediaPlayer(com.aocate.media.MediaPlayer owningMediaPlayer, Context context) {
		super(owningMediaPlayer, context);
		
		mp = new MediaPlayer();
		
//		final ReentrantLock lock = new ReentrantLock();
//		Handler handler = new Handler(Looper.getMainLooper()) {
//            @Override
//            public void handleMessage(Message msg) {
//            	Log.d(AMP_TAG, "Instantiating new AndroidMediaPlayer from Handler");
//            	lock.lock();
//            	if (mp == null) {
//            		mp = new MediaPlayer();
//            	}
//            	lock.unlock();
//            }
//        };
//		
//        long endTime = System.currentTimeMillis() + TIMEOUT_DURATION_MS;
//        
//        while (true) {
//        	// Retry messages until mp isn't null or it's time to give up
//        	handler.sendMessage(handler.obtainMessage());
//        	if ((mp != null)
//        		|| (endTime < System.currentTimeMillis())) {
//        		break;
//        	}
//        	try {
//				Thread.sleep(50);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//        }
        
        if (mp == null) {
        	throw new IllegalStateException("Did not instantiate android.media.MediaPlayer successfully");
        }
        
        mp.setOnBufferingUpdateListener(this.onBufferingUpdateListener);
		mp.setOnCompletionListener(this.onCompletionListener);
		mp.setOnErrorListener(this.onErrorListener);
		mp.setOnInfoListener(this.onInfoListener);
		Log.d(AMP_TAG, " ++++++++++++++++++++++++++++++++ Setting prepared listener to this.onPreparedListener");
		mp.setOnPreparedListener(this.onPreparedListener);
		mp.setOnSeekCompleteListener(this.onSeekCompleteListener);
	}

	@Override
	public boolean canSetPitch() {
		return false;
	}

	@Override
	public boolean canSetSpeed() {
		return false;
	}

	@Override
	public float getCurrentPitchStepsAdjustment() {
		return 0;
	}

	@Override
	public int getCurrentPosition() {
		owningMediaPlayer.lock.lock();
		try {
			return mp.getCurrentPosition();
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}

	@Override
	public float getCurrentSpeedMultiplier() {
		return 1f;
	}

	@Override
	public int getDuration() {
		owningMediaPlayer.lock.lock();
		try {
			return mp.getDuration();
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}

	@Override
	public float getMaxSpeedMultiplier() {
		return 1f;
	}

	@Override
	public float getMinSpeedMultiplier() {
		return 1f;
	}

	@Override
	public boolean isLooping() {
		owningMediaPlayer.lock.lock();
		try {
			return mp.isLooping();
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}

	@Override
	public boolean isPlaying() {
		owningMediaPlayer.lock.lock();
		try {
			return mp.isPlaying();
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}

	@Override
	public void pause() {
		owningMediaPlayer.lock.lock();
		try {
			mp.pause();
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}

	@Override
	public void prepare() throws IllegalStateException, IOException {
		owningMediaPlayer.lock.lock();
		Log.d(AMP_TAG, "prepare()");
		try {
			mp.prepare();
			Log.d(AMP_TAG, "Finish prepare()");
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}

	@Override
	public void prepareAsync() {
		mp.prepareAsync();
	}

	@Override
	public void release() {
		owningMediaPlayer.lock.lock();
		try {
			if (mp != null) {
				Log.d(AMP_TAG, "mp.release()");
				mp.release();
			}
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}

	@Override
	public void reset() {
		owningMediaPlayer.lock.lock();
		try {
			mp.reset();
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}

	@Override
	public void seekTo(int msec) throws IllegalStateException {
		owningMediaPlayer.lock.lock();
		try {
			mp.setOnSeekCompleteListener(this.onSeekCompleteListener);
			mp.seekTo(msec);
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}

	@Override
	public void setAudioStreamType(int streamtype) {
		owningMediaPlayer.lock.lock();
		try {
			mp.setAudioStreamType(streamtype);
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}

	@Override
	public void setDataSource(Context context, Uri uri)
			throws IllegalArgumentException, IllegalStateException, IOException {
		owningMediaPlayer.lock.lock();
		try {
			Log.d(AMP_TAG, "setDataSource(context, " + uri.toString() + ")");
			mp.setDataSource(context, uri);
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}

	@Override
	public void setDataSource(String path) throws IllegalArgumentException,
			IllegalStateException, IOException {
		owningMediaPlayer.lock.lock();
		try {
			Log.d(AMP_TAG, "setDataSource(" + path + ")");
			mp.setDataSource(path);
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}

	@Override
	public void setEnableSpeedAdjustment(boolean enableSpeedAdjustment) {
		// Can't!
	}

	@Override
	public void setLooping(boolean loop) {
		owningMediaPlayer.lock.lock();
		try {
			mp.setLooping(loop);
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}

	@Override
	public void setPitchStepsAdjustment(float pitchSteps) {
		// Can't!
	}
	
	@Override
	public void setPlaybackPitch(float f) {
		// Can't!
	}

	@Override
	public void setPlaybackSpeed(float f) {
		// Can't!
		Log.d(AMP_TAG, "setPlaybackSpeed(" + f + ")");
	}

	@Override
	public void setSpeedAdjustmentAlgorithm(int algorithm) {
		// Can't!
		Log.d(AMP_TAG, "setSpeedAdjustmentAlgorithm(" + algorithm + ")");
	}

	@Override
	public void setVolume(float leftVolume, float rightVolume) {
		owningMediaPlayer.lock.lock();
		try {
			mp.setVolume(leftVolume, rightVolume);
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}

	@Override
	public void setWakeMode(Context context, int mode) {
		owningMediaPlayer.lock.lock();
		try {
			if (mode != 0) {
				mp.setWakeMode(context, mode);
			}
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}

	@Override
	public void start() {
		owningMediaPlayer.lock.lock();
		try {
			mp.start();
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}

	@Override
	public void stop() {
		owningMediaPlayer.lock.lock();
		try {
			mp.stop();
		}
		finally {
			owningMediaPlayer.lock.unlock();
		}
	}
}
