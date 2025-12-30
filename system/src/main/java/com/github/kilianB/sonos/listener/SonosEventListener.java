package com.github.kilianB.sonos.listener;

import java.util.ArrayList;
import java.util.List;

import com.github.kilianB.sonos.model.AVTransportEvent;
import com.github.kilianB.sonos.model.PlayMode;
import com.github.kilianB.sonos.model.PlayState;
import com.github.kilianB.sonos.model.QueueEvent;
import com.github.kilianB.sonos.model.TrackInfo;

/**
 * Event listener used to listen to specific UPnP Events emitted by sonos speakers
 * indicating state changes like volume or bass.
 * 
 * @author Kilian
 *
 */
public interface SonosEventListener {
	
	/**
	 * Fired once the volume of the speaker changes
	 * @param newVolume the new volume of the speaker
	 */
	public void volumeChanged(int newVolume);
	
	/**
	 * Fired once the play state of the speaker changes.
	 * Additionally a {@link avtTransportEvent} will be fired.
	 * @param playState the new play state of the speaker
	 */
	public void playStateChanged(PlayState playState);
	
	/**
	 * Fired once the play mode of the speaker changes.
	 * Additionally a {@link avtTransportEvent} will be fired.
	 * @param playMode the new play mode of the speaker
	 */
	public void playModeChanged(PlayMode playMode);
	
	/**
	 * Fired once a queue was manipulated. 
	 * @param queuesAffected The ID and value of the affected queues
	 */
	public void queueChanged(List<QueueEvent> queuesAffected);
	
	/**
	 * Fired once a new track starts to play or is skipped to
	 * @param currentTrack information about the new track
	 */
	public void trackChanged(TrackInfo currentTrack);
		
	/**
	 * Fired once the treble of the speaker changes
	 * @param treble the new treble of the speaker
	 */
	public void trebleChanged(int treble);

	/**
	 * Fired once the bass of the speaker changes
	 * @param bass the new volume of the speaker
	 */
	public void bassChanged(int bass);

	/**
	 * Fired once the loudness of the speaker changes
	 * @param loudness the new loudness of the speaker
	 */
	public void loudenessChanged(boolean loudness);

	/**
	 * AVT Transport events are fired when playmode or playstates change and carry more 
	 * information that the specific event handler. Using this method allows more insight 
	 * into what is currently happening at the speaker but does not clearly state which variable
	 * change emitted this event.
	 * @param avtTransportEvent UPnPEventInformation
	 */
	public void avtTransportEvent(AVTransportEvent avtTransportEvent);

	/**
	 * Event fired once a sonos device is newly reachable via the network,
	 * Be aware that this event gets emitted rather unreliably and potentially severely lags.
	 * It also should be noted that this method is not invoked if the speaker subscribed 
	 * to itself is the device connecting.
	 * @param deviceName The name of the newly connected sonos speaker.
	 */
	public void sonosDeviceConnected(String deviceName);
	
	/**
	 * Event fired once a sonos device is no longer reachable via the network,
	 * Be aware that this event gets emitted rather unreliably and potentially severely lags.
	 * It also should be noted that this method is not invoked if the speaker subscribed 
	 * to itself is the device disconnecting.
	 * @param deviceName The name of the sonos speaker disconnecting.
	 */
	public void sonosDeviceDisconnected(String deviceName);

	/**
	 * Event fired once the grouping status of the device changes.
	 * @param allDevicesInZone The zone name of the devices the current device
	 * is in a group with including itself
	 */
	public void groupChanged(ArrayList<String> allDevicesInZone);
}
