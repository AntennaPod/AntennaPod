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

package com.aocate.presto.service;

import com.aocate.presto.service.IDeathCallback_0_8;
import com.aocate.presto.service.IOnBufferingUpdateListenerCallback_0_8;
import com.aocate.presto.service.IOnCompletionListenerCallback_0_8;
import com.aocate.presto.service.IOnErrorListenerCallback_0_8;
import com.aocate.presto.service.IOnPitchAdjustmentAvailableChangedListenerCallback_0_8;
import com.aocate.presto.service.IOnPreparedListenerCallback_0_8;
import com.aocate.presto.service.IOnSeekCompleteListenerCallback_0_8;
import com.aocate.presto.service.IOnSpeedAdjustmentAvailableChangedListenerCallback_0_8;
import com.aocate.presto.service.IOnInfoListenerCallback_0_8;

interface IPlayMedia_0_8 {
	boolean canSetPitch(long sessionId);
	boolean canSetSpeed(long sessionId);
	float getCurrentPitchStepsAdjustment(long sessionId);
	int getCurrentPosition(long sessionId);
	float getCurrentSpeedMultiplier(long sessionId);
	int getDuration(long sessionId);
	float getMaxSpeedMultiplier(long sessionId);
	float getMinSpeedMultiplier(long sessionId);
	int getVersionCode();
	String getVersionName();
	boolean isLooping(long sessionId);
	boolean isPlaying(long sessionId);
	void pause(long sessionId);
	void prepare(long sessionId);
	void prepareAsync(long sessionId);
	void registerOnBufferingUpdateCallback(long sessionId, IOnBufferingUpdateListenerCallback_0_8 cb);
	void registerOnCompletionCallback(long sessionId, IOnCompletionListenerCallback_0_8 cb);
	void registerOnErrorCallback(long sessionId, IOnErrorListenerCallback_0_8 cb);
	void registerOnInfoCallback(long sessionId, IOnInfoListenerCallback_0_8 cb);
	void registerOnPitchAdjustmentAvailableChangedCallback(long sessionId, IOnPitchAdjustmentAvailableChangedListenerCallback_0_8 cb);
	void registerOnPreparedCallback(long sessionId, IOnPreparedListenerCallback_0_8 cb);
	void registerOnSeekCompleteCallback(long sessionId, IOnSeekCompleteListenerCallback_0_8 cb);
	void registerOnSpeedAdjustmentAvailableChangedCallback(long sessionId, IOnSpeedAdjustmentAvailableChangedListenerCallback_0_8 cb);
	void release(long sessionId);
	void reset(long sessionId);
	void seekTo(long sessionId, int msec);
	void setAudioStreamType(long sessionId, int streamtype);
	void setDataSourceString(long sessionId, String path);
	void setDataSourceUri(long sessionId, in Uri uri);
	void setEnableSpeedAdjustment(long sessionId, boolean enableSpeedAdjustment);
	void setLooping(long sessionId, boolean looping);
	void setPitchStepsAdjustment(long sessionId, float pitchSteps);
	void setPlaybackPitch(long sessionId, float f);
	void setPlaybackSpeed(long sessionId, float f);
	void setSpeedAdjustmentAlgorithm(long sessionId, int algorithm);
	void setVolume(long sessionId, float left, float right);
	void start(long sessionId);
	long startSession(IDeathCallback_0_8 cb);
	void stop(long sessionId);
	void unregisterOnBufferingUpdateCallback(long sessionId, IOnBufferingUpdateListenerCallback_0_8 cb);
	void unregisterOnCompletionCallback(long sessionId, IOnCompletionListenerCallback_0_8 cb);
	void unregisterOnErrorCallback(long sessionId, IOnErrorListenerCallback_0_8 cb);
	void unregisterOnInfoCallback(long sessionId, IOnInfoListenerCallback_0_8 cb);
	void unregisterOnPitchAdjustmentAvailableChangedCallback(long sessionId, IOnPitchAdjustmentAvailableChangedListenerCallback_0_8 cb);
	void unregisterOnPreparedCallback(long sessionId, IOnPreparedListenerCallback_0_8 cb);
	void unregisterOnSeekCompleteCallback(long sessionId, IOnSeekCompleteListenerCallback_0_8 cb);
	void unregisterOnSpeedAdjustmentAvailableChangedCallback(long sessionId, IOnSpeedAdjustmentAvailableChangedListenerCallback_0_8 cb);
}