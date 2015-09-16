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
import android.support.annotation.Nullable;
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

    @SuppressWarnings("deprecation")
    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        // Pass call directly to AudioTrack if available.
        if (null != mTrack) {
            mTrack.setStereoVolume(leftVolume, rightVolume);
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

    @SuppressWarnings("deprecation")
    public void decode() {
        mDecoderThread = new Thread(() -> {
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
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
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

                            checkNormalizerSetting();
                            if(normalizer != null) {
                                normalizer.normalize(modifiedSamples);
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
                        Log.d("PCM", "Output format has changed to" + oformat);
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

    private Normalizer normalizer;

    @Nullable
    private void checkNormalizerSetting() {
        String setting = UserPreferences.getNormalizer();
        if(setting == null || setting.equals("none")) {
            normalizer = null;
        } else if(setting.equals("simple")) {
            if(!(normalizer instanceof Simple)) {
                normalizer = new Simple();
            }
        } else if(setting.equals("xmms2")) {
            if(!(normalizer instanceof Xmms2)) {
                normalizer = new Xmms2();
            }
        } else if(setting.equals("mplayer1")) {
            if(!(normalizer instanceof MPlayer1)) {
                normalizer = new MPlayer1();
            }
        } else if(setting.equals("mplayer2")) {
            if(!(normalizer instanceof MPlayer2)) {
                normalizer = new MPlayer2();
            }
        } else {
            Log.wtf(TAG, "Unknown normalizer: " + setting);
            UserPreferences.setNormalizer(null);
        }
    }

    interface Normalizer {
        void normalize(byte[] samples);
    }

    ////////////////
    // XMMS2 adapted from:
    // http://git.xmms2.org/xmms2/xmms2-devel/tree/src/plugins/normalize/compress.c

    /*  AudioCompress
     *  Copyright (C) 2002-2003 trikuare studios (http://trikuare.cx)
     *
     *  This library is free software; you can redistribute it and/or
     *  modify it under the terms of the GNU Lesser General Public
     *  License as published by the Free Software Foundation; either
     *  version 2.1 of the License, or (at your option) any later version.
     *
     *  This library is distributed in the hope that it will be useful,
     *  but WITHOUT ANY WARRANTY; without even the implied warranty of
     *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
     *  Lesser General Public License for more details.
     */

    private class Xmms2 implements Normalizer {

        private static final short GAIN_TARGET = 25000; // target level

        private static final short GAIN_MAX = 32; /* The maximum amount to amplify by */
        private static final short GAIN_SHIFT = 10;        /* How fine-grained the gain is */
        private static final short GAIN_SMOOTH = 8;        /* How much inertia ramping has */
        private static final short BUCKETS = 400;     /* How long of a history to store */

        private XmmsPeaks peaks = new XmmsPeaks(BUCKETS);
        int gainCurrent = 1 << GAIN_SHIFT;
        int gainTarget = 1 << GAIN_SHIFT;

        public void normalize(byte[] data) {
            ByteBuffer bf = ByteBuffer.wrap(data);
            bf.order(ByteOrder.LITTLE_ENDIAN);
            ShortBuffer s = bf.asShortBuffer();
            final int len = s.limit();

            short peak = 0;
            for (int i = 0; i < len; i++) {
                peak = (short) Math.max(peak, Math.abs(s.get(i)));
            }

            peaks.add(peak);

            if (peaks.max() > peak) {
                peak = peaks.max();
            }
            if(peak == 0) {
                return;
            }

            int gain = (1 << GAIN_SHIFT) * GAIN_TARGET / peak;

            if (gain < (1 << GAIN_SHIFT)) {
                gain = 1 << GAIN_SHIFT;
            }

            gainTarget = (gainTarget * ((1 << GAIN_SMOOTH) - 1) + gain) >> GAIN_SMOOTH;

            // Give it an extra insignifigant nudge to counteract possible rounding error

            if (gain < gainTarget) {
                gainTarget--;
            } else if (gain > gainTarget) {
                gainTarget++;
            }

            if (gainTarget > GAIN_MAX << GAIN_SHIFT) {
                gainTarget = GAIN_MAX << GAIN_SHIFT;
            }

            // See if a peak is going to clip
            gain = (1 << GAIN_SHIFT) * Short.MAX_VALUE / peak;

            int pos;
            if (gain < gainTarget) {
                gainTarget = gain;
                pos = 1;
            } else {
                // We're ramping up, so draw it out over the whole frame
                pos = len;
            }

            int gr = ((gainTarget - gainCurrent) << 16) / pos;

            /* Do the shiznit */
            int gf = gainCurrent << 16;

            for (int i = 0; i < len; i++) {

                /* Interpolate the gain */
                gainCurrent = gf >> 16;
                if (i < pos) {
                    gf += gr;
                } else if (i == pos) {
                    gf = gainTarget << 16;
                }

                /* Amplify */
                int sample = s.get(i) * gainCurrent >> GAIN_SHIFT;
                if (sample < Short.MIN_VALUE) {
                    sample = Short.MIN_VALUE;
                } else if (sample > Short.MAX_VALUE) {
                    sample = Short.MAX_VALUE;
                }
                s.put(i, (short) sample);
            }
        }
    }

    private class XmmsPeaks {

        private int index = 0;
        private short max = -1;
        private boolean full = false;

        private final short[] buffer;

        public XmmsPeaks(int capacity) {
            buffer = new short[capacity];
        }

        public void add(short value) {
            if(buffer[index] == max) {
                max = -1;
            }
            buffer[index] = value;
            if(value > max) {
                max = value;
            }
            index++;
            if(index == buffer.length) {
                full = true;
                index = 0;
            };
        }

        public short max() {
            if(max < 0) {
                for(int i=0; i < buffer.length; i++) {
                    short value = buffer[i];
                    if(value > max) {
                        max = value;
                    } else if(value == 0) {
                        break;
                    }
                }
            }
            return max;
        }

        public int getFillingLevel() {
            return full ? buffer.length : index;
        }

    }

    private class Simple implements Normalizer {

        private float gain = 1.0f;

        public void normalize(byte[] data) {
            ByteBuffer bf = ByteBuffer.wrap(data);
            bf.order(ByteOrder.LITTLE_ENDIAN);
            ShortBuffer s = bf.asShortBuffer();
            final int length = data.length/2;

            short peak = 0;
            for (int i = 0; i < length; i++) {
                peak = (short) Math.max(peak, Math.abs(s.get(i)));
            }

            int maxTarget = (int)(peak * gain);
            float oldGain = gain;
            if (maxTarget >= 25800) {
                gain = 1.0f * 25800 / peak;
            } else if (maxTarget < 12288) {
                gain *= 1.005;
            }

            float gainStep = (gain - oldGain)/length;
            for(int i = 0; i<length; i++) {
                short target = (short)(s.get(i) * (gain + i * gainStep));
                s.put(i, target);
            }
        }
    }

    // MPlayer algorithms adapted from:
    // svn://svn.mplayerhq.hu/mplayer/trunk
    // libaf/af_volnorm.c

    /*
     * Copyright (C) 2004 Alex Beregszaszi & Pierre Lombard
     *
     * This file is part of MPlayer.
     *
     * MPlayer is free software; you can redistribute it and/or modify
     * it under the terms of the GNU General Public License as published by
     * the Free Software Foundation; either version 2 of the License, or
     * (at your option) any later version.
     *
     * MPlayer is distributed in the hope that it will be useful,
     * but WITHOUT ANY WARRANTY; without even the implied warranty of
     * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     * GNU General Public License for more details.
     *
     * You should have received a copy of the GNU General Public License along
     * with MPlayer; if not, write to the Free Software Foundation, Inc.,
     * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
     */

    //////////////
    // MPlayer #1
    // uses a 1 value memory and coefficients new=a*old+b*cur (with a+b=1)
    private class MPlayer1 implements Normalizer {

        private final float DEFAULT_TARGET = 0.25f;
        private final float SIL_S16 = Short.MAX_VALUE * 0.01f; // silence level
        private final float MID_S16 = Short.MAX_VALUE * DEFAULT_TARGET; // ideal level

        private final float MUL_INIT = 1.0f;
        private final float MUL_MIN = 0.1f;
        private final float MUL_MAX = 5.0f;

        private final float SMOOTH_MUL = 0.06f;

        private float mul = MUL_INIT;

        @Override
        public void normalize(byte[] samples) {
            ByteBuffer bf = ByteBuffer.wrap(samples);
            bf.order(ByteOrder.LITTLE_ENDIAN);
            ShortBuffer data = bf.asShortBuffer();
            final int len = samples.length / 2;

            float curAvg = 0.0f;
            short tmp;

            for (int i = 0; i < len; i++) {
                tmp = data.get(i);
                curAvg += tmp * tmp;
            }
            curAvg = (float) Math.sqrt(curAvg / len);

            if (curAvg > SIL_S16) {
                float neededMul = MID_S16 / (curAvg * mul);
                mul = (1.0f - SMOOTH_MUL) * mul + SMOOTH_MUL * neededMul;

                // clamp the mul coefficient
                clip(mul, MUL_MIN, MUL_MAX);
            }

            // Scale & clamp the samples

            for (int i = 0; i < len; i++) {
                float prod = mul * data.get(i);
                short value = clip(prod, Short.MIN_VALUE, Short.MAX_VALUE);
                data.put(i, value);
            }
        }
    }

    //////////////
    // MPlayer #2
    // uses several samples to smooth the variations (standard weighted mean on past samples)
    private class MPlayer2 implements Normalizer {

        private final int NSAMPLES = 128;
        private final int MIN_SAMPLE_SIZE = 32000;

        private final float DEFAULT_TARGET = 0.25f;
        private final float SIL_S16 = Short.MAX_VALUE * 0.01f; // silence level
        private final float MID_S16 = Short.MAX_VALUE * DEFAULT_TARGET; // ideal level

        private final float MUL_INIT = 1.0f;
        private final float MUL_MIN = 0.1f;
        private final float MUL_MAX = 5.0f;

        private float mul = MUL_INIT;
        private CircularMemoryBuffer mem = new CircularMemoryBuffer(NSAMPLES);

        @Override
        public void normalize(byte[] data) {
            ByteBuffer bf = ByteBuffer.wrap(data);
            bf.order(ByteOrder.LITTLE_ENDIAN);
            ShortBuffer samples = bf.asShortBuffer();
            final int len = samples.limit();

            float curAvg = 0.0f;
            for (int i = 0; i < len; i++) {
                short tmp = samples.get(i);
                curAvg += tmp * tmp;
            }
            curAvg = (float) Math.sqrt(curAvg/len);

            // Evaluate an adequate 'mul' coefficient based on previous state, current
            // samples level, etc
            float avg = 0.0f;
            int totalSamples = 0;
            for (int i = 0; i < mem.getFillingLevel(); i++) {
                avg += mem.getAvg(i) * mem.getSamples(i);
                totalSamples += mem.getSamples(i);
            }

            if (totalSamples > MIN_SAMPLE_SIZE) {
                avg /= totalSamples;
                if (avg >= SIL_S16) {
                    mul = clip(MID_S16 / avg, MUL_MIN, MUL_MAX);
                }
            }

            // Scale & clamp the samples
            for (int i = 0; i < len; i++) {
                samples.put(i, clip(mul * samples.get(i), Short.MIN_VALUE, Short.MAX_VALUE));
            }

            // Stores average for future smoothing
            float newAvg = mul * curAvg;
            mem.add(len, newAvg);
        }
    }

    private class CircularMemoryBuffer {

        private int index;
        private boolean full = false;

        final int[] samples;
        final float[] avg;

        public CircularMemoryBuffer(int capacity) {
            this.samples = new int[capacity];
            this.avg = new float[capacity];
        }

        public void add(int samples, float avg) {
            this.samples[index] = samples;
            this.avg[index] = avg;
            index++;
            if(index == this.samples.length) {
                full = true;
                index = 0;
            }
        }

        public int getSamples(int index) {
            return this.samples[index];
        }

        public float getAvg(int index) {
            return this.avg[index];
        }

        public int getFillingLevel() {
            return full ? this.samples.length : index;
        }

    }

    private static float clip(float value, float min, float max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        } else {
            return value;
        }
    }

    private static short clip(float value, short min, short max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        } else {
            return (short) value;
        }
    }

}
