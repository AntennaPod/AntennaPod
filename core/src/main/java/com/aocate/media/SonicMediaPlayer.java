//Copyright 2012 James Falcon
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.

package com.aocate.media;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import org.vinuxproject.sonic.Sonic;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.locks.ReentrantLock;

import de.danoeh.antennapod.core.preferences.UserPreferences;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class SonicMediaPlayer extends AbstractMediaPlayer {

    private static final String TAG = SonicMediaPlayer.class.getSimpleName();

    public interface OnBufferingUpdateListener {
        void onBufferingUpdate(int percent);
    }

    public interface OnCompletionListener {
        void onCompletion();
    }

    public interface OnErrorListener {
        boolean onError(int extra);
    }

    public interface OnInfoListener {
        boolean onInfo(int extra);
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
        void onPrepared();
    }

    public interface OnSeekCompleteListener {
        void onSeekComplete();
    }

    protected final MediaPlayer mMediaPlayer;
    private AudioTrack mTrack;
    private Sonic mSonic;
    private MediaExtractor mExtractor;
    private MediaCodec mCodec;
    private Thread mDecoderThread;
    private String mPath;
    private Uri mUri;
    private final ReentrantLock mLock;
    private final Object mDecoderLock;
    private boolean mContinue;
    private boolean mIsDecoding;
    private long mDuration;
    private float mCurrentSpeed;
    private float mCurrentPitch;
    private int mCurrentState;
    private final Context mContext;
    private PowerManager.WakeLock mWakeLock = null;

    private final static int TRACK_NUM = 0;
    private final static String TAG_TRACK = "PrestissimoTrack";

    private final static int STATE_IDLE = 0;
    private final static int STATE_INITIALIZED = 1;
    private final static int STATE_PREPARING = 2;
    private final static int STATE_PREPARED = 3;
    private final static int STATE_STARTED = 4;
    private final static int STATE_PAUSED = 5;
    private final static int STATE_STOPPED = 6;
    private final static int STATE_PLAYBACK_COMPLETED = 7;
    private final static int STATE_END = 8;
    private final static int STATE_ERROR = 9;

    // Not available in API 16 :(
    private final static int MEDIA_ERROR_MALFORMED = 0xfffffc11;
    private final static int MEDIA_ERROR_IO = 0xfffffc14;

    public SonicMediaPlayer(MediaPlayer owningMediaPlayer, Context context) {
        super(owningMediaPlayer, context);
        mMediaPlayer = owningMediaPlayer;
        mCurrentState = STATE_IDLE;
        mCurrentSpeed = 1.0f;
        mCurrentPitch = 1.0f;
        mContinue = false;
        mIsDecoding = false;
        mContext = context;
        mPath = null;
        mUri = null;
        mLock = new ReentrantLock();
        mDecoderLock = new Object();
    }


    @Override
    public boolean canSetPitch() {
        return true;
    }

    @Override
    public boolean canSetSpeed() {
        return true;
    }

    @Override
    public float getCurrentPitchStepsAdjustment() {
        return mCurrentPitch;
    }

    public int getCurrentPosition() {
        switch (mCurrentState) {
            case STATE_ERROR:
                error();
                break;
            default:
                return (int) (mExtractor.getSampleTime() / 1000);
        }
        return 0;
    }

    @Override
    public float getCurrentSpeedMultiplier() {
        return mCurrentSpeed;
    }

    public int getDuration() {
        switch (mCurrentState) {
            case STATE_INITIALIZED:
            case STATE_IDLE:
            case STATE_ERROR:
                error();
                break;
            default:
                return (int) (mDuration / 1000);
        }
        return 0;
    }

    @Override
    public float getMaxSpeedMultiplier() {
        return 4.0f;
    }

    @Override
    public float getMinSpeedMultiplier() {
        return 0.5f;
    }

    @Override
    public boolean isLooping() {
        return false;
    }

    public boolean isPlaying() {
        switch (mCurrentState) {
            case STATE_ERROR:
                error();
                break;
            default:
                return mCurrentState == STATE_STARTED;
        }
        return false;
    }

    public void pause() {
        Log.d(TAG, "pause(), current state: " + mCurrentState);
        switch (mCurrentState) {
            case STATE_STARTED:
            case STATE_PAUSED:
                mTrack.pause();
                mCurrentState = STATE_PAUSED;
                Log.d(TAG_TRACK, "State changed to STATE_PAUSED");
                break;
            default:
                error();
        }
    }

    public void prepare() {
        Log.d(TAG, "prepare(), current state: " + mCurrentState);
        switch (mCurrentState) {
            case STATE_INITIALIZED:
            case STATE_STOPPED:
                try {
                    initStream();
                } catch (IOException e) {
                    Log.e(TAG_TRACK, "Failed setting data source!", e);
                    error();
                    return;
                }
                mCurrentState = STATE_PREPARED;
                Log.d(TAG_TRACK, "State changed to STATE_PREPARED");
                if(owningMediaPlayer.onPreparedListener != null) {
                    owningMediaPlayer.onPreparedListener.onPrepared(owningMediaPlayer);
                }
                break;
            default:
                error();
        }
    }

    public void prepareAsync() {
        switch (mCurrentState) {
            case STATE_INITIALIZED:
            case STATE_STOPPED:
                mCurrentState = STATE_PREPARING;
                Log.d(TAG_TRACK, "State changed to STATE_PREPARING");

                Thread t = new Thread(() -> {
                    try {
                        initStream();
                    } catch (IOException e) {
                        Log.e(TAG_TRACK, "Failed setting data source!", e);
                        error();
                        return;
                    }
                    if (mCurrentState != STATE_ERROR) {
                        mCurrentState = STATE_PREPARED;
                        Log.d(TAG_TRACK, "State changed to STATE_PREPARED");
                    }
                    if(owningMediaPlayer.onPreparedListener != null) {
                        owningMediaPlayer.onPreparedListener.onPrepared(owningMediaPlayer);
                    }
                });
                t.setDaemon(true);
                t.start();
                break;
            default:
                error();
        }
    }

    public void stop() {
        switch (mCurrentState) {
            case STATE_PREPARED:
            case STATE_STARTED:
            case STATE_STOPPED:
            case STATE_PAUSED:
            case STATE_PLAYBACK_COMPLETED:
                mCurrentState = STATE_STOPPED;
                Log.d(TAG_TRACK, "State changed to STATE_STOPPED");
                mContinue = false;
                mTrack.pause();
                mTrack.flush();
                break;
            default:
                error();
        }
    }

    public void start() {
        switch (mCurrentState) {
            case STATE_PREPARED:
            case STATE_PLAYBACK_COMPLETED:
                mCurrentState = STATE_STARTED;
                Log.d(TAG, "State changed to STATE_STARTED");
                mContinue = true;
                mTrack.play();
                decode();
            case STATE_STARTED:
                break;
            case STATE_PAUSED:
                mCurrentState = STATE_STARTED;
                Log.d(TAG, "State changed to STATE_STARTED");
                synchronized (mDecoderLock) {
                    mDecoderLock.notify();
                }
                mTrack.play();
                break;
            default:
                mCurrentState = STATE_ERROR;
                Log.d(TAG, "State changed to STATE_ERROR in start");
                if (mTrack != null) {
                    error();
                } else {
                    Log.d("start",
                            "Attempting to start while in idle after construction.  Not allowed by no callbacks called");
                }
        }
    }

    public void release() {
        reset();
        mCurrentState = STATE_END;
    }

    public void reset() {
        mLock.lock();
        mContinue = false;
        try {
            if (mDecoderThread != null
                    && mCurrentState != STATE_PLAYBACK_COMPLETED) {
                while (mIsDecoding) {
                    synchronized (mDecoderLock) {
                        mDecoderLock.notify();
                        mDecoderLock.wait();
                    }
                }
            }
        } catch (InterruptedException e) {
            Log.e(TAG_TRACK,
                    "Interrupted in reset while waiting for decoder thread to stop.",
                    e);
        }
        if (mCodec != null) {
            mCodec.release();
            mCodec = null;
        }
        if (mExtractor != null) {
            mExtractor.release();
            mExtractor = null;
        }
        if (mTrack != null) {
            mTrack.release();
            mTrack = null;
        }
        mCurrentState = STATE_IDLE;
        Log.d(TAG_TRACK, "State changed to STATE_IDLE");
        mLock.unlock();
    }

    public void seekTo(final int msec) {
        switch (mCurrentState) {
            case STATE_PREPARED:
            case STATE_STARTED:
            case STATE_PAUSED:
            case STATE_PLAYBACK_COMPLETED:
                Thread t = new Thread(() -> {
                    mLock.lock();
                    if (mTrack == null) {
                        return;
                    }
                    mTrack.flush();
                    mExtractor.seekTo(((long) msec * 1000), MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                    Log.d(TAG, "seek completed");
                    if(owningMediaPlayer.onSeekCompleteListener != null) {
                        owningMediaPlayer.onSeekCompleteListener.onSeekComplete(owningMediaPlayer);
                    }
                    mLock.unlock();
                });
                t.setDaemon(true);
                t.start();
                break;
            default:
                error();
        }
    }

    @Override
    public void setAudioStreamType(int streamtype) {
        return;
    }

    @Override
    public void setEnableSpeedAdjustment(boolean enableSpeedAdjustment) {
        return;
    }

    @Override
    public void setLooping(boolean loop) {
        return;
    }

    @Override
    public void setPitchStepsAdjustment(float pitchSteps) {
        mCurrentPitch += pitchSteps;
    }

    @Override
    public void setPlaybackPitch(float f) {
        mCurrentSpeed = f;
    }

    @Override
    public void setPlaybackSpeed(float f) {
        mCurrentSpeed = f;
    }

    @Override
    public void setDataSource(String path) {
        switch (mCurrentState) {
            case STATE_IDLE:
                mPath = path;
                mCurrentState = STATE_INITIALIZED;
                Log.d(TAG_TRACK, "Moving state to STATE_INITIALIZED");
                break;
            default:
                error();
        }
    }

    @Override
    public void setDataSource(Context context, Uri uri) {
        switch (mCurrentState) {
            case STATE_IDLE:
                mUri = uri;
                mCurrentState = STATE_INITIALIZED;
                Log.d(TAG_TRACK, "Moving state to STATE_INITIALIZED");
                break;
            default:
                error();
        }
    }

    public void setVolume(float left, float right) {
        // Pass call directly to AudioTrack if available.
        if (null != mTrack) {
            mTrack.setStereoVolume(left, right);
        }
    }

    @Override
    public void setWakeMode(Context context, int mode) {
        boolean wasHeld = false;
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                wasHeld = true;
                mWakeLock.release();
            }
            mWakeLock = null;
        }

        if(mode > 0) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(mode, this.getClass().getName());
            mWakeLock.setReferenceCounted(false);
            if (wasHeld) {
                mWakeLock.acquire();
            }
        }
    }

    public void error() {
        error(0);
    }

    public void error(int extra) {
        Log.e(TAG_TRACK, "Moved to error state!");
        mCurrentState = STATE_ERROR;
        if(owningMediaPlayer.onErrorListener != null) {
            boolean handled = owningMediaPlayer.onErrorListener.onError(owningMediaPlayer, 0, extra);
            if (!handled && owningMediaPlayer.onCompletionListener != null) {
                owningMediaPlayer.onCompletionListener.onCompletion(owningMediaPlayer);
            }
        }
    }

    private int findFormatFromChannels(int numChannels) {
        switch (numChannels) {
            case 1:
                return AudioFormat.CHANNEL_OUT_MONO;
            case 2:
                return AudioFormat.CHANNEL_OUT_STEREO;
            default:
                return -1; // Error
        }
    }

    public void initStream() throws IOException {
        mLock.lock();
        mExtractor = new MediaExtractor();
        if (mPath != null) {
            mExtractor.setDataSource(mPath);
        } else if (mUri != null) {
            mExtractor.setDataSource(mContext, mUri, null);
        } else {
            throw new IOException();
        }

        final MediaFormat oFormat = mExtractor.getTrackFormat(TRACK_NUM);
        int sampleRate = oFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = oFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        final String mime = oFormat.getString(MediaFormat.KEY_MIME);
        mDuration = oFormat.getLong(MediaFormat.KEY_DURATION);

        Log.v(TAG_TRACK, "Sample rate: " + sampleRate);
        Log.v(TAG_TRACK, "Mime type: " + mime);

        initDevice(sampleRate, channelCount);
        mExtractor.selectTrack(TRACK_NUM);
        mCodec = MediaCodec.createDecoderByType(mime);
        mCodec.configure(oFormat, null, null, 0);
        mLock.unlock();
    }

    private void initDevice(int sampleRate, int numChannels) {
        mLock.lock();
        final int format = findFormatFromChannels(numChannels);
        final int minSize = AudioTrack.getMinBufferSize(sampleRate, format,
                AudioFormat.ENCODING_PCM_16BIT);
        mTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, format,
                AudioFormat.ENCODING_PCM_16BIT, minSize * 4,
                AudioTrack.MODE_STREAM);
        mSonic = new Sonic(sampleRate, numChannels);
        mLock.unlock();
    }

    public void decode() {
        mDecoderThread = new Thread(() -> {
            float fAGC = 1;
            short lastSample = 0;
            mIsDecoding = true;
            mCodec.start();

            ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = mCodec.getOutputBuffers();

            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;

            while (!sawInputEOS && !sawOutputEOS && mContinue) {
                if (mCurrentState == STATE_PAUSED) {
                    System.out.println("Decoder changed to PAUSED");
                    try {
                        synchronized (mDecoderLock) {
                            mDecoderLock.wait();
                            System.out.println("Done with wait");
                        }
                    } catch (InterruptedException e) {
                        // Purposely not doing anything here
                    }
                    continue;
                }

                if (null != mSonic) {
                    mSonic.setSpeed(mCurrentSpeed);
                    mSonic.setPitch(mCurrentPitch);
                }

                int inputBufIndex = mCodec.dequeueInputBuffer(200);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = inputBuffers[inputBufIndex];
                    int sampleSize = mExtractor.readSampleData(dstBuf, 0);
                    long presentationTimeUs = 0;
                    if (sampleSize < 0) {
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        presentationTimeUs = mExtractor.getSampleTime();
                    }
                    mCodec.queueInputBuffer(
                            inputBufIndex,
                            0,
                            sampleSize,
                            presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    : 0);
                    if (!sawInputEOS) {
                        mExtractor.advance();
                    }
                }

                final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                byte[] modifiedSamples = new byte[info.size];

                int res;
                do {
                    res = mCodec.dequeueOutputBuffer(info, 200);
                    if (res >= 0) {
                        int outputBufIndex = res;
                        final byte[] chunk = new byte[info.size];
                        outputBuffers[res].get(chunk);
                        outputBuffers[res].clear();

                        if (chunk.length > 0) {
                            mSonic.writeBytesToStream(chunk, chunk.length);
                        } else {
                            mSonic.flushStream();
                        }
                        int available = mSonic.samplesAvailable();
                        if (available > 0) {
                            if (modifiedSamples.length < available) {
                                modifiedSamples = new byte[available];
                            }
                            mSonic.readBytesFromStream(modifiedSamples, available);
                            // AGC
                            if(UserPreferences.useNormalizer()) {
                                ByteBuffer bf = ByteBuffer.wrap(modifiedSamples);
                                bf.order(ByteOrder.LITTLE_ENDIAN);
                                ShortBuffer s = bf.asShortBuffer();
                                final int length = modifiedSamples.length / 2;

                                int max = 0;
                                for (int i = 0; i < length; i++) {
                                    max = Math.max(max, Math.abs(s.get(i)));
                                }

                                final int maxTarget = (int) (max * fAGC);
                                if (maxTarget > 30720) {
                                    fAGC = 1.0f * 32768 / max;
                                } else if (maxTarget < 12288) {
                                    fAGC *= 1.005;
                                } else if (maxTarget >= 24576) {
                                    fAGC *= 0.833f;
                                }

                                for(int i = 0; i<length; i++) {
                                    short tmp = s.get(i);
                                    s.put(i, (short) ((tmp + lastSample) * fAGC/2));
                                    lastSample = tmp;
                                }
                            }
                            mTrack.write(modifiedSamples, 0, available);
                        }

                        mCodec.releaseOutputBuffer(outputBufIndex, false);

                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            sawOutputEOS = true;
                        }
                    } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        outputBuffers = mCodec.getOutputBuffers();
                        Log.d("PCM", "Output buffers changed");
                    } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        mTrack.stop();
                        mLock.lock();
                        mTrack.release();
                        final MediaFormat oformat = mCodec.getOutputFormat();
                        Log.d("PCM", "Output format has changed to"
                                + oformat);
                        initDevice(
                                oformat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                oformat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
                        outputBuffers = mCodec.getOutputBuffers();
                        mTrack.play();
                        mLock.unlock();
                    }
                } while (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED
                        || res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED);
            }
            Log.d(TAG_TRACK, "Decoding loop exited. Stopping codec and track");
            Log.d(TAG_TRACK, "Duration: " + (int) (mDuration / 1000));
            Log.d(TAG_TRACK, "Current position: " + (int) (mExtractor.getSampleTime() / 1000));
            mCodec.stop();
            mTrack.stop();
            Log.d(TAG_TRACK, "Stopped codec and track");
            Log.d(TAG_TRACK, "Current position: " + (int) (mExtractor.getSampleTime() / 1000));
            mIsDecoding = false;
            if (mContinue && (sawInputEOS || sawOutputEOS)) {
                mCurrentState = STATE_PLAYBACK_COMPLETED;
                if(owningMediaPlayer.onCompletionListener != null) {
                    Thread t = new Thread(() -> {
                        owningMediaPlayer.onCompletionListener.onCompletion(owningMediaPlayer);
                    });
                    t.setDaemon(true);
                    t.start();
                }
            } else {
                Log.d(TAG_TRACK, "Loop ended before saw input eos or output eos");
                Log.d(TAG_TRACK, "sawInputEOS: " + sawInputEOS);
                Log.d(TAG_TRACK, "sawOutputEOS: " + sawOutputEOS);
            }
            synchronized (mDecoderLock) {
                mDecoderLock.notifyAll();
            }
        });
        mDecoderThread.setDaemon(true);
        mDecoderThread.start();
    }
}
