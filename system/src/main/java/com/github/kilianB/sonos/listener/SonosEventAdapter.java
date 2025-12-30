package com.github.kilianB.sonos.listener;

import java.util.ArrayList;
import java.util.List;

import com.github.kilianB.sonos.model.AVTransportEvent;
import com.github.kilianB.sonos.model.PlayMode;
import com.github.kilianB.sonos.model.PlayState;
import com.github.kilianB.sonos.model.QueueEvent;
import com.github.kilianB.sonos.model.TrackInfo;

/**
 * Adapter implementation of the {@link SonosEventListener}
 * @author Kilian
 *
 */
public class SonosEventAdapter implements SonosEventListener {

	@Override
	public void volumeChanged(int newVolume) {}

	@Override
	public void playStateChanged(PlayState newPlayState) {}

	@Override
	public void playModeChanged(PlayMode newPlayMode) {}

	@Override
	public void queueChanged(List<QueueEvent> queuesAffected) {}

	@Override
	public void trackChanged(TrackInfo currentTrack) {}

	@Override
	public void trebleChanged(int treble) {}

	@Override
	public void bassChanged(int bass) {}

	@Override
	public void loudenessChanged(boolean loudness) {}

	@Override
	public void avtTransportEvent(AVTransportEvent avtTransportEvent) {}

	@Override
	public void sonosDeviceConnected(String deviceName) {}
	
	@Override
	public void sonosDeviceDisconnected(String deviceName) {}

	@Override
	public void groupChanged(ArrayList<String> allDevicesInZone) {}

}
